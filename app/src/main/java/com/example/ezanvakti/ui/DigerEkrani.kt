package com.example.ezanvakti.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.widget.VakitWidget

enum class DigerSayfa { MENU, KAZA }

@Composable
fun DigerEkrani(
    sayfa: DigerSayfa,
    onSayfa: (DigerSayfa) -> Unit,
    zikirVm: ZikirViewModel,
    onZikreGit: () -> Unit,
    onAyarlar: () -> Unit,
) {
    BackHandler(enabled = sayfa != DigerSayfa.MENU) { onSayfa(DigerSayfa.MENU) }
    when (sayfa) {
        DigerSayfa.KAZA -> KazaEkrani(onGeri = { onSayfa(DigerSayfa.MENU) })
        DigerSayfa.MENU -> DigerMenu(onSayfa, onAyarlar)
    }
}

@Composable
private fun DigerMenu(onSayfa: (DigerSayfa) -> Unit, onAyarlar: () -> Unit) {
    val context = LocalContext.current
    val widgetEklenebilir = androidx.compose.runtime.remember {
        context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Diğer", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            MenuSatiri("📝", "Kaza takibi", "Kaza namazı ve orucu sayacı") { onSayfa(DigerSayfa.KAZA) }
            if (widgetEklenebilir) {
                MenuSatiri("🧩", "Ana ekrana widget ekle", "Sıradaki vakit ve kalan süre ana ekranda") {
                    context.getSystemService(AppWidgetManager::class.java)
                        ?.requestPinAppWidget(ComponentName(context, VakitWidget::class.java), null, null)
                }
            }
            MenuSatiri("⚙", "Ayarlar", "Bildirimler, Cuma hatırlatması, konum", onAyarlar)
        }
    }
}

@Composable
private fun MenuSatiri(simge: String, baslik: String, alt: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(simge, fontSize = 26.sp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(baslik, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(alt, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
        }
        Text("›", fontSize = 24.sp, color = Color.White.copy(alpha = 0.6f))
    }
}
