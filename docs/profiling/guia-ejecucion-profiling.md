# Guía de ejecución de corridas — MovieDB (profiling) · v2

Protocolo reutilizable para medir memoria, jank y arranque en el **realme 7 5G** con el build `benchmark` (*profileable*). Todos los comandos son para **PowerShell**.

> **Principio:** entre dos corridas, lo único que debe cambiar es el código. Todo lo demás (teléfono, red, backend, recorrido) se controla y se anota.

**Cambios de la v2 (tras A1–A3):** el paso 9 ahora lleva los tres comandos juntos (memoria completa + `gfxinfo`); hay una verificación de archivos al final de cada corrida; el calentamiento del backend se repite hasta responder en menos de 1 s; se documenta qué hacer con una corrida inválida; y se reporta el **cambio** de memoria, no el PSS absoluto.

---

## 0. Preparación de la sesión (una vez por sesión)

### 0.1 Teléfono

| Ajuste | Valor | Cómo |
|---|---|---|
| Frecuencia de actualización | **Estándar (60 Hz)** | Pantalla y brillo → Frecuencia de actualización (no "Automático") |
| Escalas de animación (3) | **1x** | Opciones de desarrollador → Dibujo. **A mano**: `adb` no puede escribirlas en realme (`WRITE_SECURE_SETTINGS`) |
| Mantener pantalla activa | Activado | Opciones de desarrollador → *Stay awake* (con USB) |
| Ahorro de energía | Desactivado | Ajustes de batería |
| Optimización de batería | `MovieDB` excluida | Ajustes → Batería |
| Brillo | Fijo, siempre el mismo | — |
| Modo avión | Desactivado | — |
| WiFi | **Siempre la misma red** | — |
| Cable USB | Conectado en **todas** las corridas | Mismo estado de energía |

### 0.2 Terminal

Abre una terminal **nueva** (para que tome el `PATH`) en la raíz del proyecto (`AppTest`) y define las variables:

```powershell
$pkg = "com.alexvicente.moviedb"
New-Item -ItemType Directory -Force mediciones | Out-Null
```

Las variables viven solo en esa terminal. Si la cierras, vuelve a definirlas (incluida `$run`, ver la sección 1).

Comprueba que `adb` ve el teléfono:

```powershell
adb devices
```

Debe aparecer `<SERIAL_DEL_MOVIL>` como `device` (si dice `unauthorized`, desbloquea el teléfono y acepta el aviso de depuración USB). Si hay más de un dispositivo (por ejemplo, el emulador), usa `-s <SERIAL_DEL_MOVIL>` en cada comando o fija el dispositivo con:

```powershell
$env:ANDROID_SERIAL = "<SERIAL_DEL_MOVIL>"
```

> Con `ANDROID_SERIAL` fijado, el `-s` explícito tiene prioridad. Recuerda cambiarlo si luego mides en el emulador.

### 0.3 Archivo para despertar el backend (una vez)

El cuerpo de la query mínima de GraphQL. Debe estar en la carpeta desde la que ejecutas `curl.exe`:

```powershell
'{"query":"{ __typename }"}' | Set-Content -NoNewline body.json
```

Uso un archivo porque PowerShell estropea las comillas al pasar JSON directo a `curl.exe`.

### 0.4 Verificar que el build instalado es el correcto

1. En Android Studio: *Build Variants* → `benchmark`.
2. Instalar con **Run → Profile 'app' as profileable (low overhead)** en el realme. Es el paso de **instalación**: se hace una vez, no antes de cada corrida.
3. Comprobar:

```powershell
adb shell dumpsys package $pkg | Select-String "flags="
```

- **No** debe aparecer `DEBUGGABLE`.
- `TEST_ONLY` sí puede aparecer: lo agrega Android Studio al instalar y no afecta el rendimiento.
- En el Profiler, el proceso debe decir **Profileable**.

> Si más tarde instalas `debug` (Glide, heap dumps), **vuelve a instalar `benchmark`** y repite esta comprobación antes de anotar números.

