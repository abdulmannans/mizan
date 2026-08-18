package app.mizan.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.mizan.android.notify.MizanNotifier
import app.mizan.android.ui.MizanApp
import app.mizan.android.ui.theme.MizanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = routeFrom(intent)

        setContent {
            MizanTheme {
                Surface {
                    MizanApp(
                        pendingRoute = pendingRoute,
                        onRouteConsumed = { pendingRoute = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = routeFrom(intent)
    }

    /** Notifications carry the destination so a tap lands on the fund or metal that fired. */
    private fun routeFrom(intent: Intent?): String? =
        intent?.getStringExtra(MizanNotifier.EXTRA_ROUTE)?.takeIf { it != "home" }
}
