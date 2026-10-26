# Glosario y manual de referencia — Profiling con Android Studio Profiler, Perfetto y adb

Proyecto: MovieDB (`com.alexvicente.moviedb`) · Rama de trabajo: `perf/android-profiling-optimization`
Escrito el 2026-10-20, tras completar el análisis del escenario A (Popular → detalle → volver).

---

## 0. Para qué sirve este documento

Es una referencia para **analizar otras pantallas** (Genres, Favoritos, Videos…) con el mismo método, y para retomar el trabajo sin releer toda la conversación. Reúne:

1. Las **pruebas realizadas** y cómo se hizo cada una (§3).
2. Un **glosario** de términos de adb, Profiler, Perfetto, Glide y de los nombres de slices que aparecen en los traces (§4).
3. **Consultas SQL de Perfetto** y **comandos adb/PowerShell** reutilizables (§5 y §6).
4. Una **guía para leer un frame pesado** (§7), los **patrones y artefactos** encontrados (§8) y las **trampas** en las que ya caímos (§9).
5. Una **receta** para analizar otra vista (§10) y el **registro de capturas** (§11).

Convenciones:

- **n** es el número de corridas o de traces. Casi todo lo que sale de un trace es **n = 1** y con overhead del Profiler: se usa para entender el porqué, nunca para las tablas de medianas.
- ✅ = demostrado con la evidencia indicada · 🔎 = hipótesis o interpretación sin verificar. Una afirmación sin marca es una definición general de Android, Perfetto o Glide.
- Los resultados completos están en el README (sección Performance) y los datos crudos en `mediciones\` (no versionada). Aquí solo hay lo necesario para reutilizar el método.

---

## 1. Entorno de referencia

| Tema | Valor |
|---|---|
| Dispositivo de métricas | realme 7 5G (RMX2111), Android 12 (API 31), 1080×2400 a 480 dpi (3.0×), ~7.3 GiB de RAM. **Todos los números salen de aquí** |
| Pantalla | **60 Hz fijados** (el teléfono ofrece 60/90/120) → presupuesto de **16.6 ms** por frame. Animaciones a 1x |
| Build medido | **`benchmark`**: hereda de `release` (R8 + shrinkResources), no debuggable, `profileable`. Se comprueba que `flags=` no incluya `DEBUGGABLE` |
| Build de observación | `debug`, solo para código temporal de observación (logs de Glide) y heap dumps. **No se toman tiempos** con él |
| Emulador | Medium Phone API 36, solo para tests funcionales. Nunca se mezclan números de emulador y de dispositivo |
| Versiones | AGP 8.6.0 · Kotlin 2.0.21 · Glide 4.16.0 · RecyclerView 1.4.0 · Paging 3.5.0 |
| Popular | GraphQL/Apollo contra un proxy en Render (plan gratuito; ~22 s en despertar) + caché Room con TTL de 30 min |
| Genres | Retrofit/Gson contra TMDB (la implementación con Paging 3 está **por confirmar**) |
| Condiciones de sesión | Batería > 50 %, temperatura de batería ≤ 35 °C (`350` en `dumpsys battery`), `Thermal Status: 0`, ahorro de energía apagado, misma red WiFi |
| Recorrido del escenario A | Cold start → lista → scroll (3 ciclos de 4 swipes abajo y 4 arriba) → abrir detalle → volver |
| Coordenadas del recorrido | `swipe 540 1800 → 540 600` (350 ms) hacia abajo, el inverso hacia arriba, pausa de 800 ms; `tap 540 700` para abrir el detalle. **No se cambian**: si se cambian, las corridas anteriores dejan de ser comparables |
| Carpetas | `mediciones\` (corridas adb, **sin versionar**), `mediciones\glide\`, `mediciones\descartadas\`, `trazas\` (en `.gitignore`) |

---

## 2. Principios de método (no negociables)

1. **Primero medir, luego optimizar.** Un cambio de rendimiento = un commit, con su medición antes/después.
2. **`adb` para los números; Profiler/Perfetto para entender el porqué.** El Profiler añade overhead: sus tiempos no entran en las tablas de medianas. No se captura un trace durante una corrida de medición.
3. **Estadística:** baseline n = 3, tras el cambio n = 5; se reporta la **mediana con su rango**. Con 3 contra 5, el mejor p-valor posible en un Mann-Whitney exacto unilateral es 1/56 ≈ 0.018. Una corrida atípica solo se descarta si se documenta una causa; si no, se conserva.
4. **Estado inicial idéntico en cada corrida:** datos y caché borrados a mano, `force-stop` + `pidof` vacío, backend caliente (< 1 s), app abierta **solo** con `adb shell am start -W`.
5. **Un solo dispositivo conectado** al ejecutar Gradle (o fijar `$env:ANDROID_SERIAL`): `connectedDebugAndroidTest` instalaría `debug` encima de `benchmark`.
6. **Todo test de regresión debe fallar sin el arreglo** (ver §3.5).
7. **Git:** commits separados por motivo, `git add` siempre por nombre de archivo (nunca `git add .`), sin reescribir historia ya subida.
8. **Honestidad de la evidencia:** separar lo confirmado de las hipótesis, indicar siempre el n y no prometer mejoras de tiempo sin medirlas. Si algo dicho antes resulta impreciso, se corrige de forma explícita.

---

## 3. Catálogo de pruebas realizadas

### 3.0 Resumen

| ID | Tipo | Herramienta | Build / commit | Estado inicial | Para qué sirvió |
|---|---|---|---|---|---|
| A1–A3 | Corrida de medición (baseline) | `adb` | `benchmark`, `0d649b5` | Datos y caché borrados | Baseline de memoria, jank y arranque |
| H0-A1–A5 | Corrida de medición | `adb` | `benchmark`, `a31493c` | Datos y caché borrados | Medir H0 (layout del `RecyclerView`) |
| T1 | System Trace | Profiler → Perfetto | `benchmark`, `0d649b5` | Datos y caché borrados | Entender el jank **antes** de H0 |
| T2 | System Trace | Profiler → Perfetto | `benchmark`, `a31493c` | Datos y caché borrados | Entender el jank **después** de H0 |
| T3 | System Trace | Profiler → Perfetto | `benchmark`, `3d41372` | Datos y caché borrados | Cerrar H7 (consultas al volver) |
| T4 | System Trace | Profiler → Perfetto | `benchmark`, `3d41372` | **Sin borrar datos**, tras calentamiento | Probar la hipótesis del caché de shaders |
| T5 | System Trace | Profiler → Perfetto | `benchmark`, `3d41372` | Sin borrar datos, tras calentamiento | Dos aperturas del detalle: placeholder y cross-fade |
| H7-Logcat | Verificación | Logcat (`Log.d` temporal) | emulador | — | 6 casos de llamadas a `getPopularMovies()` |
| Glide 1–5b | Observación de `DataSource` | Logcat (parche temporal) | `debug` | Varios | Caché de imágenes de lista y detalle |
| Tests | Regresión | Gradle (JVM y emulador) | `debug` | — | Proteger H0 y H7 |
| exit-info | Diagnóstico de cierre | `adb` | — | — | Descartar un fallo tras una captura interrumpida |

### 3.1 Corridas de medición con `adb` (A1–A3, H0-A1–A5)

**Objetivo:** números reproducibles para las tablas del README (memoria, jank, arranque). **No se usa el Profiler.**

**Protocolo (11 pasos, uno por corrida):**

1. Estado limpio: cerrar la app y **borrar datos y caché a mano** (*Ajustes → Apps → MovieDB → Info de la app*); luego `force-stop` y comprobar que `pidof` no devuelve nada. `adb shell pm clear` falla en este realme (`SecurityException: CLEAR_APP_USER_DATA`).
2. Despertar el backend con `curl` hasta obtener `HTTP 200` en < 1 s.
3. Foto del estado del teléfono (batería, `Thermal Status`, `MemAvailable`, refresco) y comprobar los criterios de §1.
4. Cold start con `adb shell am start -W` (anotar `TotalTime`, comprobar `LaunchState: COLD`).
5. Comprobar **a ojo** que la lista cargó con posters (si sale "Sin conexión", la corrida es inválida).
6. `dumpsys meminfo` inicial (guardar la salida completa y la filtrada).
7. `dumpsys gfxinfo <pkg> reset` justo antes del scroll.
8. Recorrido automatizado (idéntico en todas las corridas).
9. `dumpsys meminfo` final y `dumpsys gfxinfo` (los dos bloques son obligatorios).
10. Verificar que existen los 7 archivos de la corrida.
11. Anotar, dejar enfriar el teléfono 2–3 min y pasar a la siguiente.

Los comandos exactos están en §6.

**Archivos por corrida:** `A#-0-estado.txt`, `A#-1-arranque.txt`, `A#-2-mem-inicio.txt` (+ `-completo`), `A#-3-mem-final.txt` (+ `-completo`), `A#-4-frames.txt`.

