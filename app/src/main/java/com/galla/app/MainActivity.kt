package com.galla.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.galla.app.ui.GallaRoot

/**
 * The app's only Activity (Android's word for "a window"). Every screen is drawn inside it
 * with Jetpack Compose; moving between screens never opens another Activity.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as GallaApplication).container
        setContent {
            GallaRoot(
                container = container,
                onThemeResolved = { dark ->
                    // Draw behind the status and navigation bars, with icons that contrast the theme.
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                },
            )
        }
    }
}