---

## 1. Protocolo de una corrida

### Identificador de corrida

**A0** es la práctica y no cuenta. **A1, A2, A3** son las que cuentan para la mediana. Define el identificador y **compruébalo**, porque de él salen los nombres de archivo:

```powershell
$run = "A3"
$run
```

Debe imprimir el identificador correcto. Si `$run` sigue con el valor de la corrida anterior, `Tee-Object` **sobrescribe sus archivos sin avisar**.

**Corrida inválida** (la lista no cargó, falta un archivo, el recorrido se interrumpió, la temperatura se disparó):

1. Mueve sus archivos a una carpeta aparte, para conservar la evidencia:

```powershell
New-Item -ItemType Directory -Force "mediciones\descartadas\${run}-intento1" | Out-Null
Move-Item "mediciones\$run-*.txt" "mediciones\descartadas\${run}-intento1\"
```

2. Repite la corrida con **el mismo identificador**. Así el baseline sigue siendo A1–A3 y no hay huecos de numeración.

Una corrida es un paquete: la memoria y los frames deben salir **de la misma ejecución**.

### Paso 1 — Estado limpio

1. Cierra la app y borra los datos **a mano**: *Ajustes → Apps → MovieDB → Info de la app → Borrar datos y caché* (todos, no solo caché).
   - `adb shell pm clear` falla en este realme (`SecurityException: CLEAR_APP_USER_DATA`).
   - *Clear app storage before deployment* del IDE solo actúa al **reinstalar desde el IDE**, así que no se usa en las corridas de medición.
   - **Usa siempre este mismo método** y anótalo.
2. Confirma que no queda proceso vivo:

```powershell
adb shell am force-stop $pkg
adb shell pidof $pkg
```

El segundo comando **no debe devolver nada**.

- Si devuelve un PID, algo reabrió la app: vuelve a hacer `force-stop`, **borra los datos otra vez** (un proceso vivo pudo escribir datos nuevos) y comprueba de nuevo.
- Deja el teléfono en la pantalla de inicio.

> **No pulses Run ni Profile en el IDE** entre este paso y el `am start -W` del paso 4: el IDE abre la app, llena Room y la caché de Glide, y deshace la limpieza.

**Por qué:** el escenario parte de caché de Glide y base de Room vacías. Si no, mides un arranque tibio.

### Paso 2 — Despertar el backend (Render)

`PopularMoviesFragment` usa GraphQL contra un proxy en Render (plan gratuito) que se duerme tras un rato sin tráfico. Un servidor dormido puede tardar ~20 s en responder, y la app lo muestra como *"Sin conexión. Verifica tu internet."* (`ErrorMapper` mapea cualquier `ApolloNetworkException` a ese mensaje).

