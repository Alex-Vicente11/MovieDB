# Debugging Report: Tests Instrumentados con Paging 3 en Android

**Proyecto:** `com.example.apptest`  
**Archivo bajo prueba:** `MoviesByGenrePagingUiTest.kt`  
**Fecha de resolución:** Junio 2026  
**Severidad:** Alta — bugs de producción descubiertos a través de tests instrumentados

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Arquitectura y flujo de datos](#2-arquitectura-y-flujo-de-datos)
3. [Descripción de los bugs](#3-descripción-de-los-bugs)
4. [Proceso de diagnóstico](#4-proceso-de-diagnóstico)
5. [Pruebas realizadas y sus resultados](#5-pruebas-realizadas-y-sus-resultados)
6. [Soluciones aplicadas](#6-soluciones-aplicadas)
7. [Por qué se usaron utils como método de solución](#7-por-qué-se-usaron-utils-como-método-de-solución)
8. [Proceso de verificación ideal](#8-proceso-de-verificación-ideal)
9. [Recomendaciones y observaciones](#9-recomendaciones-y-observaciones)

---

## 1. Resumen ejecutivo

Durante el desarrollo de `MoviesByGenrePagingUiTest`, el test `whenMoviesLoad_recyclerViewShowsItems()` falló consistentemente con el error:

```
view.getVisibility() was <GONE>
RecyclerView{visibility=GONE, width=0, height=0, child-count=0}
```

Lo que inicialmente parecía un problema de timing en los tests resultó ser **dos bugs reales de producción**:

| Bug | Archivo afectado | Impacto |
|-----|-----------------|---------|
| `ActivityScenario.use{}` cerraba la Activity prematuramente | `launchFragmentInHiltContainer.kt` | Todos los tests instrumentados con Paging 3 o corrutinas asíncronas |
| Race condition en la lógica de visibilidad del RecyclerView | `GenresFragment.kt` | Usuarios reales en dispositivos lentos con Paging 3 |

**Conclusión clave:** el test no era el problema — fue el detector del problema.

---

## 2. Arquitectura y flujo de datos

### 2.1 Flujo en producción

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE DATOS                            │
│                                                             │
│  MoviesByGenreApi (Retrofit)                                │
│  GET discover/movie → MovieResponseDto                      │
│           │                                                 │
│           ▼                                                 │
│  MoviesByGenrePagingSource.load()                           │
│  DTO → Movie (dominio) + calcula prevKey/nextKey            │
│           │                                                 │
│           ▼                                                 │
│  MoviesByGenreRepositoryImpl                                │
│  Pager + PagingConfig → Flow<PagingData<Movie>>             │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    CAPA DE DOMINIO                          │
│                                                             │
│  GetMoviesByGenreUseCase                                    │
│  Expone el Flow al ViewModel sin transformarlo              │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 CAPA DE PRESENTACIÓN                        │
│                                                             │
│  GenresViewModel.movies                                     │
│  flatMapLatest(selectedGenreId) + cachedIn(viewModelScope)  │
│           │                                                 │
│           ▼                                                 │
│  GenresFragment.observeMovies()                             │
│  moviesAdapter.submitData(pagingData)                       │
│  loadStateFlow / onPagesUpdatedFlow                         │
│           │                                                 │
│           ▼                                                 │
│  recyclerViewMovies + MoviesLoadStateAdapter (footer)       │
│  ← Espresso hace sus aserciones aquí                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Flujo en el test instrumentado

```
┌─────────────────────────────────────────────────────────────┐
│  @BindValue moviesByGenreRepository (mock)                  │
│                                                             │
│  every { getMoviesByGenre(any(), any()) } returns           │
│       flowOf(PagingData.from(MovieFactory.createMovieList(5)│
│                                                             │
│  ⚠️  MoviesByGenreApi y PagingSource NUNCA se ejecutan      │
│  ✅  El mock reemplaza todo desde el Repository hacia abajo  │
└─────────────────────────┬───────────────────────────────────┘
                          │ Flow<PagingData<Movie>> estático
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  GetMoviesByGenreUseCase → pasa el Flow sin transformar     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  GenresViewModel.movies                                     │
│  flatMapLatest + cachedIn                                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  GenresFragment.observeMovies()                             │
│  moviesAdapter.submitData(pagingData) ← async               │
│  addLoadStateListener / onPagesUpdatedFlow                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  recyclerViewMovies                                         │
│  ← onView(withId(R.id.recyclerViewMovies))                  │
│     .check(matches(isDisplayed()))                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Descripción de los bugs

### Bug #1 — `ActivityScenario.use{}` cerraba la Activity prematuramente

**Archivo:** `launchFragmentInHiltContainer.kt`

**Código problemático:**
```kotlin
// ❌ ANTES
ActivityScenario.launch<HiltTestActivity>(startActivityIntent).use { scenario ->
    scenario.onActivity { activity ->
        // setup del Fragment...
        (fragment as T).action()
        // .use{} cierra aquí → Activity destruida → proceso termina
    }
} // ← Activity ya destruida, Paging 3 no puede procesar nada
```

**Por qué es un bug:** `.use{}` en Kotlin es una implementación de `Closeable` — al salir del bloque llama automáticamente a `scenario.close()`, que destruye la `HiltTestActivity`. Paging 3 necesita que la Activity (y por ende el `viewModelScope` y `lifecycleScope`) permanezcan vivos para procesar `PagingData` de forma asíncrona. El proceso moría en ~40ms, antes de que ningún `loadStateFlow` o `addLoadStateListener` pudiera recibir eventos.

**Evidencia en logs:**
```
PROCESS STARTED (1785)
submitData() llamado
submitData() completado
PROCESS ENDED (1785)   ← muere inmediatamente
```

---

### Bug #2 — Race condition en la lógica de visibilidad del RecyclerView

**Archivo:** `GenresFragment.kt` → `observeMovies()`

**Código problemático:**
```kotlin
// ❌ ANTES — race condition con itemCount
moviesAdapter.loadStateFlow.collectLatest { loadState ->
    val refresh = loadState.refresh
    binding.recyclerViewMovies.isVisible =
        refresh is LoadState.NotLoading && moviesAdapter.itemCount > 0
        //                                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //                                 itemCount puede ser 0 aunque
        //                                 los datos estén en camino
}
```

**Por qué es un bug:** `submitData()` es asíncrono. El `PagingDataDiffer` interno de Paging 3 procesa los datos en una corrutina separada. Cuando `loadStateFlow` emite `NotLoading`, `itemCount` puede aún ser `0` porque el differ no ha terminado de notificar al adapter. Esto crea un círculo vicioso:

```
recyclerViewMovies = GONE (estado inicial en XML)
    │
    ▼
loadStateFlow emite NotLoading, pero itemCount=0
    │
    ▼
recyclerViewMovies.isVisible = false → sigue GONE
    │
    ▼
Con GONE: width=0, height=0, no hace layout
    │
    ▼
child-count permanece 0 para siempre
    │
    ▼
itemCount del adapter se actualiza a 5 DESPUÉS
pero loadStateFlow ya NO vuelve a emitir
    │
    ▼
recyclerViewMovies nunca se hace VISIBLE ❌
```

**Impacto en producción:** Este bug también afectaría a usuarios reales en dispositivos lentos o bajo carga de CPU, donde la diferencia de timing entre `loadStateFlow` y la actualización de `itemCount` sería más pronunciada.

---

## 4. Proceso de diagnóstico

El diagnóstico siguió un proceso sistemático de descarte, de lo más simple a lo más complejo:

```
Hipótesis 1: Problema de timing (Thread.sleep insuficiente)
    ↓ DESCARTADO — waitForView(10000ms) también falló

Hipótesis 2: Datos del mock incorrectos (MovieFactory)
    ↓ DESCARTADO — MovieFactory.createMovieList(5) construye
                   objetos válidos con todos los campos requeridos

Hipótesis 3: Firma del mock no coincide con la invocación real
    ↓ DESCARTADO — GetMoviesByGenreUseCase llama con (genreId, language)
                   y el mock usa any(), any() — compatible

Hipótesis 4: DiffUtil con bug (areItemsTheSame/areContentsTheSame)
    ↓ DESCARTADO — Movie es data class, DiffUtil implementado correctamente

Hipótesis 5: loadStateFlow no emite (verificado con logs)
    ↓ CONFIRMADO — addLoadStateListener nunca disparó sus logs

Hipótesis 6: Proceso muere antes de que Paging 3 procese
    ↓ CONFIRMADO — PROCESS ENDED inmediatamente tras submitData()

Causa raíz A: ActivityScenario.use{} destruye la Activity
    ↓ RESUELTO — eliminar .use{}

Causa raíz B: Race condition loadStateFlow + itemCount
    ↓ RESUELTO — usar onPagesUpdatedFlow para visibilidad
```

### Herramienta de diagnóstico que resultó clave

El log de threads reveló la causa raíz más rápido que cualquier otra técnica:

```
00:04:24.635  thread-9541  Antes de launchFragment()
00:04:25.260  thread-9518  submitData() llamado
00:04:25.299  thread-9518  submitData() completado
00:04:25.568  thread-9541  Después de launchFragment()
00:04:28.569  thread-9541  Después de sleep(3000)
00:04:28.682  thread-9518  visibility=8, itemCount=5
PROCESS ENDED
```

Observaciones críticas de este log:
- Dos threads distintos: `9541` (test) y `9518` (UI/main)
- `submitData()` completa en **39ms**
- `visibility=8` (GONE) con `itemCount=5` tras 3 segundos de espera
- `addLoadStateListener` **nunca** aparece en el log

---

## 5. Pruebas realizadas y sus resultados

### Intento 1 — `Thread.sleep(300)` estándar

```kotlin
launchFragment() // contiene Thread.sleep(300)
onView(withId(R.id.recyclerViewMovies)).check(matches(isDisplayed()))
```

**Error:**
```
Got: view.getVisibility() was <GONE>
RecyclerView{visibility=GONE, width=0, height=0, child-count=0}
```

**Conclusión:** Se asumió inicialmente que era timing insuficiente.

---

### Intento 2 — `waitForView` con polling cada 50ms

```kotlin
fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 5000) {
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
        try {
            onView(matcher).check(matches(isDisplayed()))
            return
        } catch (e: Throwable) {
            Thread.sleep(50)
        }
    }
    onView(matcher).check(matches(isDisplayed()))
}
```

**Error:** Mismo error. `waitForView` agotó los 5000ms sin éxito.

**Conclusión:** Descartado timing como causa principal. El RecyclerView genuinamente nunca llegaba a `VISIBLE`.

---

### Intento 3 — `waitForView` con timeout 10000ms

**Error:** Mismo error.

**Conclusión:** Confirmado que no es cuestión de tiempo de espera.

---

### Intento 4 — `Thread.sleep(1000)` con bloque de diagnóstico

```kotlin
Thread.sleep(1000)
onView(withId(R.id.recyclerViewMovies)).check { view, _ ->
    val rv = view as RecyclerView
    println("itemCount=${rv.adapter?.itemCount}")
}
```

**Resultado:** ¡El test "pasó"! Pero por razones incorrectas — el bloque `check { }` sin aserciones no lanza excepción aunque `visibility=GONE`.

**Log:**
```
recyclerViewMovies visibility=8
recyclerViewMovies itemCount=5
```

**Conclusión clave:** `itemCount=5` pero `visibility=8`. Los datos llegaron al adapter, pero la lógica de visibilidad nunca se disparó. Esto descartó el adapter y el DiffUtil como culpables.

---

### Intento 5 — `Thread.sleep(2000)` con aserción real

```kotlin
Thread.sleep(2000)
onView(withId(R.id.recyclerViewMovies)).check(matches(isDisplayed()))
```

**Error:** Mismo error. 2 segundos de espera y el RecyclerView nunca se hizo `VISIBLE`.

**Conclusión:** Confirmado que es un problema estructural, no de timing.

---

### Intento 6 — Logs de diagnóstico en `GenresFragment`

```kotlin
moviesAdapter.addLoadStateListener { loadState ->
    Log.d("PAGING_DEBUG", "=== loadStateListener ===")
    Log.d("PAGING_DEBUG", "refresh=${loadState.refresh}")
}

viewLifecycleOwner.lifecycleScope.launch {
    viewModel.movies.collectLatest { pagingData ->
        Log.d("PAGING_DEBUG", "submitData() llamado")
        moviesAdapter.submitData(pagingData)
        Log.d("PAGING_DEBUG", "submitData() completado")
    }
}
```

**Log obtenido:**
```
submitData() llamado
submitData() completado
PROCESS ENDED  ← el listener nunca emitió
```

**Conclusión:** `addLoadStateListener` nunca disparó. El proceso moría inmediatamente después de `submitData()`. Causa raíz #1 identificada: `ActivityScenario.use{}`.

---

### Intento 7 — Eliminar `.use{}` de `launchFragmentInHiltContainer`

```kotlin
// Antes
ActivityScenario.launch<HiltTestActivity>(startActivityIntent).use { scenario -> ... }

// Después
val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)
scenario.onActivity { ... }
```

**Log obtenido:**
```
Antes de launchFragment()
submitData() llamado
submitData() completado
Después de launchFragment()
Después de sleep(3000)
visibility=8, itemCount=5  ← proceso vivo, pero RecyclerView GONE
PROCESS ENDED
```

**Conclusión:** El proceso ya no muere prematuramente, pero `visibility=8` persiste. `addLoadStateListener` sigue sin emitir. Causa raíz #2 identificada: race condition en `observeMovies()`.

---

### Solución final — `onPagesUpdatedFlow`

```kotlin
// Visibilidad separada en onPagesUpdatedFlow
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        moviesAdapter.onPagesUpdatedFlow.collect {
            binding.recyclerViewMovies.isVisible = moviesAdapter.itemCount > 0
        }
    }
}
```

**Resultado:** ✅ Test pasa con y sin `waitForView`, con `Thread.sleep` mínimo.

---

## 6. Soluciones aplicadas

### Solución Bug #1 — Eliminar `.use{}` de `launchFragmentInHiltContainer`

```kotlin
// launchFragmentInHiltContainer.kt

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    navHostController: TestNavHostController? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    // ✅ Sin .use{} — Activity permanece RESUMED durante todo el test
    val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)

    scenario.onActivity { activity ->
        fragmentFactory?.let {
            activity.supportFragmentManager.fragmentFactory = it
        }

        val fragment = activity.supportFragmentManager.fragmentFactory
            .instantiate(checkNotNull(T::class.java.classLoader), T::class.java.name)

        fragment.arguments = fragmentArgs

        // ✅ Observer con lateinit var — evita memory leak, auto-se-remueve
        navHostController?.let { navController ->
            lateinit var observer: Observer<LifecycleOwner?>
            observer = Observer { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    Navigation.setViewNavController(fragment.requireView(), navController)
                    fragment.viewLifecycleOwnerLiveData.removeObserver(observer)
                }
            }
            fragment.viewLifecycleOwnerLiveData.observeForever(observer)
        }

        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        (fragment as T).action()
    }
    // scenario permanece abierto — Espresso lo gestiona automáticamente
}
```

### Solución Bug #2 — `onPagesUpdatedFlow` en `observeMovies()`

```kotlin
// GenresFragment.kt

