package com.example.ezanvakti.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.data.EkstraDepo

/** Kaza kalemleri: anahtar (kayıt adı), görünen ad, açıklama. */
private data class KazaKalemi(val anahtar: String, val ad: String, val aciklama: String)

private val namazKalemleri = listOf(
    KazaKalemi("sabah", "Sabah", "2 rekât farz"),
    KazaKalemi("ogle", "Öğle", "4 rekât farz"),
    KazaKalemi("ikindi", "İkindi", "4 rekât farz"),
    KazaKalemi("aksam", "Akşam", "3 rekât farz"),
    KazaKalemi("yatsi", "Yatsı", "4 rekât farz"),
    KazaKalemi("vitir", "Vitir", "3 rekât vacip"),
)
private val orucKalemi = KazaKalemi("oruc", "Oruç", "Ramazan orucu kazası")

@Composable
fun KazaEkrani(onGeri: () -> Unit) {
    val context = LocalContext.current
    val depo = remember { EkstraDepo(context) }
    val degerler = remember {
        mutableStateMapOf<String, Int>().apply { (namazKalemleri + orucKalemi).forEach { put(it.anahtar, depo.kaza(it.anahtar)) } }
    }
    var duzenlenen by remember { mutableStateOf<KazaKalemi?>(null) }

    fun ayarla(k: KazaKalemi, yeni: Int) {
        val v = yeni.coerceAtLeast(0)
        degerler[k.anahtar] = v
        depo.setKaza(k.anahtar, v)
    }

    val toplamNamaz = namazKalemleri.sumOf { degerler[it.anahtar] ?: 0 }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AltSayfaBasligi("Kaza takibi", onGeri)
            Text(
                "Kılamadığınız namaz ve tutamadığınız oruçları ekleyin; kaza ettikçe − ile düşürün. " +
                    "Sayıya dokunarak toplu değer girebilirsiniz.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kalan kaza namazı", fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("$toplamNamaz", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Text("Namaz", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 6.dp))
            namazKalemleri.forEach { k ->
                KazaSatiri(k, degerler[k.anahtar] ?: 0, onDegis = { ayarla(k, it) }, onDuzenle = { duzenlenen = k })
            }
            Text("Oruç", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 6.dp))
            KazaSatiri(orucKalemi, degerler[orucKalemi.anahtar] ?: 0, onDegis = { ayarla(orucKalemi, it) }, onDuzenle = { duzenlenen = orucKalemi })

            Text(
                "Kayıtlar yalnızca bu telefonda tutulur.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    duzenlenen?.let { k ->
        SayiDialog(
            baslik = "${k.ad} kazası",
            ilk = degerler[k.anahtar] ?: 0,
            aciklama = "Kalan ${k.ad.lowercase(com.example.ezanvakti.model.VakitCalculator.tr)} kaza sayısını girin.",
            onTamam = { ayarla(k, it); duzenlenen = null },
            onKapat = { duzenlenen = null },
        )
    }
}

@Composable
private fun KazaSatiri(k: KazaKalemi, deger: Int, onDegis: (Int) -> Unit, onDuzenle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(k.ad, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(k.aciklama, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        }
        YuvarlakDugme("−", "${k.ad} kazasını bir azalt", enabled = deger > 0) { onDegis(deger - 1) }
        Text(
            "$deger",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDuzenle)
                .semantics { contentDescription = "${k.ad}: $deger. Değiştirmek için dokunun." }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
        YuvarlakDugme("+", "${k.ad} kazasını bir artır") { onDegis(deger + 1) }
    }
}

@Composable
private fun YuvarlakDugme(simge: String, aciklama: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.22f else 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = aciklama },
        contentAlignment = Alignment.Center,
    ) {
        Text(simge, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f))
    }
}

/** Alt sayfaların "‹ Geri  Başlık" satırı. */
@Composable
fun AltSayfaBasligi(baslik: String, onGeri: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹ Geri",
            fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onGeri)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Text(baslik, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}
