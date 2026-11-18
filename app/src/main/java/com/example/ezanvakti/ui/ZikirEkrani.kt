package com.example.ezanvakti.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ezanvakti.model.Tesbihat
import com.example.ezanvakti.model.ZikirModu
import com.example.ezanvakti.model.hazirHedefler
import com.example.ezanvakti.model.hazirZikirler

private val Kart = Color.White.copy(alpha = 0.12f)

@Composable
fun ZikirEkrani(vm: ZikirViewModel) {
    val d by vm.durum.collectAsStateWithLifecycle()

    // Ekran açık kalsın; ses tuşları zikir saysın (ayar açıksa).
    val view = LocalView.current
    val sayFn by rememberUpdatedState(vm::say)
    DisposableEffect(d.sesTusu) {
        view.keepScreenOn = true
        ZikirTuslari.dinleyici = if (d.sesTusu) ({ sayFn() }) else null
        onDispose {
            view.keepScreenOn = false
            ZikirTuslari.dinleyici = null
        }
    }

    var sifirlaSor by remember { mutableStateOf(false) }
    var hedefSor by remember { mutableStateOf(false) }
    var adSor by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Mod seçimi
            Row(
                Modifier.fillMaxWidth().background(Kart, RoundedCornerShape(50)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModSecenek("Serbest zikir", d.mod == ZikirModu.SERBEST, Modifier.weight(1f)) { vm.setMod(ZikirModu.SERBEST) }
                ModSecenek("Namaz tesbihatı", d.mod == ZikirModu.TESBIHAT, Modifier.weight(1f)) { vm.setMod(ZikirModu.TESBIHAT) }
            }

            // Zikir adı
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.gosterilenAd + if (d.mod == ZikirModu.SERBEST) " ▾" else "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = d.mod == ZikirModu.SERBEST) { adSor = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                if (d.mod == ZikirModu.TESBIHAT) {
                    val adim = Tesbihat.adimlar.getOrNull(d.tesbihatAdim)
                    Text(
                        adim?.anlam ?: "Allah kabul etsin.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Sayma alanı
            SayacHalkasi(d, onSay = vm::say)

            if (d.mod == ZikirModu.TESBIHAT) {
                TesbihatAdimlari(d.tesbihatAdim)
            }

            // Kontroller
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                KucukDugme("↶ Geri al", enabled = d.sayac > 0, onClick = vm::geriAl)
                KucukDugme("⟲ Sıfırla", enabled = d.sayac > 0 || d.tesbihatAdim > 0) { sifirlaSor = true }
            }

            if (d.mod == ZikirModu.SERBEST) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hedef", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hazirHedefler.forEach { h ->
                            Cip(if (h == 0) "Hedefsiz" else "$h", d.hedef == h, Modifier.weight(1f)) { vm.setHedef(h) }
                        }
                        val ozel = d.hedef !in hazirHedefler
                        Cip(if (ozel) "${d.hedef}" else "Özel", ozel, Modifier.weight(1f)) { hedefSor = true }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().background(Kart, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                AyarSatir("Titreşim", d.titresim, vm::setTitresim)
                AyarSatir("Ses tuşlarıyla say", d.sesTusu, vm::setSesTusu)
                Text(
                    "Toplam zikir: ${"%,d".format(d.toplam).replace(',', '.')}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Text(
                "Sayım her dokunuşta kaydedilir; uygulamayı kapatsanız da kaldığınız yerden devam edersiniz.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (sifirlaSor) {
        AlertDialog(
            onDismissRequest = { sifirlaSor = false },
            title = { Text("Sayaç sıfırlansın mı?") },
            text = {
                Text(
                    if (d.mod == ZikirModu.TESBIHAT) "Tesbihat baştan başlar."
                    else "${d.gosterilenAd}: ${d.sayac} sayım silinir. Toplam zikir sayınız değişmez."
                )
            },
            confirmButton = { TextButton(onClick = { vm.sifirla(); sifirlaSor = false }) { Text("Sıfırla") } },
            dismissButton = { TextButton(onClick = { sifirlaSor = false }) { Text("Vazgeç") } },
        )
    }
    if (hedefSor) {
        SayiDialog(
            baslik = "Özel hedef",
            ilk = if (d.hedef > 0) d.hedef else 33,
            onTamam = { vm.setHedef(it); hedefSor = false },
            onKapat = { hedefSor = false },
        )
    }
    if (adSor) {
        ZikirAdiDialog(
            mevcut = d.ad,
            sayac = d.sayac,
            onSec = { vm.zikreBasla(it); adSor = false },
            onKapat = { adSor = false },
        )
    }
}

@Composable
private fun SayacHalkasi(d: ZikirDurumu, onSay: () -> Unit) {
    val renk = RamazanRengi
    Box(
        Modifier
            .fillMaxWidth(0.82f)
            .widthIn(max = 340.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(enabled = !d.tesbihatBitti, onClick = onSay)
            .semantics { contentDescription = "Zikir saymak için dokunun. ${d.gosterilenAd}, ${d.sayac}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (d.gecerliHedef > 0) {
                drawArc(renk, -90f, 360f * d.ilerleme, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
            if (d.tesbihatBitti) {
                Text("✓", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = renk)
                Text("Tesbihat tamamlandı", fontSize = 16.sp)
            } else {
                Text("${d.sayac}", fontSize = 76.sp, fontWeight = FontWeight.Bold)
                val alt = when {
                    d.mod == ZikirModu.TESBIHAT -> "/ ${d.gecerliHedef}"
                    d.hedef > 0 && d.tur > 1 -> "/ ${d.hedef} · ${d.tur}. tur"
                    d.hedef > 0 -> "/ ${d.hedef}"
                    else -> "hedefsiz"
                }
                Text(alt, fontSize = 16.sp, color = Color.White.copy(alpha = 0.75f))
                Text("Dokunun", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun TesbihatAdimlari(adim: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tesbihat.adimlar.forEachIndexed { i, a ->
            val bitti = i < adim
            val simdi = i == adim
            Column(
                Modifier
                    .weight(1f)
                    .background(
                        when {
                            simdi -> Color.White.copy(alpha = 0.25f)
                            bitti -> RamazanRengi.copy(alpha = 0.25f)
                            else -> Kart
                        },
                        RoundedCornerShape(14.dp),
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (bitti) "✓ 33" else "33", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(a.ad, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ModSecenek(metin: String, secili: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        metin,
        fontSize = 14.sp,
        fontWeight = if (secili) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        color = if (secili) Color(0xFF1F2959) else Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (secili) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
fun Cip(metin: String, secili: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        metin,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        color = if (secili) Color(0xFF1F2959) else Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (secili) Color.White else Kart)
            .border(1.dp, Color.White.copy(alpha = if (secili) 0f else 0.3f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
private fun KucukDugme(metin: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        metin,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Kart)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun AyarSatir(baslik: String, acik: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(baslik, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = acik, onCheckedChange = onChange)
    }
}

/** Pozitif tam sayı soran küçük pencere. */
@Composable
fun SayiDialog(baslik: String, ilk: Int, aciklama: String? = null, onTamam: (Int) -> Unit, onKapat: () -> Unit) {
    var metin by rememberSaveable { mutableStateOf(ilk.toString()) }
    val sayi = metin.toIntOrNull()
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text(baslik) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aciklama?.let { Text(it) }
                OutlinedTextField(
                    value = metin,
                    onValueChange = { yeni -> metin = yeni.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { sayi?.let(onTamam) }, enabled = sayi != null) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç") } },
    )
}

@Composable
private fun ZikirAdiDialog(mevcut: String, sayac: Int, onSec: (String) -> Unit, onKapat: () -> Unit) {
    var ozel by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text("Zikir seçin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sayac > 0) Text("Yeni zikir sıfırdan başlar ($mevcut: $sayac sayım silinir).", fontSize = 13.sp)
                hazirZikirler.forEach { z ->
                    Text(
                        (if (z == mevcut) "● " else "") + z,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSec(z) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = ozel,
                    onValueChange = { ozel = it.take(40) },
                    singleLine = true,
                    label = { Text("Başka bir zikir yazın") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSec(ozel.trim()) }, enabled = ozel.isNotBlank()) { Text("Başla") }
        },
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç") } },
    )
}
