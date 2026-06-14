package debug.java.com.example.apptest

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity vacía usada exclusivamente como contenedor en tests instrumentados.
 *
 * ¿Por qué en debug/ y no en androidTest/ ?
 *   Las Activities deben estar en el APK principal para que el sistema Android
 *   pueda instanciarlas. debug/ se incluye en el APK de debug (el que instalan los tests)
 *   pero NO en el APK de release - no contamina producción.
 *
 * ¿Por qué necesitamos esto?
 *   launchFragmentInContainer usa una Activity interna de AndroidX que no tiene
 *   @AndroidEntryPoint. Hilt requiere que la Activity contenedora tenga esa anotación
 *   para poder inyectar en el Fragment. Sin esto el test crashea con
 *   "injecting into a non-Hilt activity"
 */

@AndroidEntryPoint
class HiltTestActivity: AppCompatActivity()