# Guía de captura de System Trace — MovieDB (Fase 4)

Esta guía es **independiente** de `guia-ejecucion-profiling.md`. Aquí no se toman números para la tabla del README: el trace sirve para **entender por qué** hay jank, no para medirlo.

> Los nombres de botones y menús de Android Studio pueden variar según la versión. Si algo no coincide, busca el equivalente y ajusta la guía.

---

## 0. Diferencias con las corridas de medición

| | Corrida de medición (A1–A3) | Captura de trace (T1, T2…) |
|---|---|---|
| Objetivo | Números reproducibles para el README | Entender qué hilo y qué código gastan el tiempo |
| Herramienta | Solo `adb` | Profiler de Android Studio |
| Overhead | El mínimo posible | El Profiler añade carga: **sus tiempos no cuentan** |
| Qué se guarda | `mediciones\A*-*.txt` | El trace y las notas de la sección 5 |
| Va a la tabla del README | Sí | **No** |

**Regla:** nunca mezcles las dos cosas. No captures un trace mientras tomas una corrida de medición, y no anotes tiempos del trace en la tabla del baseline.

Convención de nombres: **T1** (recorrido de Popular), **T2** (siguiente captura), etc.

---

## 1. Requisitos previos

- Build `benchmark` instalado (ya hecho). Comprueba:

```powershell
adb shell dumpsys package $pkg | Select-String "flags="
```

No debe aparecer `DEBUGGABLE`.

- Terminal abierta en la raíz del proyecto, con las variables definidas:

```powershell
$pkg = "com.alexvicente.moviedb"
```

- Teléfono en el mismo estado de siempre: 60 Hz, animaciones en `1x`, USB conectado, pantalla encendida.
- Ninguna sesión del Profiler abierta de antes.

---

## 2. Preparar el estado (igual que antes de una corrida)

Es el mismo inicio que en la guía de ejecución (pasos 1 a 5), para que el trace cubra el mismo escenario que mediste:

1. Cierra la app y borra datos y caché a mano (*Ajustes → Apps → MovieDB → Info de la app*).
2. Comprueba que no queda proceso:

```powershell
adb shell am force-stop $pkg
adb shell pidof $pkg
```

El segundo comando **no debe devolver nada**.

3. Calienta el backend hasta que responda en menos de 1 s:

```powershell
curl.exe -s -o NUL -w "backend: HTTP %{http_code} en %{time_total}s`n" -X POST "https://moviedb-graphql-proxy.onrender.com/graphql" -H "Content-Type: application/json" --data "@body.json"
```

4. Abre la app **solo con este comando** (no con Run ni Profile del IDE):

```powershell
adb shell am start -W -n $pkg/.MainActivity
```

5. Espera a ver `PopularMoviesFragment` **con películas y posters**. Si sale "Sin conexión", repite desde el paso 3.

---

## 3. Capturar el trace en el Profiler

La app ya está corriendo. Ahora:

1. Abre el panel **Profiler** (pestaña *Home*). Si lo ves cortado, agranda el panel arrastrando su borde superior o haz doble clic en la pestaña **Profiler**.
2. En la lista de procesos, haz clic en `com.alexvicente.moviedb`. Debe mostrar **PID** y **Profileable**.

| Lo que ves | Significado |
|---|---|
| PID + **Profileable** | Correcto ✅ |
| PID + **Debuggable** | Está instalada `debug`: reinstala `benchmark` |
| `Not running` | La app está cerrada: repite el paso 4 de la sección 2 |

3. Haz clic en la tarjeta **Capture System Activities (System Trace)**. Debe quedar resaltada en azul.
4. Abajo, en **Start profiler task from**, abre el desplegable y elige la opción que empieza **sobre el proceso ya abierto** (suele llamarse *Now* o algo equivalente). *Process start* solo sirve si el IDE lanza la app.
5. Pulsa **Start profiler task** (abajo a la derecha). El mensaje rojo *No task starting point selected* debe desaparecer.
6. **Inmediatamente** ejecuta el recorrido en la terminal (el mismo del paso 8 de la guía de ejecución):

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

7. Pulsa **Stop** en el Profiler y espera a que procese el trace.

**Por qué esta secuencia:** el trace debe cubrir el mismo recorrido que ya mediste, con la lista ya cargada, sin mezclar la espera de la red.

### Si el desplegable no ofrece "Now"

Anota qué opciones aparecen. Como alternativa: **Run → Profile 'app' as profileable** con la variante `benchmark`, y elige la tarea desde el inicio. Para el trace sirve (no necesitas estado limpio ni números), pero la app se abrirá sola desde el IDE.

---

## 4. Guardar el trace

- Android Studio guarda la grabación en la pestaña **Past Recordings** del Profiler.
- Para conservarla como evidencia, expórtala desde ahí (busca la opción de exportar; en tu versión genera un archivo `.trace`) y guárdala en una carpeta aparte, **no** en `mediciones\`:

```powershell
New-Item -ItemType Directory -Force trazas | Out-Null
```

- Nómbrala por captura **conservando la extensión que genera el IDE**, por ejemplo `trazas\T1-popular-scroll.trace`. No la renombres a otra extensión: no hace falta.
- Alternativa: abre el archivo exportado en `https://ui.perfetto.dev` con *Open trace file* (en el navegador; el archivo se carga en tu equipo). Perfetto reconoce el formato por el contenido y no por la extensión. Si no lo abre, ábrelo en Android Studio (pestaña *Past Recordings*). En Perfetto UI, **W/S** hacen zoom y **A/D** desplazan la línea de tiempo.

