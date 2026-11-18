# Estrategia de testing

Guía de qué se prueba en cada nivel, cómo ejecutarlo y qué convenciones sigue el proyecto.
Los comandos están escritos para PowerShell (Windows); en Linux/macOS usa `./gradlew`.

## Qué se prueba y dónde

| Nivel | Carpeta | Herramientas | Qué cubre |
|---|---|---|---|
| Unitarios | `src/test` | JUnit4, MockK, Truth, Turbine, coroutines-test, `TestPager` | `PagingSource`, repositorios, ViewModels (con `MainDispatcherRule`), contrato con respuestas reales de TMDB (fixtures), funciones puras de presentación |
| UI Compose | `src/test` (Robolectric) | `createAndroidComposeRule<ComponentActivity>()` | Pantallas y componentes de la feature Series: estados, callbacks, estado expandible, restauración de estado, tamaños y font scale |
| Contrato de navegación | `src/test` (Robolectric) | Safe Args | Las claves `ARG_*` de los ViewModels coinciden con los `android:name` de `nav_graph.xml` |
| Instrumentados | `src/androidTest` | Espresso, Hilt (`@HiltAndroidTest`, `@BindValue`), `createEmptyComposeRule()` | Fragments XML, navegación con `MainActivity` real y recorrido E2E de Series |

Archivos de UI Compose de Series: `TvSeriesDetailsScreenTest`, `TvSeriesScreenTest`, `TvSeasonScreenTest`,
`TvEpisodeItemTest`, `TvSeriesStateRestorationTest`, `TvSeriesConfigurationTest`.
Navegación: `NavGraphArgsContractTest` (JVM), `NavigationTest` y `TvSeriesNavigationTest` (instrumentados).

## Cómo ejecutarlos

```powershell
# Todos los unitarios y de UI Compose (JVM, sin emulador)
.\gradlew testDebugUnitTest

# Una sola clase
.\gradlew testDebugUnitTest --tests "*TvEpisodeItemTest*"

# Comprobar que compilan las dos variantes de tests (útil tras tocar testShared)
.\gradlew :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin

# Instrumentados (requieren emulador o dispositivo con las animaciones desactivadas)
.\gradlew connectedDebugAndroidTest

# Una clase instrumentada
.\gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.alexvicente.moviedb.navigation.TvSeriesNavigationTest"

# Cobertura de los unitarios (HTML en app/build/reports/jacoco/html)
.\gradlew jacocoTestReport
```

> **No uses `./gradlew test`.** Ejecuta los unit tests de todas las variantes, y los tests de UI Compose solo
> funcionan en `debug`, el único tipo de build que incluye `ui-test-manifest`. Usa `testDebugUnitTest`.

## Dónde va cada archivo de apoyo

| Lo usa… | Carpeta |
|---|---|
| Unitarios e instrumentados | `src/testShared/java` (fábricas, fakes de datos fijos) |
| Solo unitarios | `src/test/java` |
| Solo instrumentados | `src/androidTest/java` |

`testShared` se compila dentro de `test` y de `androidTest`. Por eso **un nombre de clase no puede repetirse**
entre `testShared` y `src/test` (daría `Redeclaration`), y un archivo de `testShared` solo puede depender de
librerías presentes en ambos classpaths.

## Convenciones

- **Fakes antes que mocks.** Un `mockk(relaxed = true)` sobre un método que devuelve una clase sellada
  (`Resource`) devuelve un mock que no coincide con ninguna rama del `when` y rompe lejos del origen.
- **Nombres de fakes:** `Fake*` (en `src/test`) son los que cada test configura con `result`/`requested`;
  `Canned*` (en `testShared`) devuelven datos fijos y registran los argumentos recibidos.
- **Fábricas** (`MovieFactory`, `TvFactory`, `FavoriteFactory`): un parámetro con valor por defecto por campo,
  ids únicos en las listas (las listas Compose usan `key = { it.id }`).
- **Textos en los tests:** se leen de `R.string` con `composeRule.activity.getString(...)`, no se escriben literales.
- **Estilo:** nombres `whenX_thenY` y comentarios Given/When/Then.

## Tests de UI Compose