private fun observeMovies() {
    // addLoadStateListener — sincrónico, nunca pierde eventos
    moviesAdapter.addLoadStateListener { loadState ->
        val refresh = loadState.refresh
        binding.progressBarMovies.isVisible = refresh is LoadState.Loading
        binding.layoutError.isVisible = refresh is LoadState.Error
        if (refresh is LoadState.Error) {
            binding.tvError.text = refresh.error.localizedMessage
                ?: getString(R.string.error_loading_movies)
        }
    }

    // onPagesUpdatedFlow — emite DESPUÉS de que itemCount está actualizado
    // Garantiza que la visibilidad se evalúa con el conteo correcto
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            moviesAdapter.onPagesUpdatedFlow.collect {
                binding.recyclerViewMovies.isVisible = moviesAdapter.itemCount > 0
            }
        }
    }

    // submitData — siempre después de registrar listeners
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.movies.collectLatest { pagingData ->
                moviesAdapter.submitData(pagingData)
            }
        }
    }
}
```

---

## 7. Por qué se usaron utils como método de solución

### `waitForView` — utility de espera activa

```kotlin
// androidTest/util/EspressoIdlingHelpers.kt
fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 10000) {
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
        try {
            onView(matcher).check(matches(isDisplayed()))
            return
        } catch (e: Throwable) {
            Thread.sleep(100)
        }
    }
    onView(matcher).check(matches(isDisplayed()))
}
```

**Por qué se creó:** `Thread.sleep(N)` es frágil porque usa un número arbitrario de milisegundos que puede ser insuficiente en CI o dispositivos lentos. `waitForView` reintenta la aserción hasta que se cumple o expira el timeout — se adapta a la velocidad real del dispositivo.

**Por qué vive en `util/`:** Es reutilizable en cualquier test instrumentado del proyecto que tenga operaciones asíncronas (no solo en `MoviesByGenrePagingUiTest`). Centralizar helpers en `util/` sigue el principio DRY y facilita el mantenimiento.

**Limitación descubierta en este debugging:** `waitForView` no resuelve problemas estructurales. Si el RecyclerView nunca llega a `VISIBLE` por un bug en la lógica de visibilidad, el helper agotará su timeout siempre. Un helper de espera solo es útil cuando la condición *sí* se cumplirá eventualmente — no cuando hay un bug que la impide.

### `launchFragmentInHiltContainer` — utility de infraestructura de tests

Este helper existe porque `launchFragmentInContainer` (la función estándar de AndroidX) no es compatible con Hilt — su Activity interna no tiene `@AndroidEntryPoint`. La utility resuelve ese gap de infraestructura de forma centralizada, evitando que cada test tenga que reimplementar el setup de Hilt + Fragment + NavController.

**Cuándo modificar una utility de infraestructura:** Solo cuando se descubre un bug fundamental (como el `.use{}`) que afecta a toda la suite de tests. Cambios en utilities de infraestructura tienen alto impacto — siempre verificar que los tests existentes siguen pasando después del cambio.

---

## 8. Proceso de verificación ideal

Antes de escribir un test instrumentado con Paging 3 y corrutinas, el orden de verificación recomendado para evitar los bugs documentados aquí:

```
PASO 1 — Verificar la infraestructura de la Activity
├── ¿launchFragmentInHiltContainer usa .use{}? → Eliminar
├── ¿La Activity permanece viva después del setup? → Verificar con logs de proceso
└── ¿El Fragment se adjunta correctamente al contenedor? → Verificar con commitNow()

