# Guía de LeakCanary — MovieDB (Fase 3, leaks)

Objetivo: detectar objetos retenidos (leaks) con **evidencia reproducible**, ver la **cadena de referencias** que los retiene y verificar que un arreglo los elimina. Complementa a `00-guia-profiling.md` (Fase 3) y rellena la tabla **Leaks** de `02-plantilla-mediciones.md`.

> **Regla de la sesión:** primero medir, después optimizar. LeakCanary es una herramienta de **observación en `debug`**: no se usa para tomar tiempos ni para comparar memoria Java contra los baselines de `benchmark`.

> **Estado de esta guía:** escrita a partir de la corrida del 21/10/2026 (realme 7 5G, Android 12, LeakCanary 2.14). Cada dato está marcado como **[observado]** (lo vimos en tu proyecto), **[docs]** (viene de la documentación oficial) o **[hipótesis]** (razonado, sin comprobar).

---

## 1. Cuándo usar LeakCanary y cuándo no

| Necesito… | Herramienta |
|---|---|
| Saber **si** hay un leak y **quién lo retiene** (cadena de referencias) | LeakCanary |
| Contar instancias de una clase (`RecyclerView`, `MovieViewHolder`…) | Heap dump del Memory Profiler (o su `.hprof`) |
| Medir crecimiento de Java Heap / PSS | `dumpsys meminfo` **sin LeakCanary instalado** |
| Medir tiempos, jank, arranque | `benchmark` (nunca `debug`) |

**Orden recomendado:** primero las mediciones de memoria (Memory Profiler, `meminfo`) en `debug` **sin** LeakCanary; después añadir LeakCanary. Con la librería instalada el heap incluye su `ObjectWatcher` y sus propios volcados, y los números dejan de ser comparables. **[hipótesis]**, razonada; no se midió el efecto.

---

## 2. Instalación

### 2.1 Cambios (temporales, sin commit)

`gradle/libs.versions.toml`:

```toml
[versions]
leakcanary = "2.14"

[libraries]
leakcanary-android = { group = "com.squareup.leakcanary", name = "leakcanary-android", version.ref = "leakcanary" }
```

`app/build.gradle.kts`, dentro de `dependencies { }`:

```kotlin
debugImplementation(libs.leakcanary.android)
```

> `2.14` es la versión que indicaba la documentación oficial al hacer la sesión. Comprueba si hay una más reciente en el repositorio de releases antes de reutilizarla.

LeakCanary **no necesita código** en `MovieDbApplication`: se instala solo. Además, referenciarlo desde `main` rompería la compilación de `benchmark` y `release`, donde no existe.

### 2.2 Verificar que solo entra en `debug`

```powershell
.\gradlew :app:dependencies --configuration debugRuntimeClasspath | Select-String leakcanary
.\gradlew :app:dependencies --configuration benchmarkRuntimeClasspath | Select-String leakcanary
.\gradlew :app:dependencies --configuration releaseRuntimeClasspath | Select-String leakcanary
```

Esperado: la **primera lista** `leakcanary-android:2.14` y sus módulos; la **segunda y la tercera no imprimen nada**. **[observado]**

> `debugAndroidTestRuntimeClasspath` también salió vacía **[observado]**: esa classpath es la del APK de pruebas, no la de la app. Que LeakCanary esté activo durante los tests instrumentados es probable (corren en el mismo proceso que la app `debug`), pero no lo comprueba ese comando. **[hipótesis]**

### 2.3 Instalar y comprobar que arrancó

```powershell
.\gradlew :app:installDebug
adb logcat -c
adb shell am force-stop com.alexvicente.moviedb
adb shell am start -W -n com.alexvicente.moviedb/.MainActivity | Select-String "LaunchState|Status"
Start-Sleep -Seconds 5
adb logcat -d | Select-String "LeakCanary"
```

Debe aparecer `LeakCanary is running and ready to detect memory leaks.` **[observado]**. Las líneas `hiddenapi ... allowed` son LeakCanary usando reflexión sobre clases internas de Android: son normales. En el launcher aparece un icono nuevo, **Leaks**.

---

## 3. Condiciones de prueba (checklist antes de cada corrida)

