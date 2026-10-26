# Performance

Este documento reúne cómo se midió y optimizó el rendimiento de la app, con el formato **hallazgo → causa → solución → verificación**. La regla fue *primero medir, después optimizar*: cada cambio va en su propio commit, con su medición antes/después.

## Metodología

Una medición solo es evidencia si se puede reproducir, así que el entorno se fijó y se documentó.

| Tema | Decisión |
|---|---|
| Dispositivo | realme 7 5G (físico), Android 12, 1080×2400 a 480 dpi. **Todos** los números salen de aquí; el emulador solo se usa para tests funcionales |
| Build medido | `benchmark`: hereda de `release` (R8 + shrinkResources), `isDebuggable = false`, `profileable`. El build `debug` no se mide: lleva instrumentación JaCoCo y es `debuggable` |
| Pantalla | 60 Hz fijo (presupuesto de 16.6 ms por frame), animaciones a 1x |
| Condiciones de arranque | Batería > 50 %, temperatura de batería ≤ 35 °C, `Thermal Status: 0`, ahorro de energía apagado, misma red WiFi |
| Estado inicial | Datos y caché borrados, app cerrada, backend calentado (el proxy GraphQL en Render gratuito tarda ~22 s en despertar) |
| Caché de shaders | Arranca vacío en cada corrida (se asume que borrar datos y caché lo elimina). Los valores absolutos incluyen la compilación de shaders; el antes/después es comparable porque ambos lados usan el mismo protocolo. T4 mide el caso con el caché lleno (ver "Compilación de shaders") |
| Commits medidos | Baseline: `0d649b5` (layout original, variante `benchmark`). H0: `a31493c`. Traces T3, T4 y T5 (H0 + H7, solo para entender, fuera de las medianas): `3d41372`; T4 y T5 sin borrar datos, tras un recorrido de calentamiento (T5 abre el detalle dos veces seguidas) |
| Escenario A (Popular) | Cold start → lista → 3 ciclos de scroll → abrir detalle → volver |
| Herramientas | `adb` (`dumpsys meminfo`, `gfxinfo`, `am start -W`) para los números; Perfetto / System Trace para entender *por qué*. Lo que sale del Profiler **no** entra en las tablas de medianas: añade overhead |
| Estadística | 3 corridas de baseline y 5 tras el cambio; se reporta la **mediana con su rango**. Una corrida atípica solo se descarta si se documenta una causa; si no, se conserva |
| Fase 3 (memoria y leaks) | Se hace en el build `debug` (hace falta `debuggable` para los heap dumps y LeakCanary) con un solo dispositivo conectado; **no** se usa para tiempos. Antes de cada heap dump se fuerza el GC y se comprueba en `logcat`. Los conteos salen de un script propio que cuenta las instancias alcanzables por referencias fuertes desde los GC roots (el Profiler cuenta también la basura aún sin recolectar). Cada cifra es de una captura (n = 1) |

## Resumen

| ID | Hallazgo | Estado |
|---|---|---|
| H0 | El `RecyclerView` de Popular no reciclaba (20 `ViewHolder` de golpe) | ✅ Corregido. Demostrado en memoria, `Views` y `ViewHolder` creados; indicio (no prueba) de menos jank |
| — | Scroll: `shader_compile` de 37–65 ms en la `RenderThread`, ~22 s después de empezar el recorrido (T1 y T3) | ✅ Artefacto del protocolo (caché de shaders vacío): con caché lleno (T4) baja a 2.1 ms. n = 1 |
| — | Apertura del detalle: frames de ~60 ms esperando a la `RenderThread` (compilación de shaders) | ✅ Artefacto del protocolo (T4, n = 1). Con caché lleno queda un `doFrame` de 34.4 ms que espera a la `RenderThread` (capas, texturas y `VectorDrawable`; qué dibuja cada una sin verificar) |
| — | Volver a la lista: `RV OnLayout` con 5 `onCreateViewHolder` (~44 ms de un `doFrame` de 61.5 ms en T4) y una capa de pantalla completa (T2) | 🔎 Coste real del hilo principal, medido en T3, T4 y T5 (`doFrame` de 57–62 ms en los cuatro `BACK`); qué lo reduciría no se ha probado |
| H7 | `getPopularMovies()` se llamaba 2 veces al volver del detalle (guarda de `savedInstanceState` + `TextWatcher`) | ✅ Corregido. Demostrado con Logcat, tests de regresión y los traces T3 y T4 (ninguna consulta de populares tras el `BACK`). **Sin mejora de tiempos medida** |
| — | ~80 % de los frames con Buffer Stuffing, en antes y después | 🔎 Sin diagnosticar |
| — | Glide: caché de imágenes (lista y detalle) | ✅ Comportamiento esperado: 1 descarga por póster, memoria en el scroll, disco al reiniciar y en modo avión |
| — | Glide: `placeholder` vectorial + `withCrossFade()` en las imágenes del detalle | 🔎 Apoyada por T5 (n = 1): en la segunda apertura (`MEMORY_CACHE`) desaparecen la rasterización del placeholder y el cross-fade; no se pueden separar sin cambiar código |
| — | El póster del detalle no reutiliza ninguna descarga de la lista | 🔎 Causa sin investigar |
| H9 | La vista destruida de Popular seguía retenida mientras se estaba en el detalle (1 `RecyclerView`, 5 `MovieViewHolder` y 6 `Bitmap` de la lista) | ✅ Corregido (`661af70`). Demostrado con un test rojo/verde, LeakCanary y heap dumps (una captura de cada). Es un arreglo de memoria: **sin mejora de tiempos medida** |
| — | `FavoritesViewModel.observeIsFavorite` dejaba vivo un collector por llamada | ✅ Corregido (`c96331e`). Demostrado con un test rojo/verde (`expected 1 / but was 2`); efecto en la app sin medir |
| — | Crecimiento de ~12 MB del `Java Heap` durante el escenario A | ✅ No es retención de objetos Java (`Heap Alloc` +1.1 MB, n = 5; sin acumulación en los heap dumps). 🔎 Interpretación como *churn*: sin medir asignaciones; ~4.6 MB sin atribuir |
| — | LeakCanary en `debug` | ✅ Sin efecto apreciable en `PopularMoviesFragmentTest` (n = 2 por condición, dentro de la variación entre corridas) |
| B | Escenario B (Genres, Paging 3) | ⏳ Pendiente |