---

## 5. Qué mirar en el trace

Haz zoom hasta ver el recorrido completo y busca, en este orden:

1. **Frames janky.** En la pista de frames (suelen marcarse en rojo o amarillo), identifica los más largos. Anota cuántos hay y cuándo ocurren.
2. **Qué fase del recorrido los causa.** Para cada frame largo, indica si coincide con el **scroll**, con **abrir el detalle** o con el **`BACK`**. Esto separa la causa del p99 de tus mediciones.
3. **Hilo principal.** Dentro del frame largo, mira qué `slice` ocupa más tiempo. Nombres que suelen aparecer: `Choreographer#doFrame`, `RV CreateView`, `RV OnBindView`, `inflate`, `layout`, `measure`. Que algunas secciones de `RecyclerView` aparezcan depende de cómo se configure la captura.
4. **`RenderThread`.** Si también es largo, el trabajo de dibujo es un problema. En tu baseline la GPU gasta 6–8 ms por frame, así que se espera que no sea el cuello de botella.
5. **Otros hilos.** Busca hilos de Glide (decodificación de imágenes) y de OkHttp/Room trabajando durante frames largos, o competencia por la CPU.

### Plantilla de notas por captura

| Frame (instante) | Duración | Fase (scroll / detalle / BACK) | Slice más largo en el hilo principal | ¿RenderThread largo? | Otro hilo activo |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

Anota **solo lo que veas**. Si no puedes identificar una causa, escribe "no identificado" y descríbelo: eso también es un dato.

---

## 6. Qué hacer con el resultado

- Describe o captura lo que ves en los frames largos (qué slices ocupan más tiempo y en qué hilo) y se interpreta junto con el baseline.
- Lo que encuentres son **hipótesis** que luego se verifican con un cambio de código y una nueva medición (guía de ejecución), no conclusiones.
- Si el trace apunta a falta de visibilidad dentro de tu código (por ejemplo, dónde se va el tiempo entre Apollo, Room y el adapter), el siguiente paso es la Fase 4.1: agregar secciones propias con `androidx.tracing` en **un commit aparte**. Eso cambia el código, así que se hace después de capturar este trace, no antes.

---

## 7. Después de capturar

1. Detén cualquier tarea del Profiler que siga activa y cierra su sesión.
2. Para volver a tomar mediciones con `adb`, **no dejes el Profiler grabando**.
3. Si cambiaste a otra variante (`debug` para Glide o heap dumps), vuelve a `benchmark` y comprueba `flags=` sin `DEBUGGABLE` antes de anotar números.

---

## 8. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| No veo *Start profiler task* | El panel está cortado | Agrandar el panel o maximizar la pestaña Profiler |
| El botón está deshabilitado y dice *No task starting point selected* | Falta elegir el punto de inicio | Elegir la opción de iniciar sobre el proceso abierto |
| El proceso aparece como `Debuggable` | Está instalada `debug` | Reinstalar `benchmark` con *Profile as profileable* |
| El proceso aparece como `Not running` | La app está cerrada | Abrirla con `am start -W` |
| La lista muestra "Sin conexión" | Render dormido | Repetir el calentamiento del backend (sección 2) |
| El trace sale casi vacío | Se detuvo antes de ejecutar el recorrido | Repetir la captura ejecutando el recorrido justo después de iniciar |
