# MovieDB 🎬

Aplicación Android para explorar películas usando la API de [The Movie Database (TMDB)](https://www.themoviedb.org/). Desarrollada como proyecto de portfolio con enfoque en arquitectura limpia y buenas prácticas de la industria.

---

## Capturas de pantalla

| Películas populares | Detalle de película | Géneros |
|:---:|:---:|:---:|
| ![Popular Movies](screenshots/screenshot_popular.png) | ![Movie Detail](screenshots/screenshot_detail.png) | ![Genres](screenshots/screenshot_genres.png) |

| Favoritos | Videos y Trailers |
|:---:|:---:|
| ![Favorites](screenshots/screenshot_favorites.png) | ![Videos](screenshots/screenshot_videos.png) |

---

## Stack tecnológico

| Categoría | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Arquitectura | Clean Architecture + MVVM |
| DI | Hilt |
| Red (REST) | Retrofit + OkHttp + Gson |
| Red (GraphQL) | Apollo Kotlin, sobre un proxy propio (Flask + Strawberry) |
| Persistencia | Room |
| Paginación | Paging 3 |
| Imágenes | Glide |
| Navegación | Navigation Component (Single Activity) |
| Async | Coroutines + StateFlow |
| UI | ViewBinding + Material Design 3 |
| Logging | Timber |
| Crash/ANR reporting | Firebase Crashlytics |
| Seguridad de red | Certificate Pinning (OkHttp) |

---

## Arquitectura

El proyecto sigue **Clean Architecture** organizada por features:

```
com.alexvicente.moviedb
├── core/                      # Código compartido entre features
│   ├── data/
│   │   ├── local/             # Room: DAOs, Entities, Database
│   │   ├── mapper/            # Entity → Domain mappers
│   │   └── network/           # Retrofit, Apollo, OkHttp, Interceptors
│   ├── di/                    # Módulos Hilt compartidos
│   ├── domain/model/          # Modelos de dominio
│   └── util/                  # Constants, Extensions, AppError, ReleaseTree
│
└── features/
    ├── favorites/             # Favoritos (100% local, sin API)
    ├── genres/                # Géneros + películas por género (Paging 3)
    ├── movie_details/         # Detalle de película
    ├── popular_movies/        # Películas populares (vía GraphQL/Apollo)
    ├── search/                # Búsqueda con debounce
    └── videos/                # Videos/trailers de películas
```

Cada feature sigue la estructura:
```
feature/
├── data/          # DTOs, API, RepositoryImpl
├── di/            # Módulo Hilt del feature
├── domain/        # Modelos, Repository interface, UseCases
└── presentation/  # ViewModel, Fragment, Adapter, UiState
```

---

## Features

- **Películas populares** — listado paginado con caché offline (Room), consumido vía GraphQL/Apollo
- **Géneros** — lista de géneros con filtrado de películas por género (Paging 3)
- **Detalle de película** — información completa con rating, presupuesto, ingresos y géneros
- **Videos/Trailers** — reproducción de trailers vía app de YouTube o navegador web
- **Búsqueda** — búsqueda en tiempo real con debounce de 500ms
- **Favoritos** — agregar/eliminar favoritos almacenados localmente con Room

---

## Configuración del proyecto

### Requisitos

- Android Studio Hedgehog o superior
- JDK 21 (JBR recomendado — configurar en **Settings → Build → Gradle → Gradle JDK**)
- API key de TMDB (Bearer Token de lectura)
- minSdk 24 / targetSdk 36

### Configuración de la API key

1. Crear una cuenta en [themoviedb.org](https://www.themoviedb.org/) y obtener un **Bearer Token** de lectura
2. En la raíz del proyecto, agregar al archivo `local.properties`:

```properties
TMDB_TOKEN=tu_bearer_token_aqui
```

3. Sincronizar Gradle — el token se inyecta automáticamente via `BuildConfig.TMDB_TOKEN`

> ⚠️ `local.properties` está en `.gitignore` y nunca debe subirse al repositorio.

### Clonar y ejecutar

```bash
git clone https://github.com/Alex-Vicente11/AppTest.git
cd AppTest
# Agregar TMDB_TOKEN a local.properties
# Abrir en Android Studio y ejecutar
```

---

## Tests

El proyecto cuenta con tres niveles de testing:

| Tipo | Herramientas | Cobertura |
|---|---|---|
| Unit tests | JUnit, MockK, Truth, Turbine | ~20% |
| Unit tests con contexto Android | Robolectric | Incluido |
| Instrumentados | Espresso, Hilt Testing, MockK Android | Fragmentos principales |

### Ejecutar tests

```bash
# Unit tests
./gradlew test

# Reporte de cobertura JaCoCo
./gradlew jacocoTestReport
# Reporte en: build/reports/jacoco/html/index.html

# Tests instrumentados (requiere emulador o dispositivo)
./gradlew connectedAndroidTest
```

---

## Decisiones técnicas destacadas

**Paging 3 con caché offline** — Las películas populares usan Room como _single source of truth_. La red solo se consulta cuando la caché está vacía o expirada.

**Manejo de errores centralizado** — `AppError` + `ErrorMapper` + `Resource<T>` proveen un sistema unificado de manejo de errores en todas las capas, extendido para cubrir también los errores de GraphQL (ver sección de GraphQL/Apollo).

**Single Activity** — Toda la navegación ocurre dentro de `MainActivity` mediante Navigation Component.

**Favoritos sin API** — El módulo de favoritos es 100% local — Room es la única fuente de datos, sin Retrofit ni DTOs.

**Convivencia REST + GraphQL** — El proyecto consume datos tanto por Retrofit (REST, directo a TMDB) como por Apollo (GraphQL, vía proxy propio), decisión consciente para demostrar migración incremental sin reescribir toda la capa de red de una vez (ver sección siguiente).

---

## GraphQL / Apollo Kotlin

El feature de **películas populares** consume datos a través de un **proxy GraphQL propio** en vez de hablar directo con el REST de TMDB, mientras el resto de la app (detalle, videos, búsqueda) sigue usando Retrofit sin cambios — una migración **incremental y selectiva**, no un reemplazo total de la capa de red.

### Por qué un proxy

TMDB no ofrece una API GraphQL nativa. El proxy (Flask + [Strawberry](https://strawberry.rocks/)) actúa como capa de traducción: expone un schema GraphQL hacia Android, y por dentro sigue consumiendo el REST de TMDB como cualquier otro cliente — el mismo patrón de **BFF (Backend For Frontend)** que usan empresas con múltiples microservicios detrás de una sola API orientada al cliente.

```
Android (Apollo Kotlin) → Proxy Flask/Strawberry (Render) → TMDB REST API
```

- **Repo**: [`moviedb-graphql-proxy`](https://github.com/Alex-Vicente11/moviedb-graphql-proxy)
- **Desplegado en**: Render (free tier), `https://moviedb-graphql-proxy.onrender.com/graphql`

### Beneficios de infraestructura demostrados (no solo teóricos)

- **Selección de campos** — el cliente pide exactamente los campos que la pantalla necesita (`id`, `title`, `posterPath`), sin los ~20 campos adicionales que TMDB devuelve por defecto.
- **Un solo round-trip para datos anidados** — géneros se resuelven dentro de la misma query (`popularMovies { genres { name } } }`), evitando una segunda llamada REST a `/genre/movie/list`.
- **Problema N+1 identificado y resuelto** — el resolver de géneros originalmente repetía la llamada a TMDB por cada película. Se resolvió con un cache en memoria con TTL de 1 hora, verificado empíricamente con logging temporal (1 sola llamada real en vez de 20 por página).

### Hallazgo técnico: incompatibilidad de spec entre Strawberry y Apollo Kotlin

La introspección HTTP estándar de GraphQL falló al descargar el schema: Strawberry incluye `DIRECTIVE_DEFINITION` como ubicación de directiva (parte de una extensión del spec de GraphQL aceptada recientemente), que **Apollo Kotlin no soporta en ninguna versión estable** (confirmado hasta la 4.2.0; solo existe en snapshots 5.x no productivos). Solución: generar el SDL del schema manualmente (`schema.as_str()` de Strawberry) y usarlo como archivo local en vez de depender de `downloadApolloSchema` vía introspección — evita la ruta del ecosistema que aún no está sincronizada, sin perder tipado fuerte ni generación de código.

### Manejo de errores GraphQL (distinto de status codes HTTP)

A diferencia de REST, una respuesta GraphQL casi siempre es `200 OK` — el éxito o fallo se reporta dentro del body, no en el código HTTP. Se implementó manejo diferenciado en 3 niveles, integrado al sistema de errores existente (`AppError`, `ErrorMapper`):

1. **Errores de red/transporte** (`ApolloNetworkException`) → `AppError.Network`
2. **Errores GraphQL sin datos utilizables** (`response.hasErrors() && data == null`) → excepción custom `GraphQLDataException` → `AppError.GraphQL`
3. **Éxito parcial** (datos *y* errores en la misma respuesta — posible en GraphQL, no en REST) → se muestran los datos disponibles sin romper la pantalla, con log interno del campo que falló

### Certificate Pinning sobre ambos dominios

El `CertificatePinner` de OkHttp se comparte entre Retrofit y Apollo (mismo `OkHttpClient` inyectado), pinneando hoja + CA intermedia para `api.themoviedb.org` y `moviedb-graphql-proxy.onrender.com`. Pinnear la CA intermedia (no solo la hoja) tolera la rotación automática de certificados del proxy (gestionados por Render) sin romper la app en cada renovación. Verificado corrompiendo los pines a propósito y confirmando `SSLPeerUnverifiedException: Certificate pinning failure!` en Logcat, con el detalle completo de certificados esperados vs. recibidos.

### Testing del flujo GraphQL

A diferencia de Retrofit —donde `MockWebServer` simula respuestas HTTP crudas—, el repositorio de Apollo se testea mockeando directamente el contrato de `ApolloClient` con MockK: se simula lo que `.execute()` devuelve o lanza, sin pasar por una capa de transporte real. La construcción de respuestas falsas usa `ApolloResponse.Builder` para los casos de éxito y error GraphQL (`response.hasErrors() && data == null`), y excepciones reales de Apollo (`ApolloHttpException`, `ApolloNetworkException`) para los casos de error de red/HTTP — preservando toda la cobertura de caché offline-first (válido/expirado/vacío) que ya existía para el repositorio basado en Retrofit, sin cambios de comportamiento.

### Por qué no se migró todo a GraphQL

Fue una decisión deliberada de alcance, no una limitación técnica: extender el proxy para cubrir `movieDetail`, videos y búsqueda es perfectamente viable (el patrón ya está probado), pero no aporta aprendizaje adicional sobre lo ya demostrado con `popularMovies`. Mantener ambos clientes conviviendo también refleja un escenario real de migración incremental, más representativo de cómo ocurre en equipos de producción que una reescritura completa de una sola vez.

---

## CI/CD Pipeline

Este proyecto cuenta con un pipeline de integración continua implementado con **Jenkins** y **Docker**, siguiendo un enfoque de Infrastructure as Code, migrado a **Multibranch Pipeline** para descubrir automáticamente ramas y Pull Requests sin configuración manual por rama.

### Infraestructura

- **`ci/Dockerfile`**: define un agente Jenkins personalizado (basado en `jenkins/jenkins:lts`) con Android SDK, build-tools y platforms preinstalados, garantizando builds reproducibles sin configuración manual.
- **`Jenkinsfile`**: define el pipeline como código, con las siguientes etapas:
  1. **Checkout** — usa `checkout scm`, delegando en la configuración del Multibranch Pipeline para clonar la rama o Pull Request correcto en cada contexto (vía SSH, con Deploy Key de solo lectura)
  2. **Prepare SDK** — genera `local.properties` dinámicamente
  3. **Lint** — análisis estático de código
  4. **Unit Tests** — ejecuta la suite completa de tests unitarios
  5. **Build APK** — genera el artefacto `.apk` de debug (solo en `main` y en Pull Requests, ver detalle abajo)

### Multibranch Pipeline

El job escanea el repositorio completo y crea automáticamente un sub-job por cada rama que contenga un `Jenkinsfile`, además de detectar Pull Requests como jobs independientes.

- **Discover branches**: descubre ramas automáticamente, excluyendo aquellas que ya están cubiertas por un Pull Request abierto (evita builds duplicados)
- **Discover pull requests from origin**: descubre PRs y ejecuta el build sobre un **merge simulado** con la rama destino (estrategia *"Merging the pull request with the current target branch revision"*), validando cómo quedaría el código ya integrado, no solo la rama aislada
- **Checkout over SSH**: fuerza que el clonado use la Deploy Key SSH dedicada, separado de las credenciales usadas para el descubrimiento vía API de GitHub
- **Scan periódico**: re-escaneo cada 1 hora como respaldo del webhook
- **Orphaned Item Strategy**: sub-jobs de ramas eliminadas se descartan automáticamente tras 7 días

### Build condicional por contexto

El stage `Build APK` solo se ejecuta en `main` o cuando el build corresponde a un Pull Request (`changeRequest()`), evitando generar artefactos innecesarios en ramas de feature sueltas sin PR abierto:

```groovy
stage('Build APK') {
    when {
        anyOf {
            branch 'main'
            changeRequest()
        }
    }
    steps {
        sh './gradlew assembleDebug'
    }
}
```

Lint y Unit Tests corren siempre, en cualquier rama o PR, dando señal temprana de errores sin esperar a un merge.

### Automatización

El pipeline se dispara automáticamente en cada `push` a cualquier rama y en cada evento de Pull Request (`opened`, `synchronize`, etc.) mediante un **webhook de GitHub**, sin intervención manual.

### Branch Protection

La rama `main` está protegida mediante un **Ruleset** de GitHub:

- Requiere Pull Request antes de mergear (no se permite push directo a `main`)
- Requiere que el check `continuous-integration/jenkins/pr-merge` pase exitosamente
- Requiere que la rama esté actualizada con `main` antes de mergear
- Bloquea force pushes y restringe la eliminación de la rama

Este flujo fue validado de punta a punta: se forzó intencionalmente el fallo de un test (`assertThat(movies).hasSize(5)` → `hasSize(4)`) para confirmar que GitHub bloquea el botón de merge mientras el check requerido esté en rojo, y que se desbloquea automáticamente al corregir el test y que el build vuelva a pasar.

### Seguridad

- Autenticación vía SSH con una **Deploy Key dedicada de solo lectura** (`git`), independiente de las credenciales personales del desarrollador y separada del token usado para el descubrimiento de ramas/PRs vía API
- Autenticación vía **Personal Access Token (fine-grained)** para el descubrimiento de ramas/PRs y el reporte de status checks a GitHub, con permisos mínimos necesarios (Contents: read, Pull requests: read, Commit statuses: read/write)
- Credenciales gestionadas mediante el sistema de Credentials de Jenkins (nunca expuestas en texto plano)

### Reproducibilidad

Se validó que el `.apk` generado por el pipeline es **binariamente idéntico** (0 bytes de diferencia por archivo) al generado localmente, confirmando la consistencia del entorno de build.

---

## CI/CD con GitHub Actions

Como ejercicio comparativo, el mismo pipeline se replicó en **GitHub Actions**, usando el CI nativo de la plataforma en vez de un servidor propio.

### Estructura

A diferencia de Jenkins (un único `Jenkinsfile` con lógica condicional según el contexto), el pipeline de Actions se dividió en **dos workflows independientes**, cada uno con una única responsabilidad:

- **`.github/workflows/android-ci-pr.yml`** — trigger `pull_request` hacia `main`. Corre Checkout, Prepare SDK, Lint, Unit Tests y Build APK siempre (al estar acotado al contexto de PR, no necesita condicional). Es el check que protege `main`.
- **`.github/workflows/android-ci-branch.yml`** — trigger `push` a cualquier rama. Mismas etapas, pero `Build APK` condicionado a `github.ref == 'refs/heads/main'`, evitando generar artefactos en ramas de feature sueltas. Es informativo, no bloquea merges.

Separar en dos archivos (en vez de un único workflow con ambos triggers, como se intentó inicialmente) resolvió además un problema real de builds duplicados: un mismo push a una rama con PR abierto disparaba el workflow dos veces (`push` y `pull_request`) por el mismo cambio.

### Equivalencias con Jenkins

| Jenkins | GitHub Actions | Nota |
|---|---|---|
| `agent any` + `ci/Dockerfile` | `runs-on: ubuntu-latest` | Actions ya trae Android SDK preinstalado; Jenkins requirió construir una imagen propia |
| `checkout scm` | `actions/checkout@v4` | Equivalente directo |
| Credencial `tmdb-token` (Secret text) | `${{ secrets.TMDB_TOKEN }}` | Mismo propósito, gestionado como GitHub Secret en vez de credencial de Jenkins |
| `when { anyOf { branch 'main'; changeRequest() } }` | `if: github.ref == 'refs/heads/main'` (en el workflow de branch) | En Actions, el equivalente al contexto de PR ya está separado en su propio workflow, sin necesitar el condicional |
| Multibranch Pipeline + Behaviours (discovery de ramas/PRs) | No aplica — cualquier push o PR con el archivo en `.github/workflows/` dispara el workflow automáticamente | Actions no requiere configurar discovery |
| `archiveArtifacts` | `actions/upload-artifact@v4` | Equivalente directo |
| Webhook de GitHub configurado manualmente | Integración nativa | Sin webhooks que mantener ni scans manuales de respaldo |

### Branch Protection con Ruleset (Jenkins + Actions)

La rama `main` está protegida por el mismo Ruleset descrito en la sección de Jenkins, con ambos sistemas de CI reportando estado:

- **GitHub Actions — required.** El check requerido en el Ruleset se llama `build`, que corresponde al **nombre del job** dentro del YAML (`jobs: build:`), no al nombre visible compuesto que muestra la interfaz (`Android CI Branch / build (push)`). Este fue el hallazgo clave de un troubleshooting extenso: GitHub hace el matching contra el identificador del job, no contra el string completo mostrado en la UI. Como consecuencia, **ambos workflows** (`android-ci-pr.yml` y `android-ci-branch.yml`) — al compartir el mismo nombre de job `build` — quedan cubiertos por la misma regla `Required`.
- **Jenkins (`continuous-integration/jenkins/pr-merge`) — informativo.** Sigue corriendo y reportando en cada PR (vía Commit Status API), pero no bloquea el merge. Se mantiene como pipeline de referencia para practicar administración de infraestructura propia (agentes Docker, credenciales, Multibranch Discovery).

Este flujo fue validado de punta a punta con el mismo método que Jenkins: se rompió intencionalmente un test para confirmar que GitHub bloqueaba el merge mientras el check requerido estuviera en rojo, y se confirmó el desbloqueo automático al corregirlo.

### Seguridad

- `TMDB_TOKEN` gestionado como **GitHub Secret** a nivel de repositorio, inyectado en `local.properties` únicamente durante el step `Prepare SDK`
- GitHub Actions enmascara automáticamente el valor del secret en los logs, igual que Jenkins con `withCredentials`

### Panorama: ¿por qué elegir Jenkins o GitHub Actions?

La elección real en la industria depende menos del tamaño de la empresa y más de estos factores:

- **Dónde vive el código** — el CI nativo de la plataforma (Actions para GitHub, GitLab CI para GitLab) suele ganar por defecto si no hay una razón para pelear contra esa integración.
- **Quién es dueño de la infraestructura** — sectores regulados (banca, salud, gobierno) suelen requerir CI **self-hosted** por compliance, favoreciendo Jenkins u otras soluciones on-premise, independientemente del tamaño de la empresa.
- **Complejidad heredada del pipeline** — Jenkins y su ecosistema de plugins siguen siendo dominantes en organizaciones con pipelines viejos, heterogéneos o muy específicos de negocio.
- **Tamaño del equipo de plataforma** — equipos pequeños sin presupuesto para mantener infraestructura propia suelen preferir soluciones SaaS como Actions, sin servidores que administrar.

Otras herramientas equivalentes: GitLab CI/CD, CircleCI, Azure DevOps Pipelines, AWS CodePipeline/CodeBuild, TeamCity, Bamboo (CI); ArgoCD/Flux (CD vía GitOps, complementarias a un CI, no sustitutas).

---

## Seguridad de red

Además del Certificate Pinning documentado en la sección de GraphQL/Apollo (que cubre ambos dominios consumidos por la app), el manejo de secretos sigue esta postura:

- El token de TMDB para el flujo de **películas populares** nunca llega al cliente Android — vive únicamente en el `.env` del proxy desplegado en Render.
- Los flujos restantes (`movieDetail`, videos) siguen consumiendo TMDB directo vía Retrofit, con el token en `BuildConfig.TMDB_TOKEN` — ver nota en Deuda técnica.
- `EncryptedSharedPreferences` se evaluó para este proyecto, pero no aplica aquí: protege secretos guardados en runtime (p. ej. un JWT de sesión tras un login), no un valor que ya nace hardcodeado en el binario compilado como `BuildConfig`. Se reserva para otro proyecto con autenticación real de usuario.

---

## Observabilidad

El proyecto integra un stack de logging y monitoreo en producción: **Timber** para logging estructurado, y **Firebase Crashlytics** para reporte de crashes y ANR — ambos con comportamiento diferenciado por build type y verificados empíricamente, no solo compilados.

### Timber

- **Debug**: `Timber.DebugTree()` — auto-tagging por clase, logs completos en Logcat.
- **Release**: `ReleaseTree` (custom) — descarta todo por debajo de `WARN`; los niveles `WARN`/`ERROR` se reenvían a Crashlytics (`log()` como breadcrumb, `recordException()` para excepciones).
- **Regla de R8** (`proguard-rules.pro`): `Timber.v/d/i` se eliminan del bytecode en release vía `-assumenosideeffects`; `w`/`e` se preservan intencionalmente para que `ReleaseTree` pueda seguir reportándolos.

Verificado en el `.dex` del APK de release con marcadores únicos por nivel de log: `v`/`d`/`i` ausentes tras la regla, `w`/`e` presentes como control positivo.

### Firebase Crashlytics

- SDK integrado vía Firebase BoM, con recolección restringida a release (`setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)`) para no contaminar el dashboard con pruebas locales.
- **ANR monitoring** incluido automáticamente por el SDK, sin configuración adicional.

### Patrón de verificación: `DebugActions`

Para probar Crashlytics sin dejar código de prueba alcanzable en producción, se usó una interfaz (`DebugActions`, en `main`) con una implementación distinta por *source set*:

| Source set | Comportamiento |
|---|---|
| `debug` | Lanza un `RuntimeException` real y bloquea el hilo principal (10s) para simular un ANR — disparadores reutilizables desde un botón/long-press debug-only en `MovieDetailsFragment` |
| `release` | No-op |

Gradle nunca fusiona `debug` y `release` en el mismo build, así que el disparador de prueba **no puede compilarse** en producción — una garantía a nivel de compilación, no una condición en runtime. El binding entre la interfaz y la implementación activa se resuelve vía Hilt (`@Binds`), sin ramificar con `BuildConfig.DEBUG` en el código de producción.

Ambos flujos —crash y ANR— se verificaron de extremo a extremo: forzados manualmente (el ANR con `adb shell input` mientras el hilo estaba bloqueado, reproduciendo un timeout real de dispatch), y confirmados en Firebase Console con el stack trace completo hasta el listener exacto.

### Nota de testing: Robolectric y la Application real

Al integrar Crashlytics, los tests de Room con Robolectric (`MovieDaoTest`, `FavoritesDaoTest`) empezaron a fallar con `IllegalStateException` en `FirebaseApp` — Robolectric instancia la `Application` declarada en el manifest por defecto, y su `onCreate()` ahora llama a `FirebaseCrashlytics.getInstance()` antes de que `FirebaseApp` esté inicializado en el entorno simulado (a diferencia de un dispositivo real, donde un `ContentProvider` de Firebase se adelanta). Se resolvió con `@Config(application = Application::class)`, aislando estos tests —que solo necesitan un `Context` válido para Room— de cualquier dependencia de `Application` real.

---

## Deuda técnica conocida

- Cobertura de tests: ~20% (objetivo: 60% en v1.1)
- Tests instrumentados pendientes: `MovieDetailsFragment`, `SearchFragment`, `VideosFragment`
- `fallbackToDestructiveMigration` activo — se eliminará cuando se implementen migraciones de Room
- `TMDB_TOKEN` queda embebido en el `.apk` compilado para los flujos no migrados a GraphQL (`movieDetail`, videos, búsqueda) — limitación inherente de `BuildConfig`, aceptable en este contexto por tratarse de un token de solo lectura sobre una API pública. El flujo de **películas populares** ya resuelve esto vía el proxy GraphQL (ver sección GraphQL/Apollo); extender el proxy a los flujos restantes eliminaría el token del cliente por completo, pero se dejó fuera de alcance de este proyecto a propósito — fue una decisión consciente, no una limitación técnica sin solución conocida.

---

## Licencia

Este proyecto es de uso educativo y portfolio personal.