---

## H0 — El `RecyclerView` de Popular no reciclaba

### Hallazgo

Al cargar Popular se inflaban y enlazaban los 20 items a la vez, y otra vez los 20 al volver del detalle. En el trace (T1), los 20 `onCreateViewHolder` sumaban 92.1 ms y caían todos en el mismo frame: el `Choreographer#doFrame` más largo del recorrido duró **181.7 ms**.

### Causa

`fragment_popular_movies.xml` tenía un `RecyclerView` con `layout_height="wrap_content"` y `nestedScrollingEnabled="false"` dentro de un `NestedScrollView`. Con altura sin límite, el `RecyclerView` se mide al tamaño de todo su contenido, el `LayoutManager` crea todos los items y el scroll lo hace el `NestedScrollView`. Se pierde la virtualización, que es la razón de ser de `RecyclerView`.

### Solución

Commit `a31493c`. Se eliminó el `NestedScrollView`. El `FrameLayout` con los estados (carga, error, vacío) recibe `app:layout_behavior="@string/appbar_scrolling_view_behavior"` y el `RecyclerView` pasa a `match_parent` con el nested scrolling por defecto. Es el mismo patrón que ya usa `fragment_genres.xml`.

### Verificación

Medianas con rango (baseline n=3, tras el cambio n=5; mismo dispositivo, build y escenario):

| Métrica | Baseline | H0 | Lectura |
|---|---|---|---|
| `Views`, inicio | 278 | **112** | Separación total |
| Native Heap, inicio | 56.9 MB (55.6–58.3) | **32.6 MB** (32.2–32.9) | Separación total |
| Native Heap, final | 79.3 MB (78.8–79.8) | **58.6 MB** (41.8–61.0) | Separación total |
| Graphics, inicio | 87.8 MB (87.6–98.3) | **28.5 MB** (27.9–28.5) | Separación total |
| Graphics, final | 87.3 MB (77.6–88.1) | **64.6 MB** (51.9–68.2) | Separación total, valor ruidoso |
| Janky frames | 6.00 % (5.36–6.87) | **3.40 %** (3.07–7.08) | Indicio: 4 de 5 corridas por debajo del baseline |
| p99 de frame | 32 ms | **29 ms** (27–32) | Indicio |
| Java Heap, final | 18.5 MB | 18.5 MB | Sin cambio |
| Janky frames (criterio legacy) | 18.56 % | 18.57 % | Sin cambio |
| `TotalTime` (cold start) | 366 ms | 393 ms | Sin mejora (esperable: el primer frame sale antes de que cargue la lista) |

**Cómo leer la fuerza de la evidencia.** Con 3 contra 5 corridas, el mejor p-valor posible en un Mann-Whitney exacto unilateral es 1/56 ≈ 0.018, y solo lo alcanzan las métricas de memoria y `Views`. El jank (p ≈ 0.13) y el p99 son un **indicio**, no una prueba. Con este cambio, el delta de memoria (final − inicio) y el `TOTAL PSS` no son métricas válidas: H0 cambia el estado inicial por diseño, así que se comparan valores absolutos.

**Verificación mecánica con trace** (misma recorrida, antes y después):

| Dato | T1 (antes) | T2 (H0) |
|---|---|---|
| `onCreateViewHolder` | **20** en 92.1 ms, todos de golpe al volver del detalle | **9**: 4 al inicio del scroll (22.2 ms) y 5 al volver (29.3 ms) |
| `Choreographer#doFrame` más largo (hilo principal) | **181.7 ms** | **62.7 ms** |
| Frames de hilo principal > 33 ms | 2 (181.7 y 65.2 ms) | 4 (62.7, 62.4, 60.3 y 33.9 ms) |
| Frames > 33 ms (frame timeline) | 80 | 51 |
| `Self Jank` (incluye incompletos) | 13 (máx. 227.8 ms) | 8 (máx. 87.9 ms) |
| `Other Jank` | 76 | 82 |
| Buffer Stuffing | 1 100 de 1 415 (77.7 %) | 1 117 de 1 398 (79.9 %) |
| `No Jank` | 226 | 191 |

Lo que **sí** muestra el trace: desaparece el pico de ~180 ms del `doFrame` al volver del detalle y el máximo de `Self Jank` baja de 227.8 a 87.9 ms. Lo que **no** muestra: una reducción del número de frames pesados del hilo principal (hay 4 por encima de 33 ms frente a 2) ni más frames `No Jank`. Los frames de ~60 ms de T2 son de otro tipo (ver "Hallazgos abiertos"). Son un solo trace por versión y con overhead del Profiler, así que se presentan como apoyo del mecanismo, no como prueba.

<details>
<summary>Las 8 corridas completas</summary>

Memoria en MB (inicio → final).

| Corrida | `TotalTime` | Native | Graphics | `Views` | Janky | p99 |
|---|---|---|---|---|---|---|
| A1 (baseline) | 397 ms | 55.6 → 78.8 | 87.8 → 77.6 | 278 → 323 | 6.00 % | 32 ms |
| A2 (baseline) | 366 ms | 58.3 → 79.8 | 98.3 → 88.1 | 278 → 323 | 5.36 % | 32 ms |
| A3 (baseline) | 355 ms | 56.9 → 79.3 | 87.6 → 87.3 | 278 → 323 | 6.87 % | 34 ms |
| H0-A1 | 393 ms | 32.6 → 58.6 | 28.5 → 64.6 | 112 → 278 | 4.11 % | 32 ms |
| H0-A2 | 374 ms | 32.9 → 58.7 | 28.5 → 65.6 | 112 → 278 | 3.40 % | 27 ms |
| H0-A3 (atípica) | 440 ms | 32.6 → 41.8 | 28.5 → 51.9 | 112 → 275 | 7.08 % | 31 ms |
| H0-A4 | 359 ms | 32.6 → 58.2 | 27.9 → 56.0 | 112 → 278 | 3.07 % | 29 ms |
| H0-A5 | 433 ms | 32.2 → 61.0 | 27.9 → 68.2 | 112 → 278 | 3.08 % | 27 ms |

</details>

### Limitaciones y corridas atípicas