**Corrida inválida:** lista sin cargar, archivo ausente, recorrido interrumpido, temperatura o refresco fuera de criterio. Se mueven sus archivos a `mediciones\descartadas\<run>-intento1\` y se repite **con el mismo identificador**.

**Resultado de referencia (H0, ver README):** `Views` 278 → 112; Native Heap inicial 56.9 → 32.6 MB; Graphics inicial 87.8 → 28.5 MB (todo ✅ con separación total entre corridas); Janky frames 6.00 % → 3.40 % y p99 32 → 29 ms (🔎 solo indicio, p ≈ 0.13). `TotalTime`, jank *legacy* y Java Heap no cambiaron.

### 3.2 System Trace con el Profiler y análisis en Perfetto (T1–T5)

**Objetivo:** entender **qué hilo y qué código** gastan el tiempo en los frames pesados. No produce números para las tablas.

**Cómo se captura:**

1. Dejar el teléfono en el mismo estado que antes de una corrida (T1–T3) o con un recorrido de calentamiento previo (T4, T5).
2. Abrir la app **solo** con `adb shell am start -W` (no con Run ni Profile del IDE) y esperar a ver la lista con posters. Anotar el PID (`adb shell pidof <pkg>`).
3. En el Profiler, elegir el proceso (debe decir **Profileable**) → *Capture System Activities (System Trace)* → *Start profiler task from* la opción sobre el **proceso ya abierto** (*Now*) → *Start*.
4. Ejecutar **de inmediato** el recorrido.
5. *Stop*, esperar el procesado y **exportar** a `trazas\T#-nombre.trace` (conservar la extensión que genera el IDE).
6. Abrir el archivo en `ui.perfetto.dev` (*Open trace file*; el archivo se carga en el equipo). `W`/`S` hacen zoom, `A`/`D` desplazan, y la caja de búsqueda acepta un token (por ejemplo `2630287`).
7. Obtener el `upid` con la consulta Q0 (§5).

**Capturas hechas:**

| Traza | Qué cambia respecto al resto | Resultado principal |
|---|---|---|
| T1 (antes de H0) | `RecyclerView` sin reciclar | 20 `onCreateViewHolder` en 92.1 ms, todos en un frame; `doFrame` máximo 181.7 ms |
| T2 (H0) | Layout corregido | 9 `onCreateViewHolder` (4 + 5); `doFrame` máximo 62.7 ms |
| T3 (H0 + H7) | Cierra H7 | Ninguna consulta de populares tras el `BACK`; `shader_compile` de hasta 64.6 ms |
| T4 | Sin borrar datos, caché de shaders lleno | `shader_compile` total 152.8 → 10.9 ms; sin `cache_miss` |
| T5 | Dos aperturas seguidas del detalle | La 2.ª apertura no produce ningún `doFrame` > 25 ms |

### 3.3 Verificación con Logcat (H7)

**Para qué:** comprobar cuántas veces se llama a `getPopularMovies()` en cada situación, algo que un trace no da con claridad.

**Cómo:** un `Log.d("H7")` temporal (con la pila de llamadas) en el punto de entrada, en el emulador. **Se retira antes de commitear** y se comprueba con `git grep -n "H7" -- app/src` (los aciertos dentro de `pathData` de los vectores son falsos positivos).

**Casos verificados:** arranque en frío (1 llamada); abrir el detalle y volver (0); rotar (0); escribir "ab", abrir el detalle y volver (0, con texto y resultados intactos); borrar el texto (1); muerte del proceso con `am kill` y reabrir desde Recientes (1, desde `onViewCreated`).

`am kill` solo mata procesos **cacheados**: comprobar con `adb shell dumpsys activity lru | Select-String moviedb` (debe aparecer `cch`) y con `pidof` vacío; reabrir desde Recientes.

### 3.4 Observación de Glide (`DataSource`)

**Para qué:** saber de dónde sale cada imagen (red, disco o memoria) y cuántas peticiones de red hay.

**Cómo:** un `RequestListener` **temporal** (sin commitear) en `loadUrl`, en la variante `debug`, que registra el `DataSource` con la etiqueta `GlideSrc`. Después se cuenta con `adb logcat` (función `Glide-Resumen` en §6). Al terminar se revierte con `git checkout HEAD -- <ruta de Extensions.kt>`, se vuelve a `benchmark` y se comprueba `flags=`.

```kotlin
.listener(object : RequestListener<Drawable> {
    override fun onLoadFailed(
        e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
    ): Boolean { Log.d("GlideSrc", "FAILED $model"); return false }

    override fun onResourceReady(
        resource: Drawable, model: Any, target: Target<Drawable>?,
        dataSource: DataSource, isFirstResource: Boolean
    ): Boolean { Log.d("GlideSrc", "$dataSource $model"); return false }
})
```

En Glide 4.16, `resource`, `model` y `dataSource` de `onResourceReady` deben declararse **no nulos**; con tipos nullable da "overrides nothing".

**Situaciones y resultado (✅, n = 1 cada una):**

| Situación | `DataSource` | Red |
|---|---|---|
| 1. Primera carga (5 posters visibles) | `REMOTE` ×5 | 5 |
| 2. Primer scroll sin calentamiento | `REMOTE` ×15, `MEMORY_CACHE` ×12 | 15 |
| 2b. Scroll con todo ya cargado | `MEMORY_CACHE` ×24 | 0 |
| 3. Tras reiniciar | `DATA_DISK_CACHE` ×5 | 0 |
| 4. Modo avión | `DATA_DISK_CACHE` ×5, sin `FAILED` | 0 |
| 5a. Detalle, 1.ª apertura | backdrop `w780` y póster `w500`: `REMOTE` | 2 |
| 5b. Detalle, 2.ª apertura (mismo proceso) | ambos `MEMORY_CACHE` | 0 |

**Lecciones:** (a) para medir "scroll con todo cargado" hay que hacer antes un scroll completo **sin capturar**; (b) las 20 URLs descargadas fueron distintas (comprobado con `Group-Object`); (c) el póster del detalle no coincide con ninguna URL de la lista (🔎 causa sin investigar).

### 3.5 Tests de regresión

- Unitarios (JVM/Robolectric, incluye `MovieDaoTest`): `.\gradlew :app:testDebugUnitTest --tests "*Clase*"`.
- Instrumentados (emulador): `.\gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=<paquete>.PopularMoviesFragmentTest"`.
- Conteos vigentes: `MovieDaoTest` 19 · `PopularMoviesRepositoryImplTest` 16 · `MainViewModelTest` 11 · `PopularMoviesFragmentTest` 12 · `FavoritesViewModelTest` 17.

**Cómo se demuestra que un test protege contra la regresión:**

1. Traer la versión anterior del archivo arreglado: `git checkout <hash>~1 -- <archivo>`.
2. Ejecutar el test: **debe fallar**.
3. Restaurar con `git checkout HEAD -- <archivo>` y comprobar `git status`.

Ejemplos: los 2 tests de H0 fallan con el layout de `0d649b5` (`childCount` 50 de 50; `isNestedScrollingEnabled` false); el de H7 falla con **3 llamadas** si se usa el Fragment anterior a `ee48074`; los 2 de `MainViewModelTest` fallan sin el `Job` único (11 ejecutados, 2 fallidos).

Fase 3: el de H9 (`whenFragmentViewIsDestroyed_adapterIsNoLongerObservedByOldRecyclerView`) falla sin la línea `binding.recyclerView.adapter = null` (`hasObservers=true`) y el de `FavoritesViewModel` (`whenObserveIsFavoriteCalledTwice_onlyTheLastCollectorStaysActive`) falla con `expected: 1 / but was: 2` sin asignar `isFavoriteJob`. Para ver el rojo sin tocar el historial se puede apartar solo el archivo del arreglo: `git stash push -m "<nombre>" -- <archivo>`, correr el test y `git stash pop`. **Antes** comprueba con `git diff --name-only` y `git stash list` que el stash va a guardar algo.