| Verificación | Comando | Esperado |
|---|---|---|
| Un solo dispositivo | `adb devices` | una línea `device` |
| Variante `debug` | `adb shell dumpsys package com.alexvicente.moviedb \| Select-String "flags="` | contiene `DEBUGGABLE` |
| No mantener actividades | `adb shell settings get global always_finish_activities` | `0` |
| Escalas de animación | `adb shell settings get global animator_duration_scale` (y `transition_` / `window_`) | `1.0` las tres |
| Backend caliente | `curl.exe` con `body.json` (ver glosario) | `HTTP 200` en < 1 s |
| Profiler cerrado | — | no pulsar *Start profiler task* mientras corre LeakCanary |

Si hay emulador y móvil conectados a la vez, `connectedDebugAndroidTest` corre en **ambos**. Para tests funcionales usa solo el emulador; para LeakCanary en el móvil, desconecta o cierra el emulador.

---

## 4. Qué vigila LeakCanary y cuándo analiza

**[docs]**
- Vigila Activities y Fragments destruidos, vistas de Fragment, ViewModels limpiados y Services destruidos.
- Tras destruir un objeto espera ~5 s; si no se recolectó, lo marca como **retenido**.
- Con la app **visible** no vuelca el heap hasta acumular **5** objetos retenidos (`retainedVisibleThreshold`); con la app en segundo plano el umbral baja a 1.

**[observado]** en tu proyecto:
- Lo vigilado fue la **raíz de la vista** de `PopularMoviesFragment` (el `CoordinatorLayout`) al recibir `onDestroyView()`. El **Fragment** en sí **no** se reporta mientras está en el back stack (sigue vivo y `Leaking: NO`).
- Con 1 objeto retenido y la app visible el log dice: `Found 1 object retained, not dumping heap yet (app is visible & < 5 threshold)`.
- Por eso, para provocar el volcado con un solo leak, **hay que pasar la app a segundo plano (Home)** *estando en la pantalla que quieres inspeccionar*. Si vuelves antes, el objeto ya se habrá recolectado y no habrá nada que reportar.

---

## 5. Procedimiento: escenario H9 (lista → detalle)

1. Con la app en la lista de populares y los datos cargados, **toca la primera película** y espera a que el detalle cargue (póster, título y chips visibles).
2. **Estando dentro del detalle, pulsa Home.** No navegues más.
3. Espera ~20 s. El log debería mostrar `Analysis in progress, 0% done, working on Parsing heap dump`.
4. **Abre la app desde Recientes** (multitarea) y no navegues. *Importante en este dispositivo:* tras Home, el análisis quedó **bloqueado** (hilo `HeapAnalyzer` en estado `D`, sin avanzar) casi **10 min** en `Parsing heap dump`; al traer la app a primer plano avanzó en segundos y el análisis completo duró **33 724 ms** **[observado]**. La causa interna es **[hipótesis]** (limitación de CPU en segundo plano de la capa del fabricante).
5. Lee el resultado (sección 6).

Comprobar si el análisis avanza o está bloqueado (sustituye `<PID>` y `<TID>` por los del log):

```powershell
adb shell pidof com.alexvicente.moviedb
adb shell cat /proc/<PID>/task/<TID>/stat
Start-Sleep -Seconds 10
adb shell cat /proc/<PID>/task/<TID>/stat
```

Campos útiles: el 3.º es el estado (`R` ejecutando, `D` bloqueado) y el 14.º es `utime`; si no sube entre lecturas, no está trabajando.

---

## 6. Leer y guardar el resultado

### 6.1 Extraer el informe del log

```powershell
adb logcat -d -s LeakCanary | Tee-Object "mediciones\f3-leakcanary-log.txt" | Select-Object -Last 100
```

El log completo es **más largo** que lo que muestra la pantalla de la app Leaks. Si `Select-Object -Last 60` corta el informe, extrae solo el bloque del leak:

```powershell
$l = (Get-Content "mediciones\f3-leakcanary-log.txt") -replace '^.*LeakCanary: ',''
$a = ($l | Select-String "APPLICATION LEAKS").LineNumber | Select-Object -First 1
$b = ($l | Select-String "LIBRARY LEAKS").LineNumber | Select-Object -First 1
$l[($a-1)..($b-2)]
```

Los caracteres tipo `Ôöé` / `Ôåô` son solo la codificación de la consola de PowerShell; la estructura se sigue por la sangría.

### 6.2 Copiar el `.hprof` como evidencia