- **H0-A3** tuvo `TotalTime` 440 ms, jank 7.08 % (legacy 27.46 %) y Native final 41.8 MB. No se identificó una causa, así que **se conserva** en las medianas. El `Dexopt state` de ART se comprobó y no cambió.
- **H0-A5** se repitió porque en el primer intento la app salió de primer plano durante el recorrido; ese intento se descartó. En la repetición, la señal del router era distinta (misma red WiFi) y la batería estaba al 55 %.
- A2 se midió con el backend dormido (22.6 s de calentamiento) y sus métricas quedan dentro del rango de A1 y A3.
- Los frames incompletos de los traces (`dur = -1`: 6 en T1, 6 en T2, 12 en T3 y 3 en T4) se incluyen en los recuentos. Son frames reales con la duración perdida (Perfetto descartó 5–6 eventos de fin con timestamp negativo en T1 y T2, y 11 en T3), no frames descartados. En T3, 10 de los 15 `Self Jank` son incompletos, así que sus peores frames podrían estar entre ellos: el máximo de 88.4 ms sale solo de los completos.
- **Tests de regresión.** `PopularMoviesFragmentTest` (11 tests, en emulador; el 11.º es el de H7) pasa completo con el layout actual. Al añadir los 2 tests de H0 (la clase tenía entonces 10), con el layout anterior a H0 (`0d649b5`) fallaron exactamente esos 2: `whenListIsLong_recyclerViewOnlyInflatesVisibleItems` (esperaba `childCount` menor que 50, pero fue 50: se inflaban todos los items) y `recyclerView_keepsNestedScrollingEnabled_soAppBarCanReact` (esperaba `true`). Así se comprueba que protegen contra reintroducir el problema.

---

## H7 — `getPopularMovies()` se llamaba dos veces al volver del detalle

### Hallazgo

Las consultas `SELECT * FROM movies WHERE is_popular = 1` y `SELECT MAX(cached_at)` aparecían al volver del detalle, en T1 y en T2. Un log temporal con la pila de llamadas (emulador, build con R8) mostró que al volver del back stack `getPopularMovies()` se ejecutaba **dos veces**.

### Causa

1. Desde `onViewCreated`: `savedInstanceState` llega `null` al volver del back stack, así que la guarda `if (savedInstanceState == null)` no la evitaba.
2. Desde `afterTextChanged`: al restaurar el estado de la vista, el `EditText` (dentro de un `TextInputLayout`) llama a `setText` y el `TextWatcher`, ya registrado, pedía las populares con la consulta vacía.

Además, cada llamada lanzaba un colector nuevo en `viewModelScope` (nada cancelaba el anterior) y emitía `Loading` antes de leer Room, de modo que la lista pasaba a `GONE` y volvía a `VISIBLE`.

### Solución

Commits separados por motivo, cada uno con sus tests:

| Commit | Cambio |
|---|---|
| `ee48074` | Fragment: la carga inicial depende del estado del ViewModel (`if (viewModel.uiState.value is MainUiState.Idle)`) y `setupRealtimeSearch()` pasa a `onViewStateRestored`, de modo que el `TextWatcher` se registra después de la restauración del `EditText` |
| `f858cb3` | `MovieDao.replacePopularMovies()` con `@Transaction` (borrar + insertar de forma atómica); el repositorio la usa |
| `aa70687` | `MainViewModel` guarda un `loadJob` y cancela el anterior en `searchMovies` y `getPopularMovies` (elimina colectores duplicados y la carrera entre una búsqueda lenta y las populares) |
| `3d41372` | Test de regresión de H7 |

### Verificación

**Logcat (emulador).** Llamadas a `getPopularMovies()` tras el cambio del Fragment:

| Caso | Llamadas |
|---|---|
| Arranque en frío | 1 (desde `onViewCreated`) |
| Abrir el detalle y volver | 0 |
| Rotar | 0 |
| Escribir "ab", abrir el detalle y volver | 0; el texto y los resultados quedan intactos |
| Borrar el texto | 1 (desde `afterTextChanged`) |
| Muerte del proceso (`am kill`) y reabrir desde Recientes | 1 (desde `onViewCreated`); la lista sobrevive |

`am kill` solo mata procesos cacheados: se comprobó con `dumpsys activity lru` (`cch`) y con `pidof` vacío.

**Tests.**

- `MovieDaoTest`: 3 tests nuevos de `replacePopularMovies` (19 en total).
- `MainViewModelTest`: 2 tests de cancelación (búsqueda lenta frente a populares y frente a otra búsqueda). Sin el `Job` único fallan los 2 (11 ejecutados, 2 fallidos); con él pasan.
- `PopularMoviesFragmentTest.whenFragmentViewIsRecreated_popularMoviesAreNotReloaded`: hace `detach`/`attach` del Fragment y comprueba `verify(exactly = 1)` sobre `getPopularMovies()` y que la lista sigue visible. Con el Fragment anterior a `ee48074` falla con **3 llamadas**; con el actual pasa. Para poder escribirlo, `launchFragmentInHiltContainer` ahora devuelve el `ActivityScenario` y mantiene el observer del `NavController`. La clase completa pasa (11 de 11) en el emulador.

**Trace T3** (`3d41372`, mismo recorrido y mismo protocolo que T2, con datos y caché borrados antes de capturar):

| Dato | T2 (H0) | T3 (H0 + H7) |
|---|---|---|
| Consultas de populares al volver | `is_popular` y `MAX(cached_at)` | **Ninguna.** En todo el trace no hay consulta a la tabla de populares. Control: el mismo trace sí registra el SQL de Room del detalle (`movie_details`, `favorites`) |
| `onCreateViewHolder` | 9: 4 al inicio (22.2 ms) y 5 al volver (29.3 ms) | 9: 4 (27.4 ms) y 5 (28.1 ms) |
| `Choreographer#doFrame` más largo | 62.7 ms | 71.9 ms (en el scroll, ver "Hallazgos abiertos") |
| `doFrame` > 33 ms (hilo principal) | 4 | 5 (71.9, 62.5, 50.0, 49.6 y 39.0 ms) |
| `Self Jank` (incluye incompletos) | 8 (5 incompletos) | 15 (10 incompletos) |
| `Other Jank` | 82 | 70 |
| `No Jank` | 191 | 210 |
| Buffer Stuffing | 1 117 de 1 398 (79.9 %) | 1 173 de 1 468 (79.9 %) |
| Frames incompletos (`dur = -1`) | 6 | 12 |

