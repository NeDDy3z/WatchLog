package com.neddy.watchlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.neddy.watchlog.ui.navigation.AppNavigation
import com.neddy.watchlog.ui.theme.WatchlogTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_MEDIA_ID = "notification_media_id"
    }

    private var launchMediaId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchMediaId = intent?.getLongExtra(EXTRA_MEDIA_ID, -1L)?.takeIf { it > 0L }
        setContent {
            WatchlogTheme {
                AppNavigation(initialMediaId = launchMediaId, onInitialMediaIdConsumed = { launchMediaId = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchMediaId = null
        launchMediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L).takeIf { it > 0L }
    }
}