PASO 2 — Verificar el flujo de datos hasta el adapter
├── ¿El mock devuelve datos? → Log en submitData()
├── ¿submitData() se invoca? → Log antes/después
└── ¿El adapter tiene datos? → Log de itemCount en check{}

PASO 3 — Verificar la lógica de visibilidad
├── ¿loadStateFlow emite? → addLoadStateListener con logs
├── ¿onPagesUpdatedFlow emite? → Log en collect{}
└── ¿La condición de visibilidad se evalúa con itemCount correcto?
    → Separar loadStateFlow (estados de carga) de onPagesUpdatedFlow (itemCount)

PASO 4 — Verificar la sincronización con Espresso
├── ¿Thread.sleep es suficiente? → Reemplazar por waitForView
├── ¿waitForView agota el timeout? → El problema es estructural, no de timing
└── ¿La aserción usa isDisplayed() o una alternativa más flexible?
    → isDisplayed() requiere VISIBLE + dimensiones > 0
```

### Señales de alerta temprana

| Señal | Posible causa | Verificación |
|-------|--------------|--------------|
| `PROCESS ENDED` inmediatamente tras `submitData()` | `.use{}` en `launchFragmentInHiltContainer` | Revisar el helper |
| `visibility=GONE` con `itemCount>0` | Race condition en lógica de visibilidad | Logs en `loadStateFlow` y `onPagesUpdatedFlow` |
| `waitForView` agota timeout sin éxito | Bug estructural, no timing | Diagnóstico con `check { println(...) }` |
| `loadStateListener` nunca dispara logs | Activity destruida o adapter sin RecyclerView | Verificar ciclo de vida de la Activity |
| `child-count=0` con `itemCount>0` | RecyclerView nunca hizo layout por estar `GONE` | Verificar condición de visibilidad inicial en XML |

---

## 9. Recomendaciones y observaciones

### Para tests instrumentados con Paging 3

**1. Nunca usar `loadStateFlow` para controlar visibilidad dependiente de `itemCount`**

`loadStateFlow` y `itemCount` son asincrónicos entre sí. Usa `onPagesUpdatedFlow` cuando necesites verificar el conteo de items — garantiza que `itemCount` ya está actualizado cuando emite.

```kotlin
// ❌ Frágil
loadStateFlow.collect { refresh is NotLoading && adapter.itemCount > 0 }