**Lectura.** Lo que sí muestra T3: tras el `BACK` no se lanza ninguna consulta de populares, algo que sí ocurría en T1 y T2. Lo que no muestra: una mejora de tiempos. Los `onCreateViewHolder` de la vuelta siguen siendo 5 (el coste de crear un `RecyclerView` nuevo al recrearse la vista, que no depende de H7) y el resto de métricas se parece a T2 dentro de lo que permite un solo trace por versión con overhead del Profiler. Los `Self Jank` completos pasan de 3 a 5 y los incompletos de 5 a 10, diferencias que con n = 1 no se interpretan. El efecto esperado de H7 era trabajo redundante y parpadeo, no milisegundos; con `DiffUtil` síncrono en `MovieAdapter`, repetir la carga con listas iguales no rebindea ítems. El parpadeo no se midió. T4 (otro trace, mismo commit, sin borrar datos) repite el resultado: ninguna consulta de populares en todo el trace, con `SELECT * FROM movie_details WHERE id = ?` como control de que el SQL de Room se registra.

---

## Compilación de shaders: un artefacto del protocolo

### Hallazgo

Dos momentos fijos del recorrido (~10 s después del primer frame, en el scroll, y ~32 s, al abrir el detalle) dejan al hilo principal esperando a la `RenderThread`, que compila shaders: hasta 64.6 ms de `shader_compile` en una sola vez (T3).

### Evidencia previa (T1, T2 y T3)

**Scroll: `shader_compile` en la `RenderThread`.** En T3, a los 10.3 s del primer frame (unos 22 s antes de abrir el detalle), el `doFrame` 2630287 (71.9 ms) apenas trabaja en el hilo principal (~0.5 ms de registro de HWUI; el resto de `draw` no tiene hijos) y espera a la `RenderThread`. Esta, en `DrawFrames 2630285` (el frame anterior: 88.4 ms, `Self Jank` con `App Deadline Missed`), está en `flush commands` con un `shader_compile` de 64.6 ms (`cache_miss` de 63.1 ms, 2 × `driver_compile_shader` y `driver_link_program` de 49.7 ms). El frame 2630287 dura 83.9 ms y está etiquetado Buffer Stuffing: paga la espera del anterior. En el `DrawFrames 2630287` siguiente hay un `drawLayer [RecyclerView]`. En T1 hay un grupo equivalente de 37.4 / 35.6 ms unos 22 s antes del detalle; en T2 no se revisó. Que aparezca a la misma distancia del detalle en dos traces apunta a un punto fijo del recorrido y no a algo aleatorio, pero no se identificó qué se dibuja ahí. Ese 2630285 es también el máximo de `Self Jank` completo de T3 (comprobado por token), es decir, el peor frame completo de T3 es de scroll y no del detalle ni del `BACK`. La causa probable se comprobó con T4 (más abajo).

**Apertura del detalle.** En T2, el frame 37552850 (87.9 ms, `Self Jank`) tiene un `doFrame` de ~63 ms que casi no trabaja: ~58 ms son espera de la `RenderThread`, que está en `flush commands` con `shader_compile` y `cache_miss`. En T1 el frame equivalente (27980665, 65.2 ms) contiene en la `RenderThread` un `shader_compile` de 53.7 ms con su `cache_miss` (50.9 ms): empieza ~5 ms después del inicio del `doFrame` y termina dentro de él. Ese frame cae unos 2.3 s antes del `doFrame` de la vuelta (27980771), lo que encaja con el recorrido (`tap`, 2 s, `BACK`). Es decir, el coste de abrir el detalle ya aparece antes de H0 y no es un efecto del cambio. En T3 se ve en los slices: el `doFrame` 2632276 (49.6 ms) apenas trabaja en el hilo principal (~1 ms de registro de HWUI) y la `RenderThread` (`DrawFrames 2632275`) está en `flush commands` con un `shader_compile` de 52.5 ms (con `cache_miss`, 2 × `driver_compile_shader` y `driver_link_program`). En los ~0.16 s siguientes hay otros tres `shader_compile` de 13.4, 9.9 y 12.4 ms, todos con `cache_miss` (≈ 88 ms sumando los cuatro). Los frames de 39 y 50 ms que siguen (`doFrame` 2632315 y 2632319) tampoco los causa el hilo principal: la `RenderThread` dibuja un `drawLayer [ShapeableImageView]` y en `DrawFrames 2632315` aparece esperando en `dequeueBuffer`. El frame 2632315 termina `Late Present` con `App Deadline Missed`, y su display frame en SurfaceFlinger (2632324, 18.5 ms) también falla (`SurfaceFlinger CPU Deadline Missed`). Justo antes del 2632315, el hilo principal está dentro de slices `Lcom/google/android/material/chip/Chip;` (anidados con `Ll/r;` y `LQ/k;`, nombres ofuscados por R8) cuya duración no se midió. La causa probable (caché de shaders vacío por borrar los datos en cada corrida) se comprobó con T4, más abajo. Qué ocupaba los ~50 ms de la `RenderThread` en 2632315 y 2632319 no se identificó; en T4 esos frames ya no figuran entre los `doFrame` de más de 33 ms.

### Causa

Hipótesis: el protocolo borra datos y caché antes de cada corrida, así que el caché de shaders arranca vacío y cada programa se compila (con `cache_miss`) la primera vez que se dibuja.

### Verificación (T4)

T4 repite el recorrido de T3 sobre el mismo commit (`3d41372`), pero **sin borrar datos** y tras un recorrido de calentamiento completo (scroll, detalle y `BACK`) hecho justo antes, dentro de los 30 min del TTL de la caché de Room.

| Dato | T3 (datos borrados) | T4 (sin borrar datos) |
|---|---|---|
| `shader_compile` en la `RenderThread` | 5 slices, **152.8 ms** en total, máx. 64.6 ms | 5 slices, **10.9 ms** en total, máx. 3.9 ms |
| `cache_miss` y `driver_link_program` | presentes (63.1 y 49.7 ms en el mayor) | ninguno |
| Momento de los `shader_compile` | 10.3 s (64.6 ms) y 32.3–32.5 s (52.5, 13.4, 9.9 y 12.4 ms) | 9.9 s (2.1 ms) y 32.4–32.6 s (3.9, 2.5, 1.5 y 0.8 ms) |
| `doFrame` > 33 ms (hilo principal) | 5: 71.9 (scroll), 62.5 (`BACK`), 50.0, 49.6 y 39.0 (detalle) | 2: 61.5 (`BACK`) y 34.4 (detalle) |
| `onCreateViewHolder` | 4 (27.4 ms) al inicio y 5 (28.1 ms) al volver | 4 (22.8 ms) y 5 (29.4 ms) |
| `Self Jank` (incompletos) | 15 (10); máx. completo 88.4 ms (scroll, frame 2630285) | 6 (3); máx. completo 100.8 ms (`BACK`, frame 3601367) |
| Buffer Stuffing | 1 173 de 1 468 (79.9 %) | 1 075 de 1 352 (79.5 %) |
| `Other Jank` / `No Jank` | 70 / 210 | 121 / 150 |
| Frames incompletos (`dur = -1`) | 12 | 3 |
| Consultas de populares tras el `BACK` | ninguna | ninguna |

