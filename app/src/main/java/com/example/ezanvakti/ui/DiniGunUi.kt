package com.example.ezanvakti.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.model.DiniGun
import com.example.ezanvakti.model.DiniGunTuru
import com.example.ezanvakti.model.RamazanHesabi
import com.example.ezanvakti.model.VakitCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ---------- Ortak yardımcılar ----------

val DiniGunTuru.renk: Color
    get() = when (this) {
        DiniGunTuru.KANDIL -> Color(0xFFFFC857)
        DiniGunTuru.BAYRAM -> Color(0xFF6EE7A8)
        DiniGunTuru.AREFE -> Color(0xFFFFA45B)
        DiniGunTuru.RAMAZAN -> Color(0xFF5EEAD4)
        DiniGunTuru.GUN -> Color(0xFF93C5FD)
    }

val RamazanRengi = Color(0xFF5EEAD4)

private val uzunTarih = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", VakitCalculator.tr)

fun LocalDate.uzun(): String = format(uzunTarih)

/** "Bugün", "Yarın", "22 gün sonra" */
fun kalanGunMetni(bugun: LocalDate, tarih: LocalDate): String =
    when (val n = ChronoUnit.DAYS.between(bugun, tarih)) {
        0L -> "Bugün"
        1L -> "Yarın"
        else -> if (n > 0) "$n gün sonra" else "${-n} gün önce"
    }

// ---------- Ana ekran: bugünün dini günü ----------

@Composable
fun BugununDiniGunu(gunler: List<DiniGun>, large: Boolean, modifier: Modifier = Modifier) {
    if (gunler.isEmpty()) return
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        gunler.forEach { g ->
            val baslik = if (g.turu == DiniGunTuru.KANDIL) "Bu gece ${g.ad}" else g.ad
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.turu.emoji, fontSize = if (large) 26.sp else 22.sp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(baslik, fontWeight = FontWeight.Bold, fontSize = if (large) 20.sp else 17.sp)
                    if (g.turu == DiniGunTuru.KANDIL || g.turu == DiniGunTuru.BAYRAM) {
                        Text(g.tebrik, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

// ---------- Ana ekran: sıradaki dini gün ----------

@Composable
fun SiradakiDiniGun(gun: DiniGun, bugun: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                contentDescription = "Sıradaki dini gün: ${gun.ad}, ${gun.date.uzun()}, ${kalanGunMetni(bugun, gun.date)}. Takvimi açmak için dokunun."
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(gun.turu.renk, CircleShape))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Sıradaki: ${gun.ad}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(gun.date.uzun(), fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
        }
        Text(kalanGunMetni(bugun, gun.date), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text("  ›", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

// ---------- Ramazan kartı ----------

@Composable
fun RamazanKarti(
    durum: RamazanHesabi.Durum,
    now: Long,
    large: Boolean,
    onImsakiye: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kalan = (durum.hedefZamani - now).coerceAtLeast(0)
    val etiket = when {
        durum.ilkSahur -> "Bu gece ilk sahur · imsaka kalan"
        durum.hedef == RamazanHesabi.Hedef.SAHUR -> "Sahurun bitmesine (imsaka) kalan"
        else -> "İftara kalan"
    }
    Column(
        modifier
            .background(RamazanRengi.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
            .padding(if (large) 28.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌙 Ramazan-ı Şerif", fontWeight = FontWeight.Bold, fontSize = if (large) 22.sp else 18.sp, modifier = Modifier.weight(1f))
            Text(
                "${durum.gun.gun}. gün" + (durum.gun.toplam?.let { " / $it" } ?: ""),
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "$etiket: ${VakitCalculator.spoken(kalan)}" },
        ) {
            Text(etiket, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
            Text(
                VakitCalculator.countdown(kalan),
                fontSize = if (large) 44.sp else 36.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }

        Row(Modifier.fillMaxWidth()) {
            SaatKutusu("Sahur sonu (imsak)", VakitCalculator.clock(durum.sahur), durum.hedef == RamazanHesabi.Hedef.SAHUR, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            SaatKutusu("İftar (akşam)", VakitCalculator.clock(durum.iftar), durum.hedef == RamazanHesabi.Hedef.IFTAR, Modifier.weight(1f))
        }

        TextButton(onClick = onImsakiye, modifier = Modifier.align(Alignment.End)) {
            Text("İmsakiye ›", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SaatKutusu(baslik: String, saat: String, vurgulu: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = if (vurgulu) 0.24f else 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(baslik, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        Text(saat, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = TextStyle(fontFeatureSettings = "tnum"))
    }
}