```powershell
curl.exe -s -o NUL -w "backend: HTTP %{http_code} en %{time_total}s`n" -X POST "https://moviedb-graphql-proxy.onrender.com/graphql" -H "Content-Type: application/json" --data "@body.json"
```

- Si tarda más de ~1 s, **repítelo hasta obtener `HTTP 200` en menos de 1 s**. La primera respuesta lenta despierta el servidor y la segunda confirma que está caliente. Anota ambos tiempos.
- Referencia: A1 = 0.31 s (caliente), A2 = 22.58 s (dormido), A3 = 0.44 s (caliente).
- Haz este paso **justo antes** del paso 4, **sin pausas largas** entre medias, para que Render no vuelva a dormirse.

> Esta query solo despierta el proxy; no pasa por TMDB. La primera consulta real de películas puede seguir siendo más lenta.

### Paso 3 — Foto del estado del teléfono

```powershell
& {
  adb shell dumpsys battery | Select-String "level|temperature|powered"
  adb shell dumpsys thermalservice | Select-String "Thermal Status"
  adb shell cat /proc/meminfo | Select-String "MemAvailable"
  adb shell dumpsys display | Select-String "mActiveSfDisplayMode"
} | Tee-Object "mediciones\$run-0-estado.txt"
```

**Criterios para continuar:**

| Variable | Criterio |
|---|---|
| `temperature` (batería) | ≤ `350` (35.0 °C) |
| `Thermal Status` | `0` |
| `level` | > 50 % |
| `refreshRate` | `60.0` |

Si alguno no se cumple, espera y repite el paso 3. `temperature` viene en décimas de °C (`294` = 29.4 °C).

### Paso 4 — Cold start

```powershell
adb shell am start -W -n $pkg/.MainActivity | Tee-Object "mediciones\$run-1-arranque.txt"
```

Anota `TotalTime` (ms hasta el primer frame) y verifica `LaunchState: COLD`. Este comando es **el único que abre la app**.

> `TotalTime` mide hasta el primer frame de la Activity, no hasta que la lista es visible. Para "lista visible" hace falta `reportFullyDrawn()` (Fase 4).

**No cierres ni reabras la app.** Ese arranque es tu *cold start* con estado limpio.

### Paso 5 — Comprobar que la lista cargó

Mira el teléfono. `PopularMoviesFragment` debe mostrar **películas con posters**.

- Si aparece *"Sin conexión. Verifica tu internet."* o la lista está vacía: **descarta la corrida** (ver "Corrida inválida"), repite el paso 2 y vuelve al paso 1.
- No pases al siguiente paso hasta ver la lista cargada.

### Paso 6 — Lectura de memoria 1 (inicio)

Sin tocar la pantalla. Guarda la salida **completa** y la filtrada:

```powershell
Start-Sleep 5
adb shell dumpsys meminfo $pkg | Tee-Object "mediciones\$run-2-mem-inicio-completo.txt" | Select-String "Java Heap:|Native Heap:|Graphics:|TOTAL PSS:|Views:" | Tee-Object "mediciones\$run-2-mem-inicio.txt"
```

**Por qué la completa:** la tabla completa trae `Heap Alloc` (objetos vivos, que distingue *churn* de retención) y filas como `Code`, `System` o `.so mmap`, que explican por qué el PSS absoluto cambia entre corridas.

### Paso 7 — Reiniciar estadísticas de frames

```powershell
adb shell dumpsys gfxinfo $pkg reset
```

Va justo antes del scroll para que el porcentaje de *janky frames* no incluya la carga inicial.

### Paso 8 — Recorrido automatizado

Idéntico en todas las corridas (3 ciclos de 4 swipes hacia abajo y 4 hacia arriba, abrir el detalle y volver):

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

> Las coordenadas son para 1080×2400 y **no se cambian** entre corridas: son las que se usaron en A1–A3. Un dedo humano varía más que cualquier cambio de código. Si algún día ajustas el recorrido, **las corridas anteriores dejan de ser comparables** y hay que repetirlas.

> El recorrido mezcla scroll y abrir/cerrar el detalle, así que el p99 puede reflejar cualquiera de los dos. El trace de la Fase 4 los separa.

> **Mira el teléfono al terminar el recorrido:** debe mostrar la lista de películas. Si ves el launcher, el `tap` no abrió el detalle y el `BACK` sacó la app de primer plano (la lista es el destino inicial y, en Android 12, el `BACK` en la raíz manda la app a segundo plano en vez de cerrarla), así que la corrida no es válida. Señales en los archivos: `Views` final muy por debajo de lo habitual (156 en una corrida donde lo normal es 278) y menos frames de lo normal (1362 frente a 1435–1461).
>
> Comprobación opcional por comando, justo después del recorrido y antes del paso 9 (la app está inactiva, así que no altera los frames):
>
> ```powershell
> adb shell dumpsys activity activities | Select-String "topResumedActivity|mResumedActivity"
> ```
>
> La línea debe mencionar `com.alexvicente.moviedb/.MainActivity`. Si menciona el launcher u otra app, la corrida no es válida (el formato exacto puede variar según la versión de Android).

### Paso 9 — Lecturas finales (tres comandos, todos obligatorios)

```powershell
Start-Sleep 5
adb shell dumpsys meminfo $pkg | Tee-Object "mediciones\$run-3-mem-final-completo.txt" | Select-String "Java Heap:|Native Heap:|Graphics:|TOTAL PSS:|Views:" | Tee-Object "mediciones\$run-3-mem-final.txt"
adb shell dumpsys gfxinfo $pkg | Select-String "Total frames|Janky frames|50th|90th|95th|99th" | Tee-Object "mediciones\$run-4-frames.txt"
```

Son **dos bloques**: uno de memoria (que incluye el `-completo`) y otro de `gfxinfo`. Si sustituyes uno, **no pierdas la línea de `gfxinfo`**: sin ella la corrida queda sin jank y hay que repetirla.

### Paso 10 — Verificar que están todos los archivos

```powershell
Get-ChildItem "mediciones\$run-*.txt" | Select-Object Name
```

Deben aparecer **7 archivos**:

| Archivo | Contenido |
|---|---|
| `$run-0-estado.txt` | Estado del teléfono |
| `$run-1-arranque.txt` | Cold start |
| `$run-2-mem-inicio.txt` / `-completo.txt` | Memoria inicial (filtrada y completa) |
| `$run-3-mem-final.txt` / `-completo.txt` | Memoria final (filtrada y completa) |
| `$run-4-frames.txt` | Jank y percentiles |

Si falta alguno **antes de tocar el teléfono**, repite el comando que lo genera. `gfxinfo` es acumulativo desde el `reset`, así que se puede recuperar **solo si** `pidof` devuelve el mismo PID que en los archivos de memoria **y** `Total frames rendered` es parecido al de las otras corridas (~1450). Si no, repite la corrida.

### Paso 11 — Anotar, entregar y enfriar

Pasa los datos a `02-plantilla-mediciones.md`. Para compartir una corrida con todos sus archivos en un solo bloque de texto:

```powershell
Get-ChildItem "mediciones\$run-*.txt" | Sort-Object Name | ForEach-Object {
  "=== $($_.Name) ==="
  Get-Content $_.FullName
  ""
} | Set-Clipboard
```

Añade a mano lo que no queda en archivos: el **tiempo del backend** (paso 2), si la **lista cargó con posters** (paso 5) y el **método de limpieza**.

Deja enfriar el teléfono **2–3 min** y vuelve al paso 1 con el siguiente `$run`.

---

## 2. Qué anotar por corrida

| Dato | Origen |
|---|---|
| Dispositivo, variante (`benchmark`), refresco | Fijos de la sesión |
| Tiempo del backend (warm-up) | Paso 2 |
| Batería (nivel, temperatura), `Thermal Status`, `MemAvailable` | Paso 3 |
| `TotalTime` (cold start) | Paso 4 |
| Java Heap, Native Heap, Graphics, TOTAL PSS, Views (inicio y final) | Pasos 6 y 9 |
| Total frames, Janky frames (%), percentiles 50/90/95/99 | Paso 9 |
| Método de limpieza de estado | Paso 1 |
| Red WiFi del teléfono (¿la misma de siempre?) y nivel de batería | Condiciones de la sesión |

Plantilla rápida:

| Corrida | Backend (s) | Temp. (°C) | MemAvailable (kB) | TotalTime (ms) | Δ Java+Native (kB) | Janky (%) | Janky legacy (%) | p90 / p95 / p99 (ms) |
|---|---|---|---|---|---|---|---|---|
| A1 | | | | | | | | |
| A2 | | | | | | | | |
| A3 | | | | | | | | |
| **Mediana** | | | | | | | | |

Reporta la **mediana** de A1–A3 (el valor del medio), no la mejor corrida.

**Descarta o repite** una corrida si:

- La lista mostró error o no cargó (paso 5).
- Falta algún archivo (paso 10) y no se puede recuperar.
- La temperatura, `Thermal Status` o `MemAvailable` se desvían mucho de las otras.
- La pantalla se apagó o el recorrido se interrumpió (la app se cerró, o el `tap` no abrió el detalle).
- El refresco no era 60 Hz.

**Corrida atípica:** si una corrida se desvía mucho de las demás en varias métricas a la vez pero su estado inicial cumple los criterios, **no se descarta por el resultado**. Solo se descarta si se identifica y documenta una causa (por ejemplo, cambió el estado de compilación de ART o hubo actividad en segundo plano). Si no hay causa, se conserva, se añaden corridas (hasta 5) y se reporta la mediana con el rango completo. Antes de añadirlas, comprueba `Dexopt state` y `lastUpdateTime`.

---

## 3. Lectura rápida de las métricas

- **Reporta el cambio (final − inicio) de Java Heap y Native Heap, no el TOTAL PSS.** El PSS absoluto varió ~30 MB entre corridas del mismo código, y su cambio también (+24 a +38 MB) porque incluye Graphics. Con A1–A3, Δ Java+Native varió solo ~1.9 MB (32.7 a 34.6 MB). **Excepción:** si el cambio de código modifica el estado inicial (por ejemplo, cuántos ítems se inflan al cargar), el delta deja de comparar lo mismo; compara entonces los valores absolutos de inicio y de final.
- **Native Heap:** en Android 8+ los píxeles de un `Bitmap` viven aquí, pero también hay SQLite, red y cachés de Skia: no se atribuye a Glide sin comprobarlo (Fase 2 o heap dump).
- **Java Heap:** compara `Heap Alloc` (objetos vivos) con el PSS (páginas sucias). Si el PSS crece pero lo vivo casi no, es *churn*, no retención. Se confirma con la Fase 3.
- **Graphics:** memoria de GPU (texturas, buffers de ventana, layers). Con el mismo código varió en pasos de ~10 MB entre corridas (un buffer RGBA de 1080×2400 pesa ≈ 10.1 MB; hipótesis). Pero tras cambiar el layout de Popular el valor inicial bajó de ~88–98 MB a ~28 MB, así que **parte de Graphics sí depende de lo que la pantalla dibuja**. Se reporta aparte, con valores absolutos y su dispersión entre corridas.
- **Views:** si es igual al número de ítems de la lista, el `RecyclerView` no está reciclando. Aislado no prueba nada: hay que contar `MovieViewHolder`.
- **Janky frames (actual):** frames que fallaron su deadline según el sistema. Es la más estable; es la que se reporta.
- **Janky frames (legacy):** frames que superaron ~16.6 ms. Es más ruidosa cuando la mediana está cerca del límite. Se reporta aparte y etiquetada.
- **Percentiles:** `gfxinfo` los da en milisegundos enteros, así que cambios pequeños no se ven. El p99 es lo que el usuario nota.
- **GPU vs total:** si los percentiles de GPU están muy por debajo del tiempo total, el cuello de botella está en la CPU (hilo principal / `RenderThread`).
- **Red vs. render:** el backend lento afecta el *tiempo hasta tener datos*, no el jank. Mide la red aparte (secciones `trace()` o `curl`) y compara pantallas **con la lista ya cargada**.
- **Resolución de la medición:** una mejora solo es demostrable si mueve la métrica más que lo que varían las corridas entre sí (criterio propio).

---

## 4. Escenario B (Genres con Paging 3)

Mismo protocolo con tres cambios:

1. Tras el paso 4, navegar a un género (siempre el mismo recorrido).
2. El paso 8 es un scroll más largo (hasta ~5 páginas), con pausas para que cargue cada página.
3. Esta pantalla usa **Retrofit directo contra TMDB** (no el proxy de Render), así que no necesita el paso 2.

*Pendiente: coordenadas y tiempos, a definir con el código del feature genres.*

---

## 5. Errores conocidos y soluciones

| Síntoma | Causa | Solución |
|---|---|---|
| `.\adb` no se reconoce | Con `adb` en el `PATH` el `.\` busca en la carpeta actual | Usar `adb` sin `.\` en una terminal nueva |
| `SecurityException ... WRITE_SECURE_SETTINGS` al hacer `settings put` | realme UI bloquea escrituras de ajustes globales desde `adb` | Cambiar las escalas de animación a mano |
| `SecurityException ... CLEAR_APP_USER_DATA` en `pm clear` | Misma restricción de realme UI | Borrar datos y caché desde Info de la app |
| `pidof` devuelve un PID tras borrar datos | La app se reabrió (IDE, Profiler o el propio sistema) | `force-stop`, borrar datos otra vez y comprobar `pidof` vacío |
| `kspBenchmarkKotlin` falla con `NonExistentClass` | `initWith(release)` no copia el source set | Agregar `src/release` a `sourceSets` de `benchmark` |
| `flags=` incluye `DEBUGGABLE` | Está instalado `debug` | Reinstalar con `benchmark` + *Profile as profileable* |
| "Sin conexión. Verifica tu internet." con WiFi buena | Proxy de Render dormido (timeout mapeado a error de red) | Paso 2 antes de cada corrida |
| Falta `$run-4-frames.txt` | Se omitió o se perdió el comando de `gfxinfo` del paso 9 | Recuperar solo si `pidof` y `Total frames` coinciden; si no, repetir la corrida |
| `Views` final muy bajo (156) y menos frames, o la app queda fuera de primer plano al terminar el recorrido | La app salió de primer plano durante la corrida (por un `BACK` en la raíz, que en Android 12 la manda a segundo plano, o por una interrupción del sistema) | Revisar los eventos `wm_on_paused_called` y `wm_on_stop_called` en `adb logcat -b events -d`, descartar la corrida y repetirla mirando el teléfono |
| Los archivos de una corrida anterior cambiaron | `$run` no se actualizó y `Tee-Object` sobrescribió | Comprobar `$run` antes del paso 1 y respaldar con `Copy-Item` |
| `$pid` da error al asignarlo | Variable reservada de PowerShell | Usar `$appPid` |
| Navegador "cargando" en `api.themoviedb.org/3` | Responde `204 No Content` | Probar `https://api.themoviedb.org/3/configuration` |

