package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t

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
    onTanitim: () -> Unit,
) {
    BackHandler(enabled = sayfa != DigerSayfa.MENU) { onSayfa(DigerSayfa.MENU) }
    when (sayfa) {
        DigerSayfa.KAZA -> KazaEkrani(onGeri = { onSayfa(DigerSayfa.MENU) })
        DigerSayfa.MENU -> DigerMenu(onSayfa, onAyarlar, onTanitim)
    }
}

@Composable
private fun DigerMenu(onSayfa: (DigerSayfa) -> Unit, onAyarlar: () -> Unit, onTanitim: () -> Unit) {
    val context = LocalContext.current
    val widgetEklenebilir = androidx.compose.runtime.remember {
        context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(t("Diğer"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            MenuSatiri("📝", t("Kaza takibi"), t("Kaza namazı ve orucu sayacı")) { onSayfa(DigerSayfa.KAZA) }
            if (widgetEklenebilir) {
                val ekle = { sinif: Class<*> ->
                    context.getSystemService(AppWidgetManager::class.java)
                        ?.requestPinAppWidget(ComponentName(context, sinif), null, null)
                    Unit
                }
                MenuSatiri("🧩", t("Ana ekrana widget ekle"), t("Sıradaki vakit ve kalan süre ana ekranda")) { ekle(VakitWidget::class.java) }
                MenuSatiri("🔹", t("Küçük widget ekle"), t("Yalnızca sıradaki vakit ve geri sayım")) { ekle(com.example.ezanvakti.widget.KucukWidget::class.java) }
                MenuSatiri("➖", t("Şerit widget ekle"), t("Günün vakitleri yan yana, tek satırda")) { ekle(com.example.ezanvakti.widget.SeritWidget::class.java) }
            }
            MenuSatiri("⚙", t("Ayarlar"), t("Bildirimler, Cuma hatırlatması, konum"), onAyarlar)
            MenuSatiri("👋", t("Tanıtım turunu tekrar göster"), t("Konum, görünüm, bildirim ve widget ayarları adım adım"), onTanitim)
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