// ✅ Correcto
onPagesUpdatedFlow.collect { adapter.itemCount > 0 }
```

**2. Registrar `addLoadStateListener` antes de `submitData()`**

El listener se registra sincrónicamente — a diferencia de `loadStateFlow.collectLatest` dentro de una corrutina, que puede activarse demasiado tarde. Si el orden importa, usa el listener.

**3. `PagingData.from(list)` es la herramienta correcta para tests de UI**

Para testear la capa de UI (adapter, RecyclerView, visibilidad) no necesitas el `PagingSource` real. `PagingData.from(list)` crea datos estáticos que eliminan la complejidad de paginación real, aislando correctamente la responsabilidad del test.

### Para `launchFragmentInHiltContainer`

**4. Nunca usar `.use{}` con `ActivityScenario` en tests que involucren operaciones asíncronas**

`.use{}` = `Closeable` = la Activity se destruye al salir del bloque. Cualquier corrutina, Flow, o Paging 3 que necesite el `lifecycleScope` o `viewModelScope` activo fallará silenciosamente.

**5. El observer con `lateinit var` es la forma correcta de auto-remover observers**

Las lambdas anónimas en `observeForever { }` no son removibles porque cada llamada crea una nueva instancia. Guardar el observer en `lateinit var` permite la auto-remoción y previene memory leaks en tests largos o con múltiples recreaciones de vista.

### Observaciones generales de desarrollo

**6. Un test que falla puede estar detectando un bug real de producción**

Este fue el aprendizaje más importante de esta sesión. Antes de asumir que "el test está mal", agota el diagnóstico de la lógica de producción. En este caso, ambos bugs afectarían a usuarios reales.

**7. Los logs de thread ID son una herramienta de diagnóstico subestimada**

Ver `thread-9541` (test) y `thread-9518` (UI) en el log reveló inmediatamente que había dos hilos compitiendo, lo cual fue la pista que llevó a la causa raíz. Cuando en Android hay comportamiento asíncrono inesperado, agregar el thread ID al log (`Thread.currentThread().id`) puede ahorrarte horas de debugging.

**8. El `check { view, _ -> println(...) }` de Espresso no lanza excepciones**

A diferencia de `check(matches(...))`, el bloque `check { }` con lógica personalizada solo falla si tú lanzas una excepción explícitamente. Esto puede dar falsos positivos si se usa para diagnóstico — el test "pasa" aunque el estado de la View no sea el esperado.

**9. Separar responsabilidades en `observeMovies()` mejora tanto la testabilidad como la legibilidad**

Un collector por responsabilidad:
- `addLoadStateListener` → estados de carga (loading, error)
- `onPagesUpdatedFlow` → visibilidad basada en datos
- `collectLatest { submitData() }` → alimentar el adapter

Mezclar todas estas responsabilidades en un solo `collectLatest` crea exactamente las race conditions que documentamos aquí.

**10. `waitForView` es útil, pero no es un reemplazo de `IdlingResource`**

Para proyectos con muchos tests instrumentados y CI, el siguiente paso evolutivo es implementar un `RecyclerViewIdlingResource` que le comunique a Espresso el estado real del adapter de Paging 3. Esto eliminaría completamente la necesidad de `Thread.sleep` o helpers de polling manual.

---

*Documentación generada a partir de una sesión real de debugging — Junio 2026*  
*Proyecto: `com.example.apptest` — Android Developer: Ale (Joaquín Alejandro Vicente Sánchez)*