**Lectura.** Con el caché de shaders lleno desaparecen los stalls: los 5 `shader_compile` siguen ocurriendo en los mismos momentos del recorrido, pero suman 10.9 ms en lugar de 152.8 ms y no hay `cache_miss`; en el frame de 34.4 ms del detalle el `shader_compile` (1.5 ms) contiene un `cache_hit` de 1.3 ms. Los `doFrame` de 71.9 ms (scroll) y de 49.6, 50.0 y 39.0 ms (detalle) de T3 no reaparecen entre los de más de 33 ms; en el detalle queda uno de 34.4 ms (ver "Hallazgos abiertos"). Es **un solo trace** con el caché lleno (n = 1), y el protocolo cambia dos cosas a la vez: el caché de shaders y las cachés de datos (en T4 hay un `SELECT` de `movie_details` y ningún `INSERT`, así que el detalle sale de Room). Lo segundo no explica que desaparezcan los `cache_miss`, que es una señal específica de shaders. Lo que no mejora: Buffer Stuffing sigue en ~80 % y las etiquetas de jank no mejoran (`Other Jank` 70 → 121, `No Jank` 210 → 150); las capturas tienen duraciones distintas (1 468 frente a 1 352 frames), así que no se interpretan los conteos. T5 (otra captura con el caché lleno) repite el patrón en las filas visibles de su consulta: `shader_compile` de 3.8, 4.1, 2.4, 0.9 y 0.8 ms (12.0 ms en total) y ningún `cache_miss`.

### Implicaciones

- Las medianas de H0 y del baseline se midieron con el caché de shaders vacío. La comparación antes/después es válida (ambos lados usan el mismo protocolo), pero los valores absolutos de jank, p99 y `TotalTime` incluyen este coste y no representan un uso normal.
- Con el caché lleno, lo que domina el recorrido es el `BACK` (`doFrame` de 61.5 ms; ver "Volver a la lista").
- No se han repetido las corridas de medición con caché lleno. Hacerlo sería otro protocolo (otra tabla), no una corrección de la actual.

---

## Caché de imágenes (Glide)

### Qué se midió

Con un `RequestListener` temporal en `loadUrl` (variante `debug`, sin commitear y revertido al terminar) se registró el `DataSource` de cada carga y se contó con `adb logcat`. No se toman tiempos: el build `debug` no sirve para eso. La lista de Popular muestra posters `w500` en una vista de 80×120 dp (240×360 px); el detalle muestra un backdrop `w780` y un póster `w500`.

| Situación | `DataSource` observado | Requests de red | OK |
|---|---|---|---|
| Primera carga (5 posters visibles) | `REMOTE` ×5 | 5 | ✅ |
| Primer scroll hasta ver los 20 (sin calentamiento previo) | `REMOTE` ×15, `MEMORY_CACHE` ×12 | 15 | ✅ (5 + 15 = 20; el resto son recargas desde memoria) |
| Scroll arriba/abajo con todo ya cargado | `MEMORY_CACHE` ×24 | 0 | ✅ |
| Tras reiniciar la app | `DATA_DISK_CACHE` ×5 | 0 | ✅ |
| Modo avión (tras reiniciar; la lista salió de Room) | `DATA_DISK_CACHE` ×5, sin `FAILED` | 0 | ✅ |
| Detalle, primera apertura | backdrop `w780` `REMOTE`; póster `w500` `REMOTE` | 2 | ✅ |
| Detalle, segunda apertura (mismo proceso) | backdrop y póster `MEMORY_CACHE` | 0 | ✅ |

Las imágenes se vieron correctamente en todas las situaciones. Las 20 URLs descargadas en las dos primeras filas son distintas: ninguna se descargó dos veces.

### Lectura

- La caché se comporta como se espera: una descarga por póster, memoria en el scroll, disco tras reiniciar y en modo avión. No hay hallazgo de red en la lista.
- Que tras reiniciar aparezca `DATA_DISK_CACHE` y no `RESOURCE_DISK_CACHE` corresponde a la estrategia por defecto (`AUTOMATIC`), que guarda en disco los datos originales de las imágenes remotas: cada póster se decodifica de nuevo tras un reinicio. Es el comportamiento documentado de Glide; su coste no se midió.
- El póster que muestra el detalle (`…/w500/fBFjaD…`) no coincide con ninguna de las URLs registradas en la lista, así que se descargó aparte. No se investigó por qué. Hipótesis: el `poster_path` difiere entre la fuente de la lista (GraphQL) y la del detalle (por ejemplo, por el parámetro `language`). *Comprobación:* revisar ese parámetro en ambas llamadas o comparar el `posterPath` de los dos modelos para la misma película.

---

## H9 — La vista destruida de Popular seguía retenida en el detalle

### Hallazgo

Al ir de la lista al detalle, `PopularMoviesFragment` queda en el back stack con su vista destruida. Aun así, un heap dump tomado dentro del detalle mostraba 1 `RecyclerView`, 5 `MovieViewHolder`, 5 `ItemMovieBinding` y 6 `Bitmap` de la lista (5 pósters de 240×360 y 1 de 1008×432). LeakCanary lo reportó como 1 *application leak*: el `MovieAdapter` retenía 2.5 MB en 6 849 objetos (cifra de LeakCanary; los 6 `Bitmap` salen del script propio sobre el `.hprof`).

### Causa

El Fragment guarda el adapter como propiedad y lo asigna al `RecyclerView` en cada `onViewCreated`. `RecyclerView.setAdapter` registra al `RecyclerView` como observer del adapter, y `onDestroyView()` no lo desregistraba. La cadena, idéntica en el heap dump y en LeakCanary:

