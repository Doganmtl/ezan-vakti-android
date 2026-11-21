package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.notify.NotificationScheduler
import com.example.ezanvakti.ui.theme.ArkaPlan
import com.example.ezanvakti.ui.theme.VakitDuzeni
import com.example.ezanvakti.ui.theme.arkaPlanRenkleri
import com.example.ezanvakti.widget.KucukWidget
import com.example.ezanvakti.widget.SeritWidget
import com.example.ezanvakti.widget.VakitWidget

private const val SAYFA_SAYISI = 5

/**
 * İlk açılışta gösterilen kısa kullanım turu: konum, görünüm (arka plan + vakit dizilişi),
 * bildirim izinleri ve ana ekran widget'ları. Yapılan seçimler doğrudan kaydedilir; her şey
 * sonradan Ayarlar'dan değiştirilebilir. Diğer › "Tanıtım turunu tekrar göster" ile yeniden açılır.
 */
@Composable
fun TanitimTuru(
    vm: PrayerViewModel,
    state: UiState,
    onListedenSec: () -> Unit,
    onBitti: () -> Unit,
) {
    var sayfa by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = sayfa > 0) { sayfa-- }

    // Arka plan, seçilen temayı canlı gösterir
    val (ust, alt) = arkaPlanRenkleri(ArkaPlan.from(state.arkaPlan), Vakit.AKSAM)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ust, alt)))
            .clickable(enabled = false) {},   // alttaki ekrana dokunulmasın
    ) {
      CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            // Üst satır: adım göstergesi + Atla
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(SAYFA_SAYISI) { i ->
                        Box(
                            Modifier
                                .size(width = if (i == sayfa) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (i == sayfa) 0.95f else 0.35f)),
                        )
                    }
                }
                if (sayfa < SAYFA_SAYISI - 1) {
                    TextButton(onClick = onBitti) { Text(t("Atla"), color = Color.White.copy(alpha = 0.85f)) }
                }
            }

            AnimatedContent(
                targetState = sayfa,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "tanitim",
            ) { s ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (s) {
                        0 -> HosGeldiniz()
                        1 -> KonumAdimi(vm, state, onListedenSec)
                        2 -> GorunumAdimi(vm, state)
                        3 -> BildirimAdimi()
                        else -> WidgetAdimi()
                    }
                }
            }

            // Alt satır: Geri / İleri / Başla
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sayfa > 0) {
                    TextButton(onClick = { sayfa-- }) { Text(t("Geri"), color = Color.White) }
                }
                Spacer(Modifier.weight(1f))
                val son = sayfa == SAYFA_SAYISI - 1
                Button(
                    onClick = { if (son) onBitti() else sayfa++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A1F54)),
                ) {
                    Text(if (son) t("Başla") else t("İleri"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
      }
    }
}

