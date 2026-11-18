package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.HicriTakvim

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.RamazanHesabi
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.model.trTitle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Ay başlığı ve Pazartesi'den başlayan gün kısaltmaları, etkin dilde. */
private val ayBasligi: DateTimeFormatter get() = DateTimeFormatter.ofPattern("LLLL yyyy", Dil.locale)
private val GUN_KISALTMALARI: List<String>
    get() = if (Dil.turkce) listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    else java.time.DayOfWeek.values().map { it.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, Dil.locale) }

/**
 * Dini günler takvimi: aylık görünüm (kandil, bayram, arefe, Ramazan işaretli),
 * seçili günün ayrıntısı, yaklaşan dini günler ve Ramazan/imsakiye girişi.
 */
@Composable
fun TakvimEkrani(
    dini: DiniGunlerVerisi,
    days: List<DailyTimes>,
    bugun: LocalDate,
    onizlemeBaslangic: LocalDate?,
    wide: Boolean,
    onImsakiye: () -> Unit,
) {
    var secili by rememberSaveable { mutableLongStateOf(bugun.toEpochDay()) }
    var ay by rememberSaveable { mutableLongStateOf(YearMonth.from(bugun).let { it.year * 12L + it.monthValue - 1 }) }
    val seciliTarih = LocalDate.ofEpochDay(secili)
    val gorunenAy = YearMonth.of((ay / 12).toInt(), (ay % 12).toInt() + 1)

    val sec: (LocalDate) -> Unit = { d ->
        secili = d.toEpochDay()
        ay = d.year * 12L + d.monthValue - 1
    }
    val ramazanMi: (LocalDate) -> Boolean = { d ->
        RamazanHesabi.bilgi(dini, d, days.firstOrNull { it.epochDay == d.toEpochDay() }?.hijri, onizlemeBaslangic) != null
    }

    val takvim: @Composable (Modifier) -> Unit = { mod ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AyTakvimi(
                ay = gorunenAy,
                bugun = bugun,
                secili = seciliTarih,
                dini = dini,
                ramazanMi = ramazanMi,
                onOnceki = { ay -= 1 },
                onSonraki = { ay += 1 },
                onBugun = { sec(bugun) },
                onSec = sec,
            )
            GunAyrintisi(seciliTarih, bugun, dini, days, ramazanMi(seciliTarih))
        }
    }
    val liste: @Composable (Modifier) -> Unit = { mod ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RamazanGirisi(dini, bugun, onizlemeBaslangic, onImsakiye)
            YaklasanGunler(dini, bugun, onSec = sec)
            Text(
                t("Kaynak: %s.", t(dini.kaynak ?: "Diyanet İşleri Başkanlığı")) +
                    (dini.gecerliOlduguSon?.let { t(" Takvim %s tarihine kadar.", LocalDate.parse(it).uzun()) } ?: ""),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            takvim(Modifier.weight(1f).verticalScroll(rememberScrollState()))
            liste(Modifier.width(400.dp).verticalScroll(rememberScrollState()))
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            takvim(Modifier.fillMaxWidth())
            liste(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AyTakvimi(
    ay: YearMonth,
    bugun: LocalDate,
    secili: LocalDate,
    dini: DiniGunlerVerisi,
    ramazanMi: (LocalDate) -> Boolean,
    onOnceki: () -> Unit,
    onSonraki: () -> Unit,
    onBugun: () -> Unit,
    onSec: (LocalDate) -> Unit,
) {
    val bugundeyiz = secili == bugun && YearMonth.from(bugun) == ay
    Column(
        Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOnceki) { Text("‹", fontSize = 24.sp, color = Color.White) }
            Text(
                ay.format(ayBasligi).replaceFirstChar { it.titlecase(Dil.locale) },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSonraki) { Text("›", fontSize = 24.sp, color = Color.White) }
            Text(
                t("Bugün"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = if (bugundeyiz) 0.45f else 1f),
                modifier = Modifier
                    // Kenarlık, kırpmadan ÖNCE çizilir; aksi halde 1 dp çizginin dış yarısı
                    // kırpılıp üst kenar silik görünüyordu.
                    .padding(1.dp)
                    .border(1.5.dp, Color.White.copy(alpha = if (bugundeyiz) 0.5f else 0.9f), RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !bugundeyiz, onClick = onBugun)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            GUN_KISALTMALARI.forEach {
                Text(it, fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            }
        }
        val bosluk = ay.atDay(1).dayOfWeek.value - 1   // Pazartesi = 0
        val hucre = bosluk + ay.lengthOfMonth()
        val satir = (hucre + 6) / 7
        for (r in 0 until satir) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val gunNo = r * 7 + c - bosluk + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (gunNo in 1..ay.lengthOfMonth()) {
                            val tarih = ay.atDay(gunNo)
                            GunHucresi(tarih, tarih == bugun, tarih == secili, dini, ramazanMi(tarih), onSec)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Lejant(com.example.ezanvakti.model.DiniGunTuru.KANDIL.renk, t("Kandil"))
            Lejant(com.example.ezanvakti.model.DiniGunTuru.BAYRAM.renk, t("Bayram"))
            Lejant(com.example.ezanvakti.model.DiniGunTuru.AREFE.renk, t("Arefe"))
            Lejant(RamazanRengi, t("Ramazan"))
        }
    }
}

@Composable
private fun Lejant(renk: Color, ad: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(renk, CircleShape))
        Text(t(" %s", ad), fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun GunHucresi(
    tarih: LocalDate,
    bugunMu: Boolean,
    seciliMi: Boolean,
    dini: DiniGunlerVerisi,
    ramazan: Boolean,
    onSec: (LocalDate) -> Unit,
) {
    val olaylar = dini.on(tarih)
    val aciklama = buildString {
        append(tarih.uzun())
        if (bugunMu) append(t(", bugün"))
        if (ramazan) append(t(", Ramazan"))
        olaylar.forEach { append(", ").append(it.adi) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    seciliMi -> Color.White.copy(alpha = 0.92f)
                    ramazan -> RamazanRengi.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .then(if (bugunMu && !seciliMi) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
            .clickable { onSec(tarih) }
            .clearAndSetSemantics { contentDescription = aciklama },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            tarih.dayOfMonth.toString(),
            fontSize = 15.sp,
            color = if (seciliMi) Color(0xFF13234A) else Color.White,
            fontWeight = if (bugunMu || olaylar.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            olaylar.map { it.turu }.distinct().take(3).forEach {
                Box(Modifier.size(6.dp).background(it.renk, CircleShape))
            }
        }
    }
}

@Composable
private fun GunAyrintisi(tarih: LocalDate, bugun: LocalDate, dini: DiniGunlerVerisi, days: List<DailyTimes>, ramazan: Boolean) {
    val olaylar = dini.on(tarih)
    val hicri = HicriTakvim.goster(days.firstOrNull { it.epochDay == tarih.toEpochDay() }?.hijri ?: olaylar.firstOrNull()?.hicri)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(tarih.uzun(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        val alt = listOfNotNull(hicri, kalanGunMetni(bugun, tarih).takeIf { tarih != bugun })
        if (alt.isNotEmpty()) Text(alt.joinToString(" · "), fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
        if (ramazan && olaylar.none { it.turu == com.example.ezanvakti.model.DiniGunTuru.RAMAZAN }) {
            Text(t("🌙 Ramazan (oruç günü)"), fontSize = 15.sp)
        }
        olaylar.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(g.turu.renk, CircleShape))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(if (g.turu == com.example.ezanvakti.model.DiniGunTuru.KANDIL) t("%s (akşamı)", g.adi) else g.adi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (g.turu == com.example.ezanvakti.model.DiniGunTuru.KANDIL || g.turu == com.example.ezanvakti.model.DiniGunTuru.BAYRAM) {
                        Text(g.tebrik, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }
        }
        if (olaylar.isEmpty() && !ramazan) {
            Text(t("Bu tarihte dini gün yok."), fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun YaklasanGunler(dini: DiniGunlerVerisi, bugun: LocalDate, onSec: (LocalDate) -> Unit) {
    val liste = dini.upcoming(bugun, 10)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp),
    ) {
        Text(t("Yaklaşan Dini Günler"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (liste.isEmpty()) {
            Text(t("Takvimde ileri tarihli kayıt yok."), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
        liste.forEach { g ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSec(g.date) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(g.turu.renk, CircleShape))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(g.adi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(g.date.uzun(), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Text(kalanGunMetni(bugun, g.date), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RamazanGirisi(dini: DiniGunlerVerisi, bugun: LocalDate, onizlemeBaslangic: LocalDate?, onImsakiye: () -> Unit) {
    val donem = dini.ramazan(bugun) ?: dini.sonrakiRamazan(bugun)
    val metin = when {
        onizlemeBaslangic != null -> t("Ramazan önizlemesi açık (geliştirici).")
        donem == null -> return
        donem.contains(bugun) -> t("Ramazan'ın %s. günü · %s son oruç.", donem.gun(bugun), donem.end.uzun())
        else -> t("%s Ramazanı %s başlıyor · %s gün kaldı. İlk sahur önceki gece.", donem.hicriYil, donem.start.uzun(), ChronoUnit.DAYS.between(bugun, donem.start))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RamazanRengi.copy(alpha = 0.18f))
            .clickable(onClick = onImsakiye)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🌙", fontSize = 24.sp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(t("Ramazan ve İmsakiye"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(metin, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        }
        Text("›", fontSize = 22.sp)
    }
}
