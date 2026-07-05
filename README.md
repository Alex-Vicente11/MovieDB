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
| Red | Retrofit + OkHttp + Gson |
| Persistencia | Room |
| Paginación | Paging 3 |
| Imágenes | Glide |
| Navegación | Navigation Component (Single Activity) |
| Async | Coroutines + StateFlow |
| UI | ViewBinding + Material Design 3 |

---

## Arquitectura

El proyecto sigue **Clean Architecture** organizada por features:

```
com.alexvicente.moviedb
├── core/                      # Código compartido entre features
│   ├── data/
│   │   ├── local/             # Room: DAOs, Entities, Database
│   │   ├── mapper/            # Entity → Domain mappers
│   │   └── network/           # Retrofit, OkHttp, Interceptors
│   ├── di/                    # Módulos Hilt compartidos
│   ├── domain/model/          # Modelos de dominio
│   └── util/                  # Constants, Extensions, AppError
│
└── features/
    ├── favorites/             # Favoritos (100% local, sin API)
    ├── genres/                # Géneros + películas por género (Paging 3)
    ├── movie_details/         # Detalle de película
    ├── popular_movies/        # Películas populares
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

- **Películas populares** — listado paginado con caché offline (Room)
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

**Manejo de errores centralizado** — `AppError` + `ErrorMapper` + `Resource<T>` proveen un sistema unificado de manejo de errores en todas las capas.

**Single Activity** — Toda la navegación ocurre dentro de `MainActivity` mediante Navigation Component.

**Favoritos sin API** — El módulo de favoritos es 100% local — Room es la única fuente de datos, sin Retrofit ni DTOs.

---

## Deuda técnica conocida

- Cobertura de tests: ~20% (objetivo: 60% en v1.1)
- Tests instrumentados pendientes: `MovieDetailsFragment`, `SearchFragment`, `VideosFragment`
- `fallbackToDestructiveMigration` activo — se eliminará cuando se implementen migraciones de Room

---

## Licencia

Este proyecto es de uso educativo y portfolio personal.