### 3.6 Diagnóstico de una captura interrumpida

Si la app "se cierra" durante una grabación:

```powershell
adb shell dumpsys activity exit-info $pkg
adb logcat -b crash -d
```

En cada entrada de `exit-info` mira `reason` y la marca de tiempo. `USER REQUESTED` con `stop … due to from pid` es un `force-stop` (el de tu propio protocolo); `OTHER KILLS BY SYSTEM` en procesos cacheados es una muerte normal por memoria; `CRASH`, `CRASH_NATIVE` o `ANR` exigen **parar y analizar el stack**. En el único caso que se dio no hubo evidencia de fallo (no se llegó a revisar `logcat -b crash`) y se repitió la captura. Un `BACK` en la lista (el destino inicial) manda la app a segundo plano en Android 12 sin cerrarla, así que si un `tap` no abre el detalle, el recorrido puede acabar en el launcher.

### 3.7 Fase 3: heap dumps, LeakCanary y `meminfo` (`debug`)

- **Variante:** `debug` (`DEBUGGABLE`), con un solo dispositivo conectado. LeakCanary 2.14 solo en `debugImplementation` (commit `a21a747`); no llega a `benchmark` ni a `release`. Procedimiento completo y problemas conocidos en `03-guia-leakcanary.md`.
- **Capturas:** (a) lista cargada; (b) dentro del detalle; (c) y (c2) tras lista→detalle→volver ×3; (b') dentro del detalle con el arreglo de H9. Resultados en §11.4.
- **Force GC antes de cada captura:** `adb shell run-as <paquete> kill -10 <PID>` y comprobar en `adb logcat -d` la línea `Explicit concurrent copying GC freed`. Sin esa comprobación el dump puede incluir objetos ya inalcanzables que aún no se recolectaron.
- **Conteos:** el Profiler cuenta **todos** los objetos del dump. Para saber qué está realmente retenido se analizó el `.hprof` con un script propio contando las instancias **alcanzables por referencias fuertes** desde los GC roots. Cada cifra es de una captura (n = 1).
- **LeakCanary:** con la app visible y un solo objeto retenido no vuelca el heap (umbral de 5); hay que pasar a Home *dentro de la pantalla inspeccionada*. En el realme el análisis se queda bloqueado en segundo plano: abrir la app desde Recientes.
- **`meminfo`:** `dumpsys meminfo` al inicio, al final del recorrido y tras un GC forzado. Se compara `Heap Alloc` (objetos) con el PSS del `Java Heap:` (páginas tocadas).

---
## 4. Glosario de términos

Las definiciones generales son de Android, Perfetto y Glide. La columna "En MovieDB" resume lo observado y lleva la marca ✅ o 🔎.

### 4.1 Entorno, builds y Profiler

| Término | Qué es | En MovieDB |
|---|---|---|
| `benchmark` | Variante de build que hereda de `release` (R8, `shrinkResources`), no es debuggable y es `profileable` | Única variante con la que se toman números y traces |
| `debug` | Variante debuggable con instrumentación JaCoCo | Solo para observación (logs de Glide, heap dumps) |
| `flags=` | Línea de `dumpsys package <pkg>` con las banderas del paquete | Debe **no** incluir `DEBUGGABLE` (`TEST_ONLY` sí puede aparecer) |
| Profileable | El Profiler puede perfilar la app con poco overhead sin ser debuggable | El proceso debe mostrar **Profileable** y no **Debuggable** |
| System Trace | Tarea del Profiler (*Capture System Activities*) que graba la actividad del sistema como un trace de Perfetto | T1–T5 |
| *Start profiler task from* | Punto de inicio de la tarea: sobre el proceso ya abierto (*Now*) o desde el arranque (*Process start*) | Se usa *Now* y se abre la app antes con `am start -W` |
| *Past Recordings* | Pestaña del Profiler donde queda la grabación | Desde ahí se exporta el `.trace` |
| Overhead del Profiler | El perfilado añade carga | Sus tiempos **nunca** entran en las tablas de medianas |
| Perfetto UI | `ui.perfetto.dev`: visor y consola SQL de traces | Se usa el visor y la pestaña *Query (SQL)* |
| Force GC / heap dump | Herramientas del Memory Profiler para la Fase 3 (leaks) | Pendientes |
| LeakCanary | Librería que detecta objetos retenidos; solo en `debug` | Pendiente (Fase 3) |

### 4.2 Métricas de `adb`

| Término | Qué es | En MovieDB |
|---|---|---|
| `am start -W` | Lanza la Activity y espera a que termine; imprime `LaunchState`, `TotalTime` y `WaitTime` | `LaunchState: COLD` confirma un arranque en frío |
| `TotalTime` | Milisegundos hasta el **primer frame** de la Activity (no hasta que la lista es visible; para eso haría falta `reportFullyDrawn()`) | ✅ Mediana 366 ms (baseline) y 393 ms (H0): sin mejora. En `debug` sale ~1.5 s y no es comparable |
| `dumpsys meminfo` | Memoria del proceso por categorías | Se guarda la salida completa y la filtrada |
| Java Heap | Memoria del heap de ART | ✅ ~18.5 MB en ambas versiones |
| Native Heap | Memoria nativa (en Android 8+ incluye los píxeles de los `Bitmap`; también SQLite, red, cachés de Skia) | No se atribuye a Glide sin comprobarlo |
| Graphics | Memoria de GPU (texturas, buffers, layers) | ✅ Bajó de ~88 a ~28 MB al inicio con H0 |
| TOTAL PSS | Suma ponderada de memoria; varía mucho entre corridas | Se reportan **valores absolutos de inicio y final por categoría**, no el delta del PSS (H0 cambia el estado inicial por diseño) |
| `Views` | Número de `View` vivas | ✅ 278 → 112. Si es igual al número de ítems, el `RecyclerView` no recicla; hay que contar los `ViewHolder` |
| `Heap Alloc` | Objetos vivos en el heap | Distingue *churn* de retención |
| *Churn* frente a retención | Churn: se asigna y se libera mucho. Retención: los objetos siguen vivos | Se confirma con heap dump (Fase 3) |
| `dumpsys gfxinfo <pkg> reset` | Pone a cero las estadísticas de frames | Va justo antes del scroll |
| `Total frames rendered` | Frames dibujados desde el `reset` | ~1 400–1 460 por corrida; un valor muy distinto indica recorrido interrumpido |
| *Janky frames* (actual) | Frames que fallaron su deadline según el sistema | Es la métrica de jank que se reporta |
| *Janky frames (legacy)* | Frames que superan ~16.6 ms | Más ruidosa; se reporta aparte y etiquetada |
| 50th/90th/95th/99th | Percentiles del tiempo de frame en ms **enteros** | El p99 es lo que nota el usuario; no distingue cambios sub-milisegundo |
| GPU frente a total | Si los percentiles de GPU están muy por debajo del total, el cuello está en CPU (hilo principal o `RenderThread`) | ✅ En el baseline la GPU gastaba 6–8 ms por frame |
| Mann-Whitney | Contraste de rangos entre dos grupos | Con 3 frente a 5 el mínimo p es 1/56 ≈ 0.018 |

### 4.3 Identificadores en Perfetto

| Término | Qué es | Trampa |
|---|---|---|
| `pid` | Identificador de proceso del sistema; el hilo principal tiene `tid = pid` | Aparece junto al nombre (`com.alexvicente.moviedb 4161`) |
| `upid` | Identificador **interno** del trace para el proceso | **No es el `pid`**. Se obtiene con la consulta Q0 |
| `utid` | Identificador interno de hilo | Se usa en los `join thread … using (utid)` |
| Token | Número de un frame (`surface_frame_token`, `display_frame_token`, el que acompaña a `doFrame` y a `DrawFrames`) | Sirve para buscar el mismo frame en el hilo principal, la `RenderThread` y la frame timeline |
| `ts`, `dur` | Inicio y duración en nanosegundos | `dur = -1` significa **frame incompleto** |
| `seg` | Segundos relativos al primer frame con `dur > 0` | Convención de nuestras consultas para situar eventos en el recorrido |
| Duración total del trace | Cabecera de Perfetto | 🔎 En T3 mostraba 15 h por una causa sin explicar (¿reloj?); no afectó a las consultas filtradas por frames |

### 4.4 Frame timeline y etiquetas de jank

Tabla: `actual_frame_timeline_slice` (columnas usadas: `upid`, `surface_frame_token`, `display_frame_token`, `ts`, `dur`, `jank_tag`, `jank_type`, `present_type`, `on_time_finish`).

| Término | Qué significa | En MovieDB |
|---|---|---|
| *Expected Timeline* / *Actual Timeline* | Pistas con cuándo debía terminar el frame y cuándo terminó | Un slice por frame; el color indica el jank |
| *Surface frame* | El frame de la app | Su token es `surface_frame_token` |
| *Display frame* | El frame que compone SurfaceFlinger | Su token es `display_frame_token`; Perfetto dibuja una línea (*flow*) entre ambos al seleccionar el slice |
| `No Jank` | Frame a tiempo | ~11–16 % de los frames (T1–T4) |
| `Self Jank` | Jank atribuido al propio proceso | ✅ 6–15 frames por trace, la mayoría en `BACK` o incompletos |
| `Other Jank` | Jank atribuido a otro proceso o al sistema | 70–121 frames; sin interpretar |
| `Buffer Stuffing` | El frame llegó tarde porque la cola de buffers estaba llena por frames anteriores | ✅ ~78–80 % de los frames en todos los traces, con y sin shaders; 🔎 sin diagnosticar. **No significa "frame barato"**: puede incluir un frame de 83.9 ms |
| `jank_type` | Detalle: `App Deadline Missed`, `SurfaceFlinger CPU Deadline Missed`, `Prediction Error`, `Buffer Stuffing`, `Unknown Jank` | Se consulta por token |
| `present_type` = `Late Present` | El frame se presentó después de lo previsto | Frames del `BACK` y de la apertura del detalle |
| `on_time_finish` | 1 si la app terminó a tiempo | En Buffer Stuffing es 1: no lo causa el trabajo de la app (🔎) |
| Frame **incompleto** | `dur = -1`: se perdió el evento de fin (Perfetto lo descarta por timestamp negativo) | ✅ 6 en T1, 6 en T2, 12 en T3, 3 en T4. Son frames reales; **los peores pueden estar entre ellos** |
| `trace_sorter_negative_timestamp_dropped` | Contador de eventos de fin descartados | Está en `stats` (consulta Q7) |

### 4.5 Slices del hilo principal

| Slice | Qué es |
|---|---|
| `Choreographer#doFrame <token>` | Trabajo del hilo principal para un frame (input, animación, traversal). Es la unidad que se compara entre traces |
| `Choreographer#skippedFrames` | Aparece dentro de `doFrame` cuando el frame se procesa tarde. 🔎 El número que lleva (`skippedFrames 2`) parece indicar los frames saltados |
| `traversal` | Recorrido de la jerarquía de vistas (`measure`, `layout`, `draw`) |
| `layout` / `measure` | Fases de medida y colocación |
| `draw` | Fase de dibujo del hilo principal: **graba** las órdenes de dibujo; si es larga y casi no tiene hijos, el hilo está **esperando** a la `RenderThread` |
| `Record View#draw()` y `HWUI:<Clase>` | Grabación de la display list de cada vista (jerarquía anidada) |
| `RV OnLayout` | Layout del `RecyclerView`; contiene `onCreateViewHolder` y `onBindViewHolder` |
| `RV onCreateViewHolder type=0x0` | Creación de un `ViewHolder` (incluye su `inflate`) |
| `RV onBindViewHolder` | Enlace de datos |
| `inflate` | Inflado de un layout XML |
| `animation` | Callbacks de animación del frame |
| `ViewPostImeInputStage` | Procesamiento de entrada (el `tap` o el swipe) |
| `Lcom/…/Clase;` (con el descriptor de la clase) | 🔎 Parecen slices de carga o inicialización de clases; aparecen para `Chip` y para `MovieDetailsFragment` |
| `prepare SELECT …` / `executeForCursorWindow SELECT …` | Slices del SQL de Room/SQLite; permiten saber **qué consultas** hace la vista |

### 4.6 Slices de la `RenderThread` y de la GPU

| Slice | Qué es |
|---|---|
| `DrawFrames <token>` | Trabajo de la `RenderThread` para un frame; **el frame que dibuja puede ser el anterior al `doFrame` en el que lo ves** |
| `syncFrameState` / `prepareTree` | Sincronización con el hilo principal y preparación del árbol de render |
| `flush commands` | Ejecución de las órdenes de dibujo; aquí caen las esperas por shaders |
| `flush layers` | Dibujo de las capas (layers) intermedias |
| `renderFrameImpl` | Renderizado del frame |
| `drawLayer [Clase] AxB` | Una vista se dibuja en una **capa** de ese tamaño. Una capa de pantalla completa (`CoordinatorLayout 1080.0 x 2158.0`) es sospechosa de transición o alfa |
| `VectorDrawable repaint AxB` | Un `VectorDrawable` se rasteriza a ese tamaño |
| `Bitmap#prepareToDraw AxB` | Preparación de un bitmap para dibujarlo |
| `Texture upload(<id>) AxB` | Subida de una textura a la GPU |
| `shader_compile` | Compilación (o búsqueda) de un programa de shader |
| `cache_miss` / `cache_hit` | El shader no estaba / sí estaba en el caché de shaders |
| `driver_compile_shader`, `driver_link_program` | Compilación y enlazado del programa en el driver (dentro de un `cache_miss`) |
| `eglSwapBuffersWithDamageKHR`, `queueBuffer` | Entrega del buffer al compositor |
| `dequeueBuffer` | La `RenderThread` pide un buffer; **esperar aquí** encaja con Buffer Stuffing |
| *GPU completion*, `waiting for GPU completion` | Hilo que espera a que la GPU termine |
| `[Incomplete] DrawFrames …` | Slice raíz sin cierre; desplaza las profundidades (`depth`) de sus hijos |

### 4.7 Glide

| Término | Qué es | En MovieDB |
|---|---|---|
| `DataSource` | Origen de una imagen: `REMOTE` (red), `DATA_DISK_CACHE` (datos originales en disco), `RESOURCE_DISK_CACHE` (recurso ya decodificado en disco), `MEMORY_CACHE`, `LOCAL` | ✅ Observados `REMOTE`, `DATA_DISK_CACHE` y `MEMORY_CACHE` |
| Estrategia `AUTOMATIC` | Valor por defecto: guarda en disco los datos **originales** de las imágenes remotas | Por eso tras reiniciar sale `DATA_DISK_CACHE` y se decodifica de nuevo (coste no medido) |
| Clave de la caché de memoria | Incluye el tamaño de destino | 🔎 Una misma URL en vistas de distinto tamaño no comparte entrada de memoria |
| `withCrossFade()` | Transición entre placeholder e imagen (300 ms por defecto). Glide la omite cuando la imagen sale de `MEMORY_CACHE` | Aplicada en `loadUrl` |
| Placeholder | Drawable mostrado mientras carga | `ic_movie_placeholder`, un vector, para todas las vistas |
| `Glide.with(context)` | Asocia la petición al ciclo de vida del contexto (una Activity, no el Fragment) | 🔎 Hipótesis G6: sin medir |
| Tamaños de TMDB | `w92`, `w154`, `w185`, `w342`, `w500`, `w780`… | Lista: `w500` para una vista de 240 px; backdrop: `w780` para 1080 px (🔎 se amplía) |

### 4.8 Otros términos del proyecto

| Término | Qué es |
|---|---|
| Caché de shaders | Caché persistente de programas de shader compilados. Borrar los datos de la app **probablemente** la vacía (✅ efecto observado en T3 frente a T4; mecanismo 🔎) |
| Calentamiento (*warm-up*) | Recorrido previo sin capturar para llenar cachés |
| TTL de Room | 30 min: pasado ese tiempo la lista vuelve a pedirse a la red |
| Corrida atípica | Corrida con valores lejanos pero estado inicial correcto; se conserva si no se documenta una causa |
| H0, H7, G1–G6 | Identificadores de hipótesis: H0 = `RecyclerView` sin reciclar; H7 = recarga al volver; G* = hipótesis de Genres |

---
## 5. Consultas de Perfetto reutilizables

Se ejecutan en *Query (SQL)* de `ui.perfetto.dev`, **de una en una**. Sustituye los marcadores: `<UPID>`, `<PID>` (el `pid` del proceso, que es también el `tid` del hilo principal), `<TOKEN>`, `<SEG>`. Todas se ejecutaron en los traces de MovieDB salvo donde se indica.

**Q0 — Identificar el proceso** (obtener el `upid`):

```sql
select upid, pid, name, start_ts from process where name like '%moviedb%'
```

**Q1 — Frames por etiqueta de jank** (con incompletos):

```sql
select jank_tag, count(*) as frames, round(avg(dur)/1e6,1) as avg_ms,
       round(max(dur)/1e6,1) as max_ms, sum(dur < 0) as incompletos
from actual_frame_timeline_slice
where upid = <UPID>
group by jank_tag order by frames desc
```

El `avg_ms` de `Self Jank` no sirve si hay incompletos (promedia duraciones perdidas).

**Q2 — Dónde caen los frames malos** (segmentos de 2 s desde el primer frame):

```sql
with f as (select ts, dur, on_time_finish from actual_frame_timeline_slice
           where upid = <UPID> and dur > 0),
t0 as (select min(ts) as t0 from f)
select cast((ts - t0)/2e9 as int)*2 as desde_seg, count(*) as frames,
       sum(on_time_finish = 0) as no_a_tiempo, sum(dur > 33e6) as mas_de_33ms,
       round(max(dur)/1e6,1) as max_ms
from f, t0 group by 1 order by 1
```

**Q3 — Los 10 `doFrame` más largos del hilo principal:**

```sql
select s.name, round(s.dur/1e6,1) as dur_ms, s.ts
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid)
where t.upid = <UPID> and t.tid = <PID> and s.name like 'Choreographer#doFrame%'
order by s.dur desc limit 10
```

**Q4 — `ViewHolder` creados, en orden temporal:**

```sql
select s.ts, round(s.dur/1e6,1) as dur_ms, s.name
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid)
where t.upid = <UPID> and s.name like 'RV onCreateViewHolder%'
order by s.ts
```

**Q5 — SQL de Room que hace la vista** (sin filtro de proceso: incluye ruido de GMS y WorkManager; filtra por los nombres de tabla de la vista que analices):

```sql
select s.name, s.ts, round(s.dur/1e6,1) as dur_ms
from slice s
where s.name like '%is_popular%' or s.name like '%MAX(cached_at)%' or s.name like '%movie_details%'
order by s.ts
```

Un resultado vacío **no prueba nada por sí solo**: hace falta un **control positivo** (una consulta que sí debe aparecer, como `movie_details` al abrir el detalle). Una consulta amplia (`%SELECT%`, `%INSERT%`) sin filtro de proceso sirve para comprobar que el trace registra el SQL.

**Q6 — Shaders en la `RenderThread`** (top 10). Los nombres de `driver_*` salen de los traces de MovieDB; la versión original usaba `driver_compile`, que **no coincide** con `driver_compile_shader`:

```sql
select s.name, round(s.dur/1e6,1) as dur_ms, s.ts
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid)
where t.upid = <UPID> and t.name = 'RenderThread'
  and s.name in ('shader_compile', 'cache_miss', 'cache_hit', 'driver_compile_shader', 'driver_link_program')
order by s.dur desc limit 10
```

**Q6b — Cuántos `shader_compile` y cuánto suman** (para comparar traces):

```sql
select count(*) as n, round(sum(s.dur)/1e6,1) as total_ms, round(max(s.dur)/1e6,1) as max_ms
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid)
where t.upid = <UPID> and t.name = 'RenderThread' and s.name = 'shader_compile'
```

**Q7 — Calidad del trace** (eventos descartados o fuera de orden):

```sql
select name, value from stats where value > 0 and severity in ('error','data_loss') order by value desc
```

**Q8 — Línea de tiempo por fases** (frames pesados, shaders, `ViewHolder` y consultas del detalle, con segundos relativos). Ajusta el umbral (`25e6` o `33e6`):

```sql
with t0 as (select min(ts) as t0 from actual_frame_timeline_slice where upid = <UPID> and dur > 0)
select round((s.ts - t0.t0)/1e9, 2) as seg, s.name, round(s.dur/1e6,1) as dur_ms
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid), t0
where t.upid = <UPID> and (
      (t.tid = <PID> and s.name like 'Choreographer#doFrame%' and s.dur > 25e6)
   or (t.name = 'RenderThread' and s.name in ('shader_compile','cache_miss'))
   or s.name like 'RV onCreateViewHolder%'
   or s.name like 'prepare SELECT * FROM movie_details%')
order by s.ts
```

**Q9 — Qué ocupa el hilo principal en un frame** (hijos más largos de un `doFrame`):

```sql
select d.depth, d.name, round(d.dur/1e6,1) as dur_ms
from descendant_slice((select id from slice where name = 'Choreographer#doFrame <TOKEN>' limit 1)) d
order by d.dur desc limit 15
```

**Q10 — Qué corre en la `RenderThread` durante ese frame** (+20 ms de margen). **Limitación:** `s.ts >= w.ts` deja fuera slices que **empezaron antes** del frame (por ejemplo el `DrawFrames` del frame anterior, que es justo donde suelen estar los shaders), y no asigna cada hijo a su `DrawFrames`:

```sql
with w as (select ts, ts + dur as te from slice where name = 'Choreographer#doFrame <TOKEN>' limit 1)
select s.depth, s.name, round(s.dur/1e6,1) as dur_ms
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid), w
where t.upid = <UPID> and t.name = 'RenderThread'
  and s.ts >= w.ts and s.ts < w.te + 20e6
order by s.dur desc limit 15
```

Variante sugerida (🔎 **no ejecutada**), que incluye lo que empezó antes del frame:

```sql
with w as (select ts, ts + dur as te from slice where name = 'Choreographer#doFrame <TOKEN>' limit 1)
select s.depth, s.name, round(s.dur/1e6,1) as dur_ms, round((s.ts - w.ts)/1e6,1) as inicio_rel_ms
from slice s
join thread_track tt on s.track_id = tt.id
join thread t using (utid), w
where t.upid = <UPID> and t.name = 'RenderThread'
  and s.ts < w.te + 20e6 and s.ts + s.dur > w.ts
order by s.dur desc limit 15
```

**Q11 — Lista de `Self Jank` con su momento en el recorrido:**

```sql
select round((ts - (select min(ts) from actual_frame_timeline_slice where upid = <UPID> and dur > 0))/1e9, 2) as seg,
       surface_frame_token, round(dur/1e6,1) as dur_ms, jank_type
from actual_frame_timeline_slice
where upid = <UPID> and jank_tag = 'Self Jank'
order by ts
```

**Q12 — Los dos lados de un display frame** (la app y SurfaceFlinger):

```sql
select upid, surface_frame_token, display_frame_token, round(dur/1e6,1) as dur_ms,
       jank_tag, jank_type, present_type, on_time_finish
from actual_frame_timeline_slice
where display_frame_token = <TOKEN>
```

**Q13 — Etiqueta y tipo de jank de frames concretos:**

```sql
select surface_frame_token, round(dur/1e6,1) as dur_ms, jank_tag, jank_type
from actual_frame_timeline_slice
where upid = <UPID> and surface_frame_token in (<TOKEN1>, <TOKEN2>)
```

**Q14 — Trabajo de la `RenderThread` ligado a imágenes y capas** a partir de un instante (`<SEG>`), para comparar dos aperturas de una pantalla:

```sql
with t0 as (select min(ts) as t0 from actual_frame_timeline_slice where upid = <UPID> and dur > 0),
r as (
  select round((s.ts - t0.t0)/1e9, 2) as seg, s.name, round(s.dur/1e6,1) as dur_ms
  from slice s
  join thread_track tt on s.track_id = tt.id
  join thread t using (utid), t0
  where t.upid = <UPID> and t.name = 'RenderThread'
    and (s.name like 'VectorDrawable repaint%' or s.name like 'Bitmap#prepareToDraw%'
         or s.name like 'Texture upload%' or s.name like 'drawLayer [%')
)
select * from r where seg > <SEG> order by seg
```

---

## 6. Comandos `adb` y PowerShell reutilizables

Se ejecutan en PowerShell (Windows), desde la raíz del proyecto.

**Sesión:**

```powershell
$pkg = "com.alexvicente.moviedb"
$env:ANDROID_SERIAL = "<serie del dispositivo>"     # solo si hay más de un dispositivo
adb devices
adb shell dumpsys package $pkg | Select-String "flags=|lastUpdateTime"     # sin DEBUGGABLE; lastUpdateTime reciente
git rev-parse --short HEAD ; git status
$run = "A3" ; $run                                   # comprobar: Tee-Object sobrescribe sin avisar
```

**Estado limpio y backend caliente:**

```powershell
adb shell am force-stop $pkg
adb shell pidof $pkg                                  # no debe devolver nada
curl.exe -s -o NUL -w "backend: HTTP %{http_code} en %{time_total}s`n" -X POST "https://moviedb-graphql-proxy.onrender.com/graphql" -H "Content-Type: application/json" --data "@body.json"
```

(Los datos y la caché se borran **a mano** desde *Info de la app*; `pm clear` falla en este dispositivo.)

**Foto del estado del teléfono:**

```powershell
& {
  adb shell dumpsys battery | Select-String "level|temperature|powered"
  adb shell dumpsys thermalservice | Select-String "Thermal Status"
  adb shell cat /proc/meminfo | Select-String "MemAvailable"
  adb shell dumpsys display | Select-String "mActiveSfDisplayMode"
} | Tee-Object "mediciones\$run-0-estado.txt"
```

**Cold start, memoria y frames:**

```powershell
adb shell am start -W -n $pkg/.MainActivity | Tee-Object "mediciones\$run-1-arranque.txt"
adb shell dumpsys meminfo $pkg | Tee-Object "mediciones\$run-2-mem-inicio-completo.txt" | Select-String "Java Heap:|Native Heap:|Graphics:|TOTAL PSS:|Views:" | Tee-Object "mediciones\$run-2-mem-inicio.txt"
adb shell dumpsys gfxinfo $pkg reset
# ... recorrido ...
adb shell dumpsys meminfo $pkg | Tee-Object "mediciones\$run-3-mem-final-completo.txt" | Select-String "Java Heap:|Native Heap:|Graphics:|TOTAL PSS:|Views:" | Tee-Object "mediciones\$run-3-mem-final.txt"
adb shell dumpsys gfxinfo $pkg | Select-String "Total frames|Janky frames|50th|90th|95th|99th" | Tee-Object "mediciones\$run-4-frames.txt"
```

**Recorrido del escenario A** (3 ciclos de scroll, abrir el detalle y volver):

```powershell
1..3 | ForEach-Object {
  1..4 | ForEach-Object { adb shell input swipe 540 1800 540 600 350; Start-Sleep -Milliseconds 800 }
  1..4 | ForEach-Object { adb shell input swipe 540 600 540 1800 350; Start-Sleep -Milliseconds 800 }
}
adb shell input tap 540 700
Start-Sleep 2
adb shell input keyevent KEYCODE_BACK
Start-Sleep 2
```

**Comprobar que el recorrido terminó en la app** (el `BACK` en la raíz saca la app de primer plano):

```powershell
adb shell dumpsys activity activities | Select-String "topResumedActivity|mResumedActivity"
```

Para detectar el momento exacto en que muere el proceso, entre los pasos del recorrido: `"pid: " + (adb shell pidof $pkg)`.

**Verificar los 7 archivos y compartir una corrida:**

```powershell
Get-ChildItem "mediciones\$run-*.txt" | Select-Object Name
Get-ChildItem "mediciones\$run-*.txt" | Sort-Object Name | ForEach-Object { "=== $($_.Name) ==="; Get-Content $_.FullName; "" } | Set-Clipboard
```

**Resumen de `DataSource` de Glide** (hay que **redefinirla en cada terminal nueva**; antes de cada situación: `adb logcat -c`):

```powershell
function Glide-Resumen($archivo) {
  adb logcat -d -s GlideSrc:D | Tee-Object -FilePath $archivo | Out-Null
  Get-Content $archivo | Select-String "GlideSrc: (\w+)" |
    ForEach-Object { $_.Matches[0].Groups[1].Value } |
    Group-Object | Select-Object Name, Count
}
```

Para ver las líneas completas con las URLs: `Get-Content <archivo>`. **No** hagas `Get-Content (adb logcat … | Tee-Object …)`: PowerShell interpreta cada línea del log como una **ruta** y falla (el `Tee-Object` sí escribe el archivo).

URLs descargadas más de una vez:

```powershell
Get-Content mediciones\glide\1-primera-carga.txt, mediciones\glide\2-scroll.txt | Select-String "REMOTE (\S+)" | ForEach-Object { $_.Matches[0].Groups[1].Value } | Group-Object | Where-Object Count -gt 1
```

**Diagnóstico y procesos:**

```powershell
adb shell dumpsys activity exit-info $pkg
adb logcat -b crash -d
adb shell dumpsys activity lru | Select-String moviedb          # `cch` = proceso cacheado (para `am kill`)
```

---

## 7. Cómo leer un frame pesado (guía de decisión)

1. **Localízalo y sitúalo en el recorrido.** Q3 da los `doFrame` más largos y Q8 o Q2 los sitúan por segundo (scroll, abrir el detalle, `BACK`).
2. **Comprueba si está completo.** Q11 y la columna `incompletos` de Q1: un `dur = -1` oculta el frame real.
3. **Mira qué hace el hilo principal** (Q9):
   - `layout` → `RV OnLayout` con `onCreateViewHolder` e `inflate` grandes: **trabajo del hilo principal** (crear vistas).
   - `draw` largo con hijos de apenas 1–2 ms (`Record View#draw()`, `HWUI:…`): el hilo **espera** a la `RenderThread`.
4. **Si espera, mira la `RenderThread`** (Q10 y, mejor, la variante que incluye el `DrawFrames` anterior):
   - `shader_compile` con `cache_miss` (`driver_compile_shader`, `driver_link_program`): **caché de shaders vacío**.
   - `drawLayer [...]` grandes, `VectorDrawable repaint`, `Bitmap#prepareToDraw`, `Texture upload`: **drawables, bitmaps y capas**.
   - `dequeueBuffer`: espera de buffer; encaja con Buffer Stuffing.
5. **Mira la frame timeline** (Q12, Q13): `App Deadline Missed` frente a `SurfaceFlinger CPU Deadline Missed`, `Late Present` y `Prediction Error`.
6. **Repite con otra condición** para separar causas (por ejemplo, caché de shaders lleno frente a vacío, o primera frente a segunda apertura).
7. **Cruza con la observación de la fuente** (Glide `DataSource`, SQL de Room) si el frame depende de imágenes o datos.
8. **Recuerda el límite:** un trace es n = 1 y con overhead. Si algo importa, repite la captura y anota lo que no cuadra.

---

## 8. Patrones y artefactos encontrados en MovieDB

| Síntoma | Evidencia | Causa | Estado |
|---|---|---|---|
| `Views` ≈ número de ítems y 20 `onCreateViewHolder` en un frame; `doFrame` de 181.7 ms | T1; `meminfo` | `RecyclerView` `wrap_content` con `nestedScrollingEnabled=false` dentro de un `NestedScrollView`: se inflan todos los ítems | ✅ H0 corregido (commit `a31493c`) |
| Consultas de Room tras el `BACK` | T1, T2 (consultas); Logcat (2 llamadas) | `savedInstanceState` llega `null` al volver del back stack, y la restauración del `EditText` dispara el `TextWatcher` | ✅ H7 corregido; verificado con Logcat, test de regresión, T3 y T4. **Sin mejora de tiempos medida** |
| `shader_compile` + `cache_miss` de 50–65 ms en dos puntos fijos del recorrido (scroll y apertura del detalle) | T1, T3 | Caché de shaders vacío por borrar los datos antes de cada corrida | ✅ T4 y T5: 5 `shader_compile` suman ~11–12 ms y no hay `cache_miss` (n = 2). Los valores absolutos de las medianas **incluyen** este coste |
| `BACK` con `doFrame` de 57–62 ms | T3, T4, T5 (4 `BACK`) | `layout` 46.7 ms → `RV OnLayout` 44.3 ms con 5 `onCreateViewHolder` (~26–29 ms; el primero con un `inflate` de ~10.6 ms): se recrea la vista de la lista | ✅ Coste real, independiente de cachés. 🔎 Qué lo reduciría, sin probar |
| Primera apertura del detalle: `doFrame` de 34–40 ms con caché de shaders lleno | T4, T5 | `VectorDrawable repaint` de 1080x1080 y 495x495 (placeholder), `Bitmap#prepareToDraw`, `Texture upload`, capas de pantalla completa y ~0.3 s de `drawLayer [ShapeableImageView]` | 🔎 La 2.ª apertura (imágenes en memoria) no tiene ningún `doFrame` > 25 ms. Placeholder y cross-fade desaparecen juntos y el experimento no los separa. El vector se repinta **dos veces**, no frame a frame |
| Buffer Stuffing en ~80 % de los frames | T1–T4 | Sin diagnosticar; no lo causa el trabajo de la app (`on_time_finish = 1`) | 🔎 No depende de los shaders |
| Póster del detalle ≠ póster de la lista | Glide 5a | Sin investigar (hipótesis: `poster_path` distinto entre fuentes) | 🔎 |
| `w500` para una vista de 240 px; `w780` para un backdrop de 1080 px | Layouts y URLs | Tamaños pedidos a TMDB | 🔎 Bytes sin medir |
| Un `RecyclerView`, 5 `MovieViewHolder`, 5 `ItemMovieBinding` y 6 `Bitmap` de la lista siguen vivos mientras se está en el detalle | Heap dump (b); LeakCanary: 1 *application leak* (2.5 MB en 6 849 objetos) | `PopularMoviesFragment` conserva el adapter como propiedad y el adapter mantiene registrado al `RecyclerView` de la vista destruida (`adapter → mObservable → mObservers → RecyclerViewDataObserver → RecyclerView`) | ✅ H9 corregido (`661af70`). Verificado con test rojo/verde, LeakCanary sin retención (3 min 16 s) y heap dump (b'): 0 `RecyclerView`, 0 `MovieViewHolder`. Una captura de cada |
| Tras lista→detalle→volver ×3 no hay acumulación | Heap dump (c2) con GC confirmado: 1 `RecyclerView`, 5 `ViewHolder`, 0 `MovieDetailsFragment` | La retención de H9 era transitoria (se libera al recrear la vista) | ✅ Sin leak permanente de Fragments; el `Bitmap` de 1599×900 lo retiene el `LruBitmapPool` de Glide (pool, no leak) |
| `FavoritesViewModel.observeIsFavorite` acumula collectors del `Flow` de Room | Lectura de código; test con un flow que no termina: `expected 1 / but was 2` | `isFavoriteJob?.cancel()` nunca cancelaba nada porque el `Job` no se asignaba | ✅ Corregido (`c96331e`), 17/17 en `FavoritesViewModelTest`. Efecto en la app no medido |
| Crecimiento de ~12 MB del `Java Heap` (PSS) durante el escenario A | `meminfo` (H0, n = 5): `Heap Alloc` +1.1 MB; PSS +12.2 MB; `Dalvik Heap` PSS +7.6 MB | No es retención de objetos Java: las páginas del heap se tocan al asignar | 🔎 Interpretación como *churn* sin medir asignaciones; ~4.6 MB sin atribuir a una categoría |
| Capa `drawLayer [CoordinatorLayout]` de pantalla completa en las transiciones | Análisis de traces previo (pendiente) | Hipótesis: animaciones `nav_default_*_anim` del nav graph sobre la raíz del Fragment | 🔎 Abierta, sin comprobar |

---

## 9. Trampas y lecciones aprendidas

- **`pid` ≠ `upid`.** Las consultas usan `upid`; el `pid` aparece junto al nombre en la cabecera. Se obtiene con Q0.
- **Frames incompletos.** Los peores frames pueden ser `dur = -1`. Siempre que cites un máximo, di si sale solo de los completos.
- **Los conteos entre traces no son comparables** si la duración de la captura difiere (1 468 frente a 1 352 frames).
- **Ventana de Q10:** deja fuera el `DrawFrames` del frame anterior, y los shaders suelen estar ahí. Usa la variante que incluye lo que empezó antes.
- **Un resultado vacío no prueba nada.** Hace falta un control positivo.
- **"No borrar datos" cambia dos cosas a la vez** (caché de shaders y cachés de datos de Room/Glide). Fíjate en señales **específicas** (`cache_miss`), no solo en el jank.
- **El protocolo de medición (datos borrados) mide con el caché de shaders vacío.** El antes/después es comparable; los absolutos no representan un uso normal.
- **No mezcles trace y medición:** el Profiler añade overhead.
- **Un trace por versión es n = 1.** No interpretes diferencias pequeñas de conteos ni de etiquetas de jank.
- **Calentamiento antes de medir "con todo cargado"** (Glide): sin él, el scroll mezcla primera carga y recargas.
- **Corre los comandos del recorrido seguidos.** El TTL de Room (30 min) y el sueño del backend (Render) cambian el escenario.
- **No uses Run ni Profile del IDE entre el estado limpio y `am start -W`:** el IDE reabre la app y llena las cachés.
- **El `BACK` en la raíz de la app** la manda a segundo plano; si el `tap` no abrió el detalle, la corrida no es válida.
- **`am kill` solo mata procesos cacheados.**
- **`Glide-Resumen` se pierde al abrir una terminal nueva.**
- **Una variante equivocada contamina los números.** Con el realme conectado, `connectedDebugAndroidTest` instala `debug` encima de `benchmark`.
- **Parches temporales de observación:** se retiran antes de commitear y se comprueban con `git grep`/`git status`.
- **Un cambio que no mejora tiempos puede seguir siendo válido** (H7: trabajo redundante y parpadeo), pero el README debe decirlo así.
- **Un test que pasa sin el arreglo no protege nada.** Hay que ver el rojo. Con `flowOf()` un collector sobrante muere solo y el test no detecta la acumulación: usa un flow que no termina (como el de Room).
- **Un heap dump incluye basura sin recolectar.** Fuerza el GC (`kill -10` + comprobar `GC freed`) y cuenta lo alcanzable por referencias fuertes; si no, 3 `RecyclerView` pueden ser 1 vivo y 2 pendientes de recolectar.
- **LeakCanary con la app visible no vuelca con < 5 retenidos.** Home dentro de la pantalla inspeccionada; si vuelves antes, el objeto ya se recolectó y no hay nada que reportar. En el realme, abre la app tras el Home o el análisis queda bloqueado.
- **`git stash push` puede decir `No local changes to save`.** Un `try/finally` no protege si cierras la ventana a mitad; comprueba `git diff --name-only` y `git stash list` antes y después.
- **PowerShell:** `else` va en la misma línea que la `}` del `if`; `Select-String` no tiene `-Recurse`; `$pkg:id` se interpreta como variable, escribe `${pkg}:id`.
- **`connectedDebugAndroidTest` corre en todos los dispositivos conectados.** Para tests funcionales, solo el emulador conectado.
- **Los tiempos de una clase de tests varían entre sesiones** (44 a 53 s para la misma clase): compara con n ≥ 2 por condición y lee el `time=` del XML de resultados, no el tiempo total de Gradle.

---

## 10. Receta para analizar otra vista (por ejemplo Genres)

1. **Lee el código** de la pantalla (Fragment, ViewModel, repositorio, adapter, layout) y escribe una lista de **hipótesis** sin afirmarlas. Confirma antes qué implementación hay (en Genres, si sigue con Paging 3 o paginación por páginas respaldada por Room).
2. **Define el recorrido** y fija sus coordenadas. Documenta qué hace cada paso.
3. **Decide y declara el protocolo:** estado limpio (caché de shaders vacío) o con calentamiento (caché lleno). Lo ideal es etiquetar cada medición con su condición. Si la pantalla no usa el backend de Render, no hace falta calentarlo (Genres usa Retrofit contra TMDB; anota la variabilidad de red).
4. **Baseline con `adb`** (n = 3) sobre un commit limpio y con la variante `benchmark`: memoria, jank, percentiles y `TotalTime`.
5. **System Trace (1–2 capturas)** del mismo recorrido, con su `PID` anotado, y Q0 para el `upid`.
6. **Consultas:** Q1, Q2, Q3, Q4, Q6b, Q7, Q8 y, para los frames pesados, Q9–Q13 siguiendo la guía de §7.
7. **Observación de la fuente** si hay imágenes o datos (parche temporal de Glide; control positivo de Room).
8. **Candidatos de mejora:** uno por vez, cada uno en su commit, con un test de regresión que **falle sin el arreglo** y n = 5 contra el baseline en la misma condición.
9. **Documenta** con el formato **hallazgo → causa → solución → verificación**, separando lo demostrado de las hipótesis y diciendo el n de cada cosa.

Para Genres hay, además, métricas de paginación (requests, ítems cargados, memoria tras 1/3/5 páginas) y las hipótesis G1 (`initialLoadSize`), G2 (`maxSize`), G5 (chips) y G6 (`Glide.with`). Genres sirve de **control** para H0: su layout ya era `match_parent` y usa el mismo `item_movie.xml`.

**Fase 3 en Genres:** el adapter (`PagingDataAdapter` con `withLoadStateFooter`, es decir, un `ConcatAdapter`) también es una propiedad del Fragment y se recrea en cada `onViewCreated`, el mismo patrón que tenía Popular. Hipótesis (sin medir): la cadena de H9 pasa aquí por el observer que el `ConcatAdapter` registra en el adapter de datos. Recorrido propuesto: entrar a un género, ~5 páginas de scroll, detalle, volver; heap dumps (a), (b) y (c2) y LeakCanary como en §3.7 y en `03-guia-leakcanary.md`. El `addLoadStateListener` de `observeMovies()` nunca se quita; revisar si sobrevive al adapter.

---

## 11. Registro de capturas

### 11.1 Traces

| Traza | PID | `upid` | Commit | Estado | Archivo | Frames de referencia |
|---|---|---|---|---|---|---|
| T1 | 11256 | n/d | `0d649b5` | Datos borrados | n/d | `doFrame` 27980665 (65.2 ms, con `shader_compile` de 53.7 ms) y 27980771 (181.7 ms, la vuelta con `BACK`) |
| T2 | 13901 | 792 | `a31493c` | Datos borrados | n/d | 37552850 (87.9 ms, apertura del detalle) y 37552949 (100.9 ms, vuelta) |
| T3 | 4161 | 751 | `3d41372` | Datos borrados | `T3-popular-scroll.trace` (78 MB) | Scroll: 2630285 (`Self Jank` 88.4 ms) y 2630287 (Buffer Stuffing 83.9 ms); detalle: 2632276 (49.6 ms), 2632315 y 2632319; `BACK`: 2632420 (62.5 ms) |
| T4 | 4083 | 733 | `3d41372` | Sin borrar datos (calentado) | `T4-popular-scroll-sin-borrar-datos.trace` (68 MB) | Detalle: 3601313 (34.4 ms); `BACK`: 3601365 (61.5 ms) y 3601367 (`Self Jank` 100.8 ms) |
| T5 | 6828 | 775 | `3d41372` | Sin borrar datos (calentado), 2 aperturas | `T5-popular-detalle-dos-aperturas.trace` (70 MB) | 1.ª apertura: 6485373 (40 ms); `BACK`: 6485430 (59.2 ms; `Self Jank` 128.7 ms) y 6485502 (57.3 ms; `Self Jank` 87 ms) |

### 11.2 Resultados de las corridas `adb` (medianas con rango; ver README)

| Métrica | Baseline (n = 3) | H0 (n = 5) | Lectura |
|---|---|---|---|
| `Views`, inicio | 278 | 112 | ✅ |
| Native Heap, inicio | 56.9 MB | 32.6 MB | ✅ |
| Graphics, inicio | 87.8 MB | 28.5 MB | ✅ |
| Janky frames | 6.00 % | 3.40 % | 🔎 indicio (p ≈ 0.13) |
| p99 | 32 ms | 29 ms | 🔎 indicio |
| `TotalTime` | 366 ms | 393 ms | sin mejora |

### 11.3 Estado del trabajo

- **Hecho:** H0, H7, análisis de shaders (artefacto del protocolo), caché de imágenes de Glide, traces T1–T5 del escenario A, y la Fase 3 de Popular (H9 `661af70`, LeakCanary en `debug` `a21a747`, `FavoritesViewModel` `c96331e`; ver §11.4).
- **Pendiente:** Fase 3 de Genres (leaks en `debug`); la capa `drawLayer [CoordinatorLayout]` y los ~4.6 MB del Java Heap sin atribuir; subir los commits locales a GitHub; optimizar el `BACK` con un baseline nuevo en la condición de caché lleno; Fase 4.1 (`androidx.tracing`); escenario B (Genres); rellenar los TODO del README (cómo reproducir, escenario B); limpieza de `Thread.sleep()` en los tests; revisión de manejo de errores (`CancellationException` en `PopularMoviesRepositoryImpl`).

### 11.4 Capturas de la Fase 3 (`debug`, realme 7 5G; conteos de un script propio sobre el `.hprof`)

| Captura | Estado | `RecyclerView` / `MovieViewHolder` | Otros datos | Archivo |
|---|---|---|---|---|
| (a) | Lista cargada, sin arreglo | 1 / 5 | 5 `Bitmap` del heap `app` (pósters 240×360) | `a-lista-cargada.hprof` |
| (b) | En el detalle, sin arreglo | 1 / 5 (retenidos por `PopularMoviesFragment.adapter`) | 9 `Bitmap`; `MovieDetailsFragment` 1 | `b-en-detalle.hprof` |
| (c) | Tras ×3, GC no confirmado | 3 / 15 en el dump; **1 / 5 alcanzables** | El resto, basura sin recolectar | `c-tras-3-vueltas.hprof` |
| (c2) | Tras ×3, GC confirmado | 1 / 5 | `MovieDetailsFragment` 0; 6 `Bitmap` (5 pósters + 1 de 1599×900 en el `LruBitmapPool`) | `c2-tras-3-vueltas-gc.hprof` |
| (d) LeakCanary | En el detalle, sin arreglo | — | 1 *application leak*; firma `56357fd8…`; `MovieAdapter` 2.5 MB en 6 849 objetos | `d-leakcanary-en-detalle.hprof` |
| (b') | En el detalle, con el arreglo (`661af70`) | **0 / 0** | `ItemMovieBinding` 0; 3 `Bitmap` (ninguno de la lista); heap `app` vivo ≈ 4.77 MiB frente a ≈ 5.15 en (b) | `b2-en-detalle-con-arreglo.hprof` |

Archivos en `mediciones\fase3-heapdumps\`. `meminfo` en `debug` (inicio → final → final tras GC): `Dalvik Heap` Heap Alloc 7 202 → 8 651 → 8 117 KB; `Java Heap:` PSS 22 708 → 24 936 → 22 464 KB; `Views` 112 → 157 → 112. Con LeakCanary tras el arreglo: sin objeto retenido en 3 min 16 s (`mediciones\f3-leakcanary-log-despues.txt`). Scroll tras volver del detalle: mismos 4 títulos visibles antes y después (`mediciones\ui-antes.xml`, `ui-despues.xml`).

---

## 12. Archivos de referencia

| Archivo | Contenido |
|---|---|
| `00-guia-profiling.md` | Guía original por fases (Fase 0 a 6) |
| `01-hallazgos-iniciales-codigo.md` | Hipótesis H0–H9 (Popular) y G1–G6 (Genres) sacadas de leer el código |
| `02-plantilla-mediciones.md` | Plantilla de tablas del README |
| `03-guia-leakcanary.md` | Guía de LeakCanary y de la Fase 3 (procedimiento, registro de corridas, problemas conocidos) |
| `guia-ejecucion-profiling.md` | Protocolo de una corrida con `adb` (v2) |
| `guia-system-trace.md` | Protocolo de captura de System Trace |
| `README-contexto-profiling.md` | Contexto y apéndices (datos crudos y consultas de Perfetto) |
| `README-seccion-performance-borrador.md` | Borrador vigente de la sección Performance del README |