@Composable
private fun Baslik(simge: String, baslik: String, aciklama: String) {
    Text(simge, fontSize = 44.sp)
    Text(baslik, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text(aciklama, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
}

@Composable
private fun HosGeldiniz() {
    Baslik(
        "🕌",
        t("Ezan Vakti'ne hoş geldiniz"),
        t("Birkaç adımda uygulamayı kendinize göre ayarlayalım. Burada seçtiğiniz her şeyi daha sonra Ayarlar'dan değiştirebilirsiniz."),
    )
    Ipucu("📍", t("Konumunuzu seçin"))
    Ipucu("🎨", t("Arka planı ve vakitlerin dizilişini belirleyin"))
    Ipucu("🔔", t("Bildirimleri açın"))
    Ipucu("🧩", t("Ana ekrana widget ekleyin"))
}

@Composable
private fun Ipucu(simge: String, metin: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(simge, fontSize = 20.sp)
        Text(metin, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun KonumAdimi(vm: PrayerViewModel, state: UiState, onListedenSec: () -> Unit) {
    val context = LocalContext.current
    val izinIste = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { verildi ->
        if (verildi) vm.konumuBul() else onListedenSec()
    }
    Baslik(
        "📍",
        t("Konumunuz"),
        t("Vakitler bulunduğunuz yere göre gösterilir. Diyanet'in Türkiye il/ilçe ve dünya şehirleri listesinden seçilir."),
    )
    Secenek("📡", t("Konumumu bul"), t("Telefonun konumuyla en yakın ilçe ya da şehir")) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            vm.konumuBul()
        } else {
            izinIste.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    Secenek("🔎", t("Listeden seç"), t("Ülke, il ve ilçeyi kendiniz seçin"), onListedenSec)

    when {
        state.phase is Phase.Locating -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Text(t("Konum aranıyor…"), modifier = Modifier.padding(start = 12.dp))
        }
        state.location != null -> Durum("✓ " + t("Seçili konum: %s", state.location.title))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GorunumAdimi(vm: PrayerViewModel, state: UiState) {
    Baslik(
        "🎨",
        t("Görünüm"),
        t("Arka plan rengini ve vakitlerin ekranda nasıl dizileceğini seçin."),
    )
    Text(t("Arka plan"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArkaPlan.entries.forEach { tema ->
            val (u, a) = arkaPlanRenkleri(tema, Vakit.OGLE)
            FilterChip(
                selected = state.arkaPlan == tema.kod,
                onClick = { vm.setArkaPlan(tema.kod) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (tema == ArkaPlan.VAKTE_GORE) {
                                    Brush.sweepGradient(listOf(Color(0xFF1A1F54), Color(0xFFF28C59), Color(0xFF2980D9), Color(0xFFE65940), Color(0xFF1A1F54)))
                                } else Brush.verticalGradient(listOf(u, a))
                            ),
                    )
                },
                label = { Text(t(tema.ad)) },
                colors = beyazChip(),
            )
        }
    }
    if (state.arkaPlan == ArkaPlan.VAKTE_GORE.kod) {
        Text(t("\"Vakte göre\" seçiliyken renk gün içinde vakitle birlikte değişir."), fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
    }

    Text(t("Vakitlerin dizilişi"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VakitDuzeni.entries.forEach { d ->
            FilterChip(
                selected = state.vakitDuzeni == d.kod,
                onClick = { vm.setVakitDuzeni(d.kod) },
                label = { Text(t(d.ad)) },
                colors = beyazChip(),
            )
        }
    }
    DuzenOnizleme(VakitDuzeni.from(state.vakitDuzeni))
}

@Composable
private fun beyazChip() = FilterChipDefaults.filterChipColors(
    labelColor = Color.White,
    selectedContainerColor = Color.White,
    selectedLabelColor = Color(0xFF1A1F54),
)

/** Seçilen dizilişin küçük bir önizlemesi (6 vakit). */
@Composable
private fun DuzenOnizleme(duzen: VakitDuzeni) {
    val vakitler = Vakit.entries
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        vakitler.chunked(duzen.sutun).forEach { satir ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                satir.forEach { v ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(v.adi, fontSize = 13.sp, maxLines = 1)
                    }
                }
                repeat(duzen.sutun - satir.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BildirimAdimi() {
    val context = LocalContext.current
    fun bildirimIzni() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var izinVar by remember { mutableStateOf(bildirimIzni()) }
    var tamZamanli by remember { mutableStateOf(NotificationScheduler.canScheduleExact(context)) }
    val izinIste = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { izinVar = it }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        izinVar = bildirimIzni()
        tamZamanli = NotificationScheduler.canScheduleExact(context)
    }

    Baslik(
        "🔔",
        t("Bildirimler"),
        t("Her vakit girdiğinde size haber verelim. Vakitten önce hatırlatma, Cuma ve kandil bildirimlerini Ayarlar'dan açıp kapatabilirsiniz."),
    )
    if (izinVar) {
        Durum("✓ " + t("Bildirim izni verildi"))
    } else {
        Secenek("🔔", t("Bildirimlere izin ver"), t("Vakit bildirimleri için gerekli")) {
            izinIste.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (tamZamanli) {
            Durum("✓ " + t("Bildirimler tam vaktinde gelecek"))
        } else {
            Secenek("⏰", t("Tam vaktinde bildirim"), t("\"Alarm ve hatırlatıcılar\" izni olmadan bildirimler birkaç dakika gecikebilir")) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                )
            }
        }
    }
}

@Composable
private fun WidgetAdimi() {
    val context = LocalContext.current
    val yonetici = remember { context.getSystemService(AppWidgetManager::class.java) }
    val eklenebilir = remember { yonetici?.isRequestPinAppWidgetSupported == true }
    Baslik(
        "🧩",
        t("Ana ekran widget'ları"),
        t("Uygulamayı açmadan sıradaki vakti ve kalan süreyi ana ekranınızda görün."),
    )
    if (eklenebilir) {
        val ekle = { sinif: Class<*> -> yonetici?.requestPinAppWidget(ComponentName(context, sinif), null, null); Unit }
        Secenek("🧩", t("Büyük widget"), t("Sıradaki vakit, kalan süre ve günün vakitleri")) { ekle(VakitWidget::class.java) }
        Secenek("🔹", t("Küçük widget"), t("Yalnızca sıradaki vakit ve geri sayım")) { ekle(KucukWidget::class.java) }
        Secenek("➖", t("Şerit widget"), t("Günün vakitleri yan yana, tek satırda")) { ekle(SeritWidget::class.java) }
        Text(t("Telefonunuz eklemeden önce onay isteyebilir."), fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
    } else {
        Text(
            t("Ana ekranda boş bir yere basılı tutun, \"Widget'lar\"ı seçin ve Ezan Vakti'ni bulun."),
            fontSize = 15.sp,
        )
    }
    Text(
        t("Bu turu daha sonra Diğer › \"Tanıtım turunu tekrar göster\" ile yeniden açabilirsiniz."),
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.75f),
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Secenek(simge: String, baslik: String, alt: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(simge, fontSize = 24.sp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(baslik, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(alt, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
        }
        Text("›", fontSize = 22.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun Durum(metin: String) {
    Text(
        metin,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x3329C76F))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