```
PopularMoviesFragment.adapter → MovieAdapter → mObservable → mObservers[0]
  → RecyclerViewDataObserver → RecyclerView → FrameLayout → CoordinatorLayout (la vista destruida)
```

Como el Fragment sigue vivo en el back stack, todo lo que cuelga de él también.

### Solución

Commit `661af70`. En `onDestroyView()`, antes de `_binding = null`:

```kotlin
binding.recyclerView.adapter = null
```

Con `setAdapter(null)` el `RecyclerView` se desregistra del adapter y la jerarquía de vistas deja de estar alcanzable.

### Verificación

| Captura (`debug`, realme 7 5G) | `RecyclerView` | `MovieViewHolder` | `Bitmap` del heap de la app |
|---|---|---|---|
| (a) Lista cargada, sin arreglo | 1 | 5 | 5 (pósters de la lista) |
| (b) Dentro del detalle, sin arreglo | **1 (retenido)** | **5** | 9 |
| (c2) Tras lista→detalle→volver ×3, GC confirmado | 1 | 5 | 6 (5 pósters + 1 en el `LruBitmapPool` de Glide) |
| (b') Dentro del detalle, con el arreglo | **0** | **0** | 3 (ninguno de la lista) |

- **Test de regresión.** `PopularMoviesFragmentTest.whenFragmentViewIsDestroyed_adapterIsNoLongerObservedByOldRecyclerView` comprueba que, tras destruir la vista, el adapter ya no tiene observers (`hasObservers()`). **Falla sin el cambio** (`hasObservers=true`) y pasa con él; la clase completa pasa 12 de 12 en el emulador.
- **LeakCanary.** Sin el arreglo avisaba de un objeto retenido a los ~5 s de entrar al detalle. Con el arreglo, en 3 min 16 s no apareció ningún aviso de retención ni análisis (mismo recorrido, mismo dispositivo).
- **Sin acumulación.** Tras tres idas y vueltas (c2) queda 1 `RecyclerView` vivo (el de la lista actual) y 0 `MovieDetailsFragment`: la retención era transitoria, mientras se estaba en el detalle.
- **Scroll.** Tras bajar 4 swipes, abrir una película y volver, la lista muestra los mismos 4 títulos visibles (`uiautomator`).
- El `Bitmap` de 1599×900 que queda tras el GC lo retiene el `LruBitmapPool` de Glide (ruta observada en el heap dump): es un bitmap reciclable del pool, no un leak.

### Limitaciones

- Una captura por columna (n = 1). Los conteos son de un script propio sobre los `.hprof`, no de la interfaz del Profiler.
- El análisis de LeakCanary se bloqueó en segundo plano en este dispositivo (casi 10 min parado, 34 s en primer plano); la causa interna es una hipótesis.
- No se midió el efecto sobre la memoria o los tiempos en `benchmark`: es una corrección de retención, y se documenta como tal.
- Un `Bitmap` de 1008×432 retenido por la lista en (b) no se identificó.

---

## `FavoritesViewModel`: un collector vivo por llamada

### Hallazgo

Leyendo el código del detalle: `MovieDetailsFragment.showMovieDetails()` llama a `favoritesViewModel.observeIsFavorite(...)` en cada emisión de `Success`, y `observeUiState` usa `repeatOnLifecycle(STARTED)`, así que se vuelve a llamar cada vez que la pantalla regresa a `STARTED`.

### Causa

`observeIsFavorite` hacía `isFavoriteJob?.cancel()`, pero `isFavoriteJob` **nunca se asignaba**: el `cancel()` no cancelaba nada y cada llamada dejaba vivo su collector del `Flow` de Room hasta que el ViewModel se limpiaba. Es el mismo patrón que ya se había corregido en `MainViewModel` (`aa70687`).

### Solución

Commit `c96331e`: `isFavoriteJob = viewModelScope.launch { ... }` (una línea).

### Verificación

El test `whenObserveIsFavoriteCalledTwice_onlyTheLastCollectorStaysActive` usa un `Flow` que no termina (como el de Room) y cuenta los collectors activos tras dos llamadas. **Falla sin el cambio** (`expected: 1 / but was: 2`) y pasa con él; `FavoritesViewModelTest` pasa 17 de 17. El test anterior sobre dos llamadas seguidas no lo detectaba porque usaba `flowOf()`, que termina solo.

### Limitaciones

No se midió el efecto en la app. No es un leak permanente: los collectors mueren con el ViewModel del detalle. No se revisó el DAO de favoritos, así que no se sabe cuánta consulta repetida implicaba.

---

## Memoria: el crecimiento de ~12 MB del Java Heap

### Hallazgo

En `benchmark`, entre el inicio y el final del escenario A, el `Java Heap:` de `dumpsys meminfo` crece unos 12 MB (medianas de 5 corridas tras H0: 6 340 → 18 508 KB).

### Qué se comprobó

| Fila de `dumpsys meminfo` (medianas, n = 5, KB) | Inicio | Final | Δ |
|---|---|---|---|
| `Java Heap:` (PSS, resumen) | 6 340 | 18 508 | **+12 168** |
| `Dalvik Heap`, PSS | 4 638 | 12 241 | +7 603 |
| `Dalvik Heap`, Heap Size | 12 229 | 14 500 | +2 271 |
| `Dalvik Heap`, Heap Alloc (objetos) | 6 085 | 7 250 | **+1 165** |

En `debug`, con GC forzado, `Heap Alloc` pasa de 7 202 a 8 651 KB al final del recorrido y a 8 117 tras el GC, y las `Views` vuelven de 157 a 112. Los heap dumps (a), (b) y (c2) no muestran acumulación.

### Lectura

- **No es retención de objetos Java.** Los objetos crecen ~1.1 MB como máximo (sin GC en ninguna de las dos lecturas), y tras el GC el uso vuelve a niveles cercanos al inicial.
- 🔎 **Interpretación (sin medir asignaciones):** el heap reserva ~12 MB pero al inicio solo 4.6 MB están residentes; al asignar objetos, aunque luego sean basura, esas páginas se tocan y el PSS sube hacia la capacidad del heap. En `debug` el arranque ya empieza en 22.7 MB de PSS, casi en el techo, y por eso allí solo crece 2.2 MB.
- **Sin atribuir:** ~4.6 MB del +12 MB no están en la fila `Dalvik Heap`; no se sabe en qué otra categoría caen.

---

## Hallazgos abiertos (hipótesis)

Ninguno se da por confirmado. Cada uno indica la evidencia actual y cómo se comprobaría.

**Detalle con el caché de shaders lleno.** En T4 queda un `doFrame` de 34.4 ms al abrir el detalle (token 3601313, a los 32.5 s), junto a dos `Self Jank` cortos (3601300, incompleto, y 3601305, 14.2 ms). El hilo principal no es la causa: su `draw` dura 34.0 ms pero solo ~1.5 ms son trabajo de HWUI (`Record View#draw()`); el resto es espera a la `RenderThread`, igual que en T3. En la `RenderThread`, en los ~54 ms desde el inicio del frame, hay `flush layers` (15.3 y 2.8 ms), dos `drawLayer [CoordinatorLayout] 1080.0 x 2158.0` de pantalla completa (10.4 y 6.2 ms; en T2 también había una capa de este tamaño al volver), `VectorDrawable repaint` de 1080x1080 (4.9 y 1.6 ms) y de 495x495 (1.2 ms), `Bitmap#prepareToDraw 1080x900` (4.3 ms), `Texture upload` de 1080x1080 (2.4 ms) y un `drawLayer [ShapeableImageView] 330.0 x 495.0` (2.1 ms). Limitaciones: la consulta no asigna cada slice a su `DrawFrames` (aparecen el 3601315, de 7.4 ms, y el 3601319, de 13.5 ms) y deja fuera trabajo de un `DrawFrames` anterior que empezó antes del frame; qué dibuja cada capa y cada `VectorDrawable` no se ha verificado. Los tamaños coinciden con las vistas del layout: el backdrop es un `ImageView` de 300 dp (1080×900 px) y el póster mide 110×165 dp (330×495 px); un placeholder vectorial cuadrado con `centerCrop` daría 1080×1080 y 495×495. `loadUrl` aplica `placeholder` y `withCrossFade()` (ver "Caché de imágenes (Glide)"). Qué origina la capa de pantalla completa de 1080×2158 (¿animación de navegación?) no se ha revisado. En T5 (réplica con caché lleno) la primera apertura vuelve a dar un `doFrame` de 40 ms, con `shader_compile` de 0.8–4.1 ms, y la segunda no produce ningún `doFrame` de más de 25 ms (ver "Glide: transición y placeholder").

**Glide: transición y placeholder.** `loadUrl` aplica `.placeholder(...)`, `.error(...)` y `.transition(withCrossFade())` con el mismo placeholder vectorial (`ic_movie_placeholder`) para todas las vistas. Según el comportamiento documentado de Glide, la transición se omite cuando la imagen sale de `MEMORY_CACHE`; en la primera apertura del detalle (`REMOTE`) y tras reiniciar el proceso (`DATA_DISK_CACHE`) no ocurre, y la segunda apertura sí sale de memoria (tabla de arriba). En T3 las imágenes del detalle venían de la red y en T4 (proceso reiniciado antes de capturar) se infiere que de `DATA_DISK_CACHE`; el `DataSource` no se registró en esos traces. Hipótesis: durante el cross-fade se dibujan placeholder e imagen a la vez y el vector se repinta frame a frame, lo que encajaría con los `VectorDrawable repaint` del detalle. *Comprobación (T5, n = 1):* un trace con dos aperturas seguidas del detalle en el mismo proceso y con el caché de shaders lleno. **Primera apertura** (31.6–32.1 s): `VectorDrawable repaint` de 1080x1080 (5.1 y 1.7 ms) y de 495x495 (1.2 y 0.3 ms) con sus `Texture upload` (3.4 + 0.9 y 0.7 + 0.5 ms), `Bitmap#prepareToDraw` de 1080x900 (3.5 ms) y de 330x495 (1.0 ms), dos `drawLayer [CoordinatorLayout]` de 11.6 y 11.8 ms y unas 20 veces `drawLayer [ShapeableImageView] 330.0 x 495.0` de 0.1–0.3 ms durante ~0.3 s (de 31.81 a 32.11 s, lo que coincide con los 300 ms por defecto del cross-fade de Glide); `doFrame` de 40 ms. **Segunda apertura** (36.1–36.3 s): ninguno de los slices de 1080x1080, 495x495 ni de bitmaps, un solo `drawLayer [ShapeableImageView]` de 0.1 ms, `drawLayer [CoordinatorLayout]` de 1.8 ms como máximo y ningún `doFrame` de más de 25 ms. **Lectura:** apoya que parte del trabajo de la primera apertura viene del placeholder vectorial rasterizado al tamaño de la vista (~14 ms de `RenderThread` entre `repaint` y `upload`) y del cross-fade, pero corrige la hipótesis: el vector **no** se repinta frame a frame (hay dos `repaint` de 1080x1080, no ~20). El coste es puntual, y durante el cross-fade solo hay capas baratas. Con la imagen en `MEMORY_CACHE` se espera (comportamiento de Glide, no verificado) que esté lista en cuanto se conoce el tamaño de la vista, de modo que el placeholder no llegue a dibujarse y no haya transición: placeholder y cross-fade desaparecen juntos y este experimento no los separa. Que desaparezcan también `Bitmap#prepareToDraw` y `Texture upload` de las imágenes es esperable por reutilizar bitmaps ya subidos y no dice nada del cross-fade. Separarlos exigiría un cambio de código medido por separado.

**Tamaños pedidos a TMDB.** La lista pide `w500` (500 px de ancho) para una vista de 240 px de ancho; el detalle pide `w780` para un backdrop de 1080 px de ancho, lo que implica ampliarlo (el slice `Bitmap#prepareToDraw 1080x900` del detalle sugiere un bitmap final del tamaño de la vista, unos 3.9 MB en ARGB_8888). No se midieron los bytes descargados ni el efecto en memoria. *Comprobación:* comparar con `curl` los bytes de una misma imagen en distintos tamaños de TMDB (`w185`, `w342`, `w500`) antes de decidir nada.

**`Glide.with(context)`.** `loadUrl` usa el contexto de la vista (hipótesis G6). La cadena que LeakCanary reportó para H9 no pasa por Glide, y los `Bitmap` de la lista que seguían vivos en el detalle desaparecieron al corregir H9. Su efecto sobre la memoria no se midió por separado.

**Volver a la lista.** El frame previo a 37552949 (T2) contiene `RV OnLayout` con 5 `onCreateViewHolder` e `inflate`, y la `RenderThread` dibuja una capa de 1080×2158 (`drawLayer [CoordinatorLayout]`), probablemente por la transición entre fragments. Los 5 `onCreateViewHolder` son el coste esperable de crear un `RecyclerView` nuevo al recrearse la vista del Fragment, y no dependen de H7. En T3 el `doFrame` 2632420 (62.5 ms) tiene el mismo patrón y aquí sí es el hilo principal quien trabaja: la mayor parte del frame es `layout` → `RV OnLayout` con los 5 `onCreateViewHolder` (10.5, 5.5, 4.3, 3.9 y 3.9 ms = 28.1 ms, cada uno con su `inflate`) y sus `onBindViewHolder`, seguido de `draw`; la `RenderThread` empieza su `DrawFrames 2632420` al final. Es coherente con crear la vista nueva y no con recargar datos (H7 ya no lanza consultas). En T4, con el caché de shaders lleno, el mismo frame (`doFrame` 3601365, 61.5 ms) se mide exacto: `layout` 46.7 ms → `RV OnLayout` 44.3 ms (con los 5 `onCreateViewHolder`: 11, 5.5, 4.6, 4.6 y 3.7 ms = 29.4 ms; el primero incluye un `inflate` de 10.6 ms) y `draw` 9.7 ms. Es decir, el coste de la vuelta no depende de los shaders ni de la caché de datos. En la frame timeline ese tramo aparece como dos `Self Jank`: el 3601365 (incompleto) y el 3601367 (100.8 ms; `Prediction Error, App Deadline Missed, Buffer Stuffing`). En T5, con dos `BACK` seguidos, el `doFrame` mide 59.2 y 57.3 ms (5 `onCreateViewHolder`: 27.4 y 26.4 ms) y los `Self Jank` asociados son de 128.7 y 87 ms; que el segundo no sea apreciablemente más barato indica que el coste no depende de cachés. Con los de T3 (62.5 ms) y T4 (61.5 ms), los cuatro `BACK` quedan entre 57 y 62 ms. Hipótesis añadida, sin comprobar: la capa de 1080×2158 viene de las animaciones por defecto del nav graph (`nav_default_*_anim`), aplicadas sobre la raíz del Fragment.

**Buffer Stuffing.** ~78–80 % de los frames, en antes y después (T3: 1 173 de 1 468, 79.9 %; T4, con el caché de shaders lleno: 1 075 de 1 352, 79.5 %), con `on_time_finish = 1`: no lo causa el trabajo de la app. Sin diagnosticar; puede influir el overhead del Profiler. Dos matices de T3: una sola parada de la `RenderThread` se reparte en varias etiquetas, así que Buffer Stuffing no significa "frame barato" (el frame 2630287 dura 83.9 ms y está etiquetado Buffer Stuffing); y en `DrawFrames 2632315` la `RenderThread` aparece esperando en `dequeueBuffer`, lo que encaja con Buffer Stuffing pero no se diagnosticó. Que se mantenga con el caché de shaders lleno descarta a los shaders como causa.

**Frames largos sin `doFrame` largo.** En la frame timeline de T3 hay 23 frames de más de 33 ms (sin contar los incompletos; por segmentos de 2 s desde el primer frame: 5 en 6–8 s, 6 en 10–12 s, 6 en 24–26 s, 4 en 32–34 s y 2 en 34–36 s). Los únicos `doFrame` del hilo principal de más de 33 ms (71.9, 62.5, 50.0, 49.6 y 39.0 ms) caen en 10, 32 y 34 s. En 6–8 s y 24–26 s no hay ninguno, así que esa duración no viene del hilo principal. Sin diagnosticar.

**Crecimiento del Java Heap.** La pregunta de la retención está respondida (ver arriba). Queda abierto de dónde salen ~4.6 MB que no están en la fila `Dalvik Heap`, y *comprobar* la interpretación como *churn* con un rastreo de asignaciones.

---

## Escenario B — Genres con Paging 3

⏳ Pendiente. Mismo método (baseline con el protocolo de la metodología, adaptado a Genres, que usa Retrofit contra TMDB y no necesita calentar el backend de Render) más la Fase 3 de leaks. Hipótesis a medir: G1 (`initialLoadSize`), G2 (`maxSize`), G5 (chips), G6 (`Glide.with`) y, para leaks, si el adapter como propiedad del Fragment reproduce el patrón de H9 a través del `ConcatAdapter` del footer.

---

## Cómo reproducir

Guías completas (en esta misma carpeta): [`guia-ejecucion-profiling.md`](guia-ejecucion-profiling.md) (protocolo de una corrida con `adb`), [`guia-system-trace.md`](guia-system-trace.md) (captura con el Profiler), [`03-guia-leakcanary.md`](03-guia-leakcanary.md) (Fase 3) y [`GLOSARIO-profiling.md`](GLOSARIO-profiling.md) (términos, consultas de Perfetto y registro de capturas).

1. Instalar el build `benchmark` con *Run → Profile 'app' as profileable (low overhead)*.
2. Dejar el teléfono en las condiciones de la tabla de metodología y borrar datos y caché.
3. Calentar el backend, abrir con `adb shell am start -W -n com.alexvicente.moviedb/.MainActivity` y ejecutar el recorrido fijo.
4. Leer `dumpsys meminfo` y `dumpsys gfxinfo` (tras `gfxinfo reset`) al terminar.

**Fase 3 (memoria y leaks).** Se hace en `debug`, no en `benchmark`:

1. Instalar con `.\gradlew :app:installDebug` (LeakCanary se instala solo; solo está en `debugImplementation`).
2. Forzar el GC antes de cada heap dump: `adb shell run-as com.alexvicente.moviedb kill -10 <PID>` y comprobar en `adb logcat -d` la línea `Explicit concurrent copying GC freed`.
3. Para LeakCanary: ir al detalle, pulsar Home *dentro* del detalle y volver a abrir la app para que el análisis termine (ver la guía).

**Datos que no se versionan.** Los `.hprof` (~55 MB cada uno) y las trazas de Perfetto (70–80 MB) están en `.gitignore`. Además, en los 5 heap dumps analizados aparecen cadenas con forma de JWT (probablemente el `TMDB_TOKEN` de `BuildConfig`), así que no deben subirse a un repositorio público.