---

## 6. Reglas de la sesión

- Primero medir, después optimizar. Un cambio de código por commit, con su medición antes/después.
- **No cambiar código durante las corridas** (cambia el build que mides).
- **Medir con `adb`, no con el Profiler del IDE.** El Profiler añade overhead y se usa para entender el porqué (traces, heap dumps), no para tomar tiempos. Ninguna tarea del Profiler debe estar grabando durante una corrida.
- No mezclar números de `debug` y `benchmark`, ni de teléfono y emulador. Cada tabla indica su dispositivo.
- Glide (`DataSource`) y heap dumps/leaks: en `debug`. Tiempos, jank y memoria: en `benchmark`.
- Las escalas de animación se dejaron en `1x` para medir. Si sigues usando el teléfono para Espresso, vuelve a ponerlas en `0` al terminar.
- Conserva la carpeta `mediciones\` (incluida `descartadas\`) como evidencia del README.

### Orden recomendado de las fases siguientes

1. **Fase 4 (System Trace / Perfetto) en `benchmark`:** es la misma instalación y separa scroll de detalle y red de render.
2. **Cambiar a `debug`** para las Fases 2 (Glide) y 3 (heap dump, conteo de `MovieViewHolder`).
3. **Volver a `benchmark`** antes de cualquier número para el README y comprobar `flags=` sin `DEBUGGABLE`.

### Pendientes fuera de la sesión

- AGP 8.6.0 avisa que solo está probado hasta `compileSdk = 35` (el proyecto usa 36). No bloquea; actualizar **después** de medir.
- Distinguir timeout de falta de red en `ErrorMapper` (hoy ambos muestran "Sin conexión").
- Revisar los timeouts del cliente de Apollo en `NetworkModule.kt` (una respuesta de ~22 s del proxy dormido puede superar los 10 s por defecto de OkHttp, si no se han cambiado).