El archivo queda en `Download` del móvil y podría borrarse. Cópialo (ajusta el nombre al de la línea `Heap dump file path` del log):

```powershell
adb pull "/storage/emulated/0/Download/leakcanary-com.alexvicente.moviedb/<archivo>.hprof" "mediciones\fase3-heapdumps\<nombre>.hprof"
```

No commitees los `.hprof` (decenas de MB). Pendiente: valorar añadir `*.hprof` al `.gitignore`.

### 6.3 Cómo leer la cadena

- `GC Root` arriba y el objeto vigilado (`Leaking: YES`) abajo.
- `Leaking: NO` = el objeto sigue vivo con normalidad (p. ej. `MainActivity`, o `PopularMoviesFragment` mientras está en el back stack). `UNKNOWN` = no se puede decir; `YES` = debería haberse recolectado.
- Las referencias subrayadas con **`~~~`** son las **sospechosas**: empieza por la primera (la más cercana al GC root).
- `Retaining X in N objects` en cada nodo = lo que se liberaría si ese nodo desapareciera. **Ojo:** un nodo no retiene lo que también es alcanzable por otro camino. En esta corrida el `CoordinatorLayout` retenía solo 75.3 kB porque el `RecyclerView` (2.5 MB) también cuelga de `PopularMoviesFragment.adapter`. La cifra que describe el leak es la del primer nodo sospechoso (`MovieAdapter`).
- `0 LIBRARY LEAKS` = el leak lo causa código de la app, no un bug de una librería conocida.

---

## 7. Registro de corridas (plantilla)

| Campo | Baseline (antes del arreglo) | Después del arreglo |
|---|---|---|
| Fecha / hora | 21/10/2026 17:01 | 23/10/2026 23:53 |
| Commit | `3d41372` + LeakCanary (sin arreglo) | `3d41372` + LeakCanary + arreglo (sin commitear) |
| Variante | `debug` (`DEBUGGABLE`) | `debug` (instalado con `installDebug`) |
| Dispositivo | realme 7 5G, Android 12 (API 31) | realme 7 5G, Android 12 (API 31) |
| Escenario | Detalle de una película → Home (1 ida) | Detalle de una película (1 ida), igual que el baseline |
| Objeto vigilado | `CoordinatorLayout` de `PopularMoviesFragment` (`onDestroyView`) | `CoordinatorLayout` de `PopularMoviesFragment`, key `03fc9974-90eb-4bab-8c9b-1c6890dd2cc0` |
| Retenidos / fugas distintas | 1 / 1 application leak | 0: sin `Found 1 object retained` en ≥ 3 min 16 s (PID 7536 vivo) |
| Library leaks / unreachable | 0 / 0 | — (no hubo análisis) |
| Referencia sospechosa (`~~~`) | `PopularMoviesFragment.adapter` → `mObservable` → `mObservers[0]` → `RecyclerViewDataObserver.this$0` | — (no hubo análisis) |
| Retained (primer nodo sospechoso) | `MovieAdapter`: 2.5 MB en 6 849 objetos | — |
| Retained (objeto vigilado) | `CoordinatorLayout`: 75.3 kB en 1 460 objetos | — |
| Tiempo retenido antes del volcado | 11 551 ms | — |
| Duración del análisis | 33 724 ms (app en primer plano) | No se disparó |
| Firma del leak | `56357fd80363ceefff14b83d0061616777381e44` | — |
| Log | `mediciones\f3-leakcanary-log.txt` | `mediciones\f3-leakcanary-log-despues.txt` |
| `.hprof` | `mediciones\fase3-heapdumps\d-leakcanary-en-detalle.hprof` | No se generó (no hubo volcado) |

Relación con la plantilla de mediciones (tabla **Leaks**):

| Verificación | Resultado |
|---|---|
| Fragments destruidos retenidos tras lista→detalle→volver ×3 | 0 `MovieDetailsFragment`; 1 `PopularMoviesFragment` (el vivo) |
| Instancias de `RecyclerView` vivas estando en el detalle (H9) | 1 (retenido por `PopularMoviesFragment.adapter`) |
| Instancias de `MovieViewHolder` vivas estando en el detalle (H9) | 5 |
| LeakCanary (debug) | 1 application leak (H9), 0 library leaks, 0 unreachable |

