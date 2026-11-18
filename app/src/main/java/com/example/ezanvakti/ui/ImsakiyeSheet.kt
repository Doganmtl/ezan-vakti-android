package com.example.ezanvakti.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.RamazanHesabi
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val kisaTarih = DateTimeFormatter.ofPattern("d MMM EEE", VakitCalculator.tr)

/** Ramazan imsakiyesi: seçili ilçenin elimizdeki günleri için imsak (sahur sonu) ve iftar saatleri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImsakiyeSheet(
    yer: String,
    days: List<DailyTimes>,
    dini: DiniGunlerVerisi,
    bugun: LocalDate,
    onizlemeBaslangic: LocalDate?,
    onDismiss: () -> Unit,
) {
    val satirlar = days.sortedBy { it.epochDay }.mapNotNull { d ->
        val tarih = LocalDate.ofEpochDay(d.epochDay)
        RamazanHesabi.bilgi(dini, tarih, d.hijri, onizlemeBaslangic)?.let { Triple(it.gun, tarih, d) }
    }
    val donem = dini.ramazan(bugun) ?: dini.sonrakiRamazan(bugun)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 16.dp)) {
            Text("İmsakiye", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                yer + (donem?.let { " · ${it.hicriYil} Ramazanı (${it.start.uzun()} – ${it.end.uzun()})" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (satirlar.isEmpty()) {
                Text(
                    "Ramazan günlerinin vakitleri, Diyanet her ayın vakitlerini yayımladıkça burada görünür " +
                        "(genellikle Ramazan'a yaklaşık bir ay kala). Ramazan başlangıcı ve bayram tarihleri Takvim sekmesinde.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Baslik("Gün", Modifier.width(44.dp))
                Baslik("Tarih", Modifier.weight(1f))
                Baslik("İmsak", Modifier.width(70.dp))
                Baslik("İftar", Modifier.width(70.dp))
            }
            HorizontalDivider()
            LazyColumn {
                items(satirlar, key = { it.second.toEpochDay() }) { (gun, tarih, d) ->
                    val bugunMu = tarih == bugun
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (bugunMu) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$gun", fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                        Text(tarih.format(kisaTarih) + if (bugunMu) " · bugün" else "", modifier = Modifier.weight(1f))
                        Text(VakitCalculator.clock(d.time(Vakit.IMSAK)), style = TextStyle(fontFeatureSettings = "tnum"), modifier = Modifier.width(70.dp))
                        Text(VakitCalculator.clock(d.time(Vakit.AKSAM)), fontWeight = FontWeight.SemiBold, style = TextStyle(fontFeatureSettings = "tnum"), modifier = Modifier.width(70.dp))
                    }
                    HorizontalDivider()
                }
                item {
                    Text(
                        "İmsak: sahurun son anı, orucun başladığı vakit. İftar: akşam ezanı. Vakitler Diyanet verisidir; " +
                            "ilerideki günler veri yayımlandıkça eklenir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Baslik(text: String, modifier: Modifier) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}
