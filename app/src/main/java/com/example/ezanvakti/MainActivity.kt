package com.example.ezanvakti

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ezanvakti.ui.MainScreen
import com.example.ezanvakti.ui.PrayerViewModel
import com.example.ezanvakti.ui.ZikirTuslari
import com.example.ezanvakti.ui.theme.EzanVaktiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Arka plan her zaman koyu degrade olduğu için durum çubuğu simgeleri beyaz
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            EzanVaktiTheme {
                val vm: PrayerViewModel = viewModel()
                MainScreen(vm)
            }
        }
    }

    // Zikir ekranı açıkken ses tuşları zikir sayar (ses seviyesini değiştirmez).
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val dinleyici = ZikirTuslari.dinleyici
        if (dinleyici != null && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.repeatCount == 0) dinleyici()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (ZikirTuslari.dinleyici != null && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