> Los conteos de `RecyclerView` / `MovieViewHolder` salen de los heap dumps (a), (b) y (c2) analizados con script, no de la interfaz de LeakCanary. Cita cada cifra con su método.

---

## 8. Verificar un arreglo (antes / después)

### 8.1 Test de regresión (emulador)

El test mide la arista real del leak: tras destruir la vista, el `adapter` que conserva el Fragment **no** debe seguir teniendo observers (`hasObservers()`).

- Test: `PopularMoviesFragmentTest.whenFragmentViewIsDestroyed_adapterIsNoLongerObservedByOldRecyclerView`.
- **Sin el arreglo:** `FAILED`, `expected to be false`, log `rv.adapter=MovieAdapter@… hasObservers=true`. **[observado]**
- **Con el arreglo** (`binding.recyclerView.adapter = null` en `onDestroyView()`, antes de `_binding = null`): pasa, log `rv.adapter=null hasObservers=false`. **[observado]**
- Clase completa con el arreglo: **12 de 12** tests en verde en el emulador (`Medium_Phone_API_36.0`), 1 min 41 s, con LeakCanary instalado. **[observado]**

Ciclo rojo/verde sin editar a mano (aparta solo el archivo del arreglo):

```powershell
$f = "app/src/main/java/com/alexvicente/moviedb/features/popular_movies/presentation/ui/PopularMoviesFragment.kt"
git stash push -m "h9-fix" -- $f
# correr el test: debe FALLAR
git stash pop
# correr el test: debe PASAR
```

### 8.2 Verificación en el dispositivo con LeakCanary

Repite exactamente la sección 5 con el arreglo aplicado y rellena la columna *Después* de la sección 7.

Esperado: ya no aparece una *application leak* con `PopularMoviesFragment.adapter` en la cadena. El texto exacto que muestra LeakCanary cuando no hay fugas no se vio en esta sesión (simplemente no hubo avisos de retención ni análisis); compara contra el baseline por la **ausencia de `Found 1 object retained`**, del análisis y de la firma `56357fd8…`.

**Resultado de la corrida «después» [observado]** (23/10/2026, mismo móvil, 1 ida al detalle): LeakCanary vigiló el `CoordinatorLayout` al entrar al detalle (`Watching instance … key 03fc9974-…`) y en los 3 min 16 s siguientes no apareció `Found 1 object retained` ni análisis alguno; el proceso seguía vivo con el mismo PID (7536) y el archivo de log solo contiene el encabezado y la línea de vigilancia. En el baseline el aviso de retención salió a los ~5 s. Es una sola corrida; no sustituye a repetirla.