- **Configuración de Robolectric:** `@Config(sdk = [34], application = Application::class)`.
  `sdk = 34` porque Robolectric 4.14.1 llega a la API 35 y el proyecto compila con 36.
  `application = Application::class` evita `MovieDbApplication`, cuyo `onCreate` llama a
  `FirebaseCrashlytics.getInstance()` y lanza `IllegalStateException` bajo Robolectric.
- **Pantalla alta** (`qualifiers = "w360dp-h1600dp"`): el `LazyColumn` compone todos los ítems, y
  `assertDoesNotExist()` es fiable.
- **`@GraphicsMode(NATIVE)`** cuando el test depende de medir texto (`hasVisualOverflow`, font scale).
  Con el modo LEGACY el texto no se mide de verdad. Es más lento: no se aplica a todas las clases.
- **Paging en pantallas:** `PagingData.from(lista, sourceLoadStates = ...)`. Sin `sourceLoadStates`,
  `LazyPagingItems` se queda en `refresh = Loading` y la lista nunca aparece.
- **`assertDoesNotExist()` siempre con su control positivo:** pasa tanto si el elemento está bien oculto como
  si el test nunca pudo encontrarlo.
- **Restauración de estado:** todo `rememberSaveable` se prueba con `StateRestorationTester` (recreación) y,
  en listas, saliendo y volviendo a entrar a la composición por scroll.
- **Tamaños y font scale:** `DeviceConfigurationOverride` (`ForcedSize`, `FontScale`). Son guardas de
  comportamiento; los solapamientos y recortes visuales requieren screenshot tests.

## Tests de navegación (instrumentados)

- `HiltAndroidRule` con `order = 0` y `createEmptyComposeRule()` con `order = 1`; la `Activity` se lanza a
  mano en `setUp`, después de configurar los repositorios.
- La espera de transiciones se hace con `waitUntil` sobre el texto esperado, no con `Thread.sleep`.
- Cada salto de navegación debe poder probarse sin depender del anterior: un helper que navega directo
  al destino evita que un fallo temprano tumbe todos los tests con un timeout.

## Verificación en rojo

Un test que nunca se ha visto fallar no demuestra nada. Antes de dar por bueno un test:

1. Ejecútalo en verde.
2. Rompe a propósito la línea de producción que protege.
3. Comprueba que falla con el mensaje esperado.
4. Revierte la mutación (`git diff app/src/main` debe quedar vacío antes de cada commit).

## Cobertura

Línea base (18-nov-2026, solo unit tests de `debug`): **51 % de instrucciones** (12.113 de 24.893 sin cubrir) y
**39 % de ramas** (1.234 de 2.030 sin cubrir).

Cómo leer el reporte:

- **Solo incluye unit tests.** Fragments, adapters y `MainActivity` salen en 0 % aunque los cubran los tests
  instrumentados, cuya cobertura no se fusiona todavía en el reporte.
- **El código generado cuenta en el total:** Apollo (`graphql.*`) y los módulos de Hilt (`*.di`).
- **En Compose, el porcentaje de ramas no es una meta útil.** Incluye código que genera el compilador
  (comprobaciones `$changed` y `$default`) y las funciones `@Preview`. Se revisan las líneas sin cubrir de
  cada clase, no el porcentaje del paquete.
- **Capas de lógica de Series ya altas:** `tv_series.presentation` 96 %, `data.repository` 96 %,
  `data.paging` 98 % y `domain.usecase` 100 %.

## Pendiente

- **Screenshot tests:** a la espera de subir AGP (hoy 8.6.0). El plugin actual de pruebas de capturas
  requiere AGP 9.5 o superior; la versión legacy está deprecada.
- **`MovieDetailsModule` en `NavigationTest`:** no se desinstala, así que el detalle de una película usa el
  repositorio real (red). Falta un fake de `MovieDetailsRepository` para que esos tests sean herméticos.
- **BOM de Compose:** el catálogo declara `2025.04.01` (Compose 1.8.0), pero Gradle resuelve Compose 1.10.0
  en `androidTest`. Revisar quién arrastra esa versión al migrar Kotlin, AGP y el BOM.
- **Tests de `Route` con Hilt** para las pantallas Compose de Series.