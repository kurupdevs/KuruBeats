/*
 * KuruBeats (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.kurubeats.app.automotive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.kurubeats.app.ui.screens.settings.AndroidAutoSettingsRoute
import com.kurubeats.app.ui.theme.KuruBeatsTheme

@AndroidEntryPoint
class AutomotiveSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KuruBeatsTheme(disableAnimations = true) {
                AndroidAutoSettingsRoute(onBack = ::finish)
            }
        }
    }
}