**Captura (b') con el arreglo, dentro del detalle [observado]** (heap dump tras GC forzado confirmado; instancias alcanzables por referencias fuertes, conteo con script propio sobre el `.hprof`): `RecyclerView` 1 → **0**, `MovieViewHolder` 5 → **0**, `ItemMovieBinding` 5 → **0**, `Bitmap` del heap `app` 9 → **3** (ninguno de los pósters de la lista); `PopularMoviesFragment` y `MovieAdapter` siguen en 1 (los conserva el Fragment, ya sin `RecyclerView` que lo observe). Heap `app` vivo ≈ 5.15 → 4.77 MiB (aproximación, sin píxeles). Una sola captura por columna.

**Scroll al volver del detalle [observado]:** tras 4 swipes hacia abajo, abrir una película y volver, `uiautomator` listó los mismos 4 títulos visibles antes y después (`Compare-Object` sin diferencias; coincide con lo visto en el móvil). Solo compara lo visible, no el estado interno del `RecyclerView`. XML en `mediciones\ui-antes.xml` y `mediciones\ui-despues.xml`.

---

## 9. Tests instrumentados con LeakCanary presente

Clase `PopularMoviesFragmentTest` (12 tests) en el emulador `Medium_Phone_API_36.0`, tiempo de ejecución de los tests leído del XML de resultados (`time=`, no el tiempo total de Gradle, que incluye compilación). **[observado]**

| Condición | Classpath con LeakCanary | Tiempos (s) | Resultado |
|---|---|---|---|
| Con LeakCanary | `True` (comprobado) | 49.6, 47.8 | 12/12, 0 fallos |
| Sin LeakCanary | `False` (comprobado) | 50.2, 53.3 | 12/12, 0 fallos |
| Corridas previas (se infiere que sin LeakCanary: un `git stash push` posterior dijo `No local changes to save`) | no comprobado | 44.2, 47.0, 46.4, 47.1 | 12/12 |

Lectura: con n = 2 por condición, la diferencia entre con y sin (≈ 3 s, y en sentido contrario al que se temía) queda dentro de la variación entre corridas (44 a 53 s en total). No se vio un efecto de LeakCanary sobre el tiempo ni sobre la estabilidad de esta clase. No se comprobó si LeakCanary llegó a volcar el heap durante los tests.

Decisión (la pide el prompt de la Fase 3): criterio acordado = commitear aparte (`chore(debug)`) si no hay cambio apreciable de estabilidad ni de tiempo. Se cumple para esta clase. Costes que quedan: para las **mediciones de memoria Java en `debug`** (sección 1) hay que apartarlo temporalmente (`git stash`); `benchmark` y `release` no se ven afectados (sección 2.2).

---

## 10. Problemas conocidos

| Síntoma | Causa | Qué hacer |
|---|---|---|
| El log no muestra ningún análisis | Con la app visible hace falta llegar a 5 retenidos | Pasar a Home dentro de la pantalla a inspeccionar |
| `0% done, working on Parsing heap dump` durante minutos | **[observado]** app en segundo plano en este dispositivo; hilo `HeapAnalyzer` bloqueado | Abrir la app desde Recientes y no navegar |
| No aparece la notificación en el móvil | Posible bloqueo de notificaciones del fabricante; en Android 13+ puede pedir permiso **[hipótesis]** | Leer el log (`-s LeakCanary`) o abrir el icono **Leaks** |
| `Heap dump duration: Unknown` en la app y `9225 ms` en el log | Diferencia de presentación; no se investigó | Ignorar; citar el del log |
| `Select-String` sin resultados en `releaseRuntimeClasspath` | Es lo esperado | — |
| Caracteres raros (`Ôöé`) | Codificación de la consola de PowerShell | Seguir la estructura por la sangría |
| `git stash push` dice `No local changes to save` | El árbol ya estaba limpio (p. ej. un stash anterior quedó sin restaurar porque se cerró la ventana a mitad de un `try/finally`) | Comprobar `git diff --name-only` y `git stash list` **antes**; no asumir que el stash guardó algo |
| `else : El término 'else' no se reconoce` | En PowerShell `else` debe ir en la misma línea que la `}` del `if` | Escribir `} else {` en una sola línea |
| `connectedDebugAndroidTest` falla compilando en otro archivo de tests | Un bloque pegado en el archivo equivocado | `Select-String "nombreDelTest" app\src\androidTest` (sin `-Recurse`) y mover el bloque |

---

## 11. Escenarios pendientes (no medidos)

- `GenresFragment` (mismo patrón de adapter como propiedad + `addLoadStateListener`): escenario B.
- Detalle → videos → atrás.
- Rotación en lista y en detalle.
- Cambio de pestaña con el BottomNav (el Fragment se destruye del todo, no solo su vista: camino distinto al de H9).
- `FavoritesViewModel.observeIsFavorite` llamado en cada emisión de `Success` (si acumula collectors): no se ha visto su código.

---

## 12. Limpieza al terminar

Si LeakCanary se descarta, antes de restaurar comprueba que esos dos archivos solo contienen cambios de LeakCanary:

```powershell
git diff -- app/build.gradle.kts gradle/libs.versions.toml
git restore app/build.gradle.kts gradle/libs.versions.toml
```

El commit del arreglo de H9 debe contener **solo** `PopularMoviesFragment.kt` y `PopularMoviesFragmentTest.kt`, añadidos por nombre (sin `git add .`). No entran `antes-del-tap.png`, `body.json`, `mediciones/` ni los `.hprof`.

---

## Límites de la evidencia

- Una sola corrida de LeakCanary (una ida al detalle), un dispositivo, `debug`.
- Los conteos por clase y las rutas de referencias de los heap dumps (a), (b), (c2) salen de un script propio sobre los `.hprof`; coinciden con la cadena de LeakCanary, pero no son la salida de una herramienta oficial.
- La explicación del bloqueo en segundo plano y el efecto de LeakCanary sobre las pruebas instrumentadas son hipótesis hasta repetirlas.
