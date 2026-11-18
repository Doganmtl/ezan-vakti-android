package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.model.VakitDurumu
import com.example.ezanvakti.ui.theme.VakitDuzeni

/** Günün altı vakti; geçmiş vakitler soluk, içinde bulunulan vakit vurgulu. [duzen] alt alta ya da 2/3 sütunlu ızgara. */
@Composable
fun PrayerList(
    today: DailyTimes,
    durum: VakitDurumu,
    now: Long,
    large: Boolean,
    modifier: Modifier = Modifier,
    duzen: VakitDuzeni = VakitDuzeni.LISTE,
) {
    if (duzen != VakitDuzeni.LISTE) {
        VakitIzgarasi(today, durum, now, large, duzen.sutun, modifier)
        return
    }
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp)
    ) {
        Vakit.entries.forEach { vakit ->
            val time = today.time(vakit)
            // Gece yarısından sonra "yatsı" önceki güne aittir; bugünün yatsısı vurgulanmaz.
            val isCurrent = vakit == durum.current &&
                VakitCalculator.epochDay(durum.currentStart) == today.epochDay
            val isPast = time <= now && !isCurrent

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .background(
                        if (isCurrent) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = if (large) 14.dp else 10.dp)
                    .alpha(if (isPast) 0.55f else 1f)
                    .clearAndSetSemantics {
                        contentDescription = t("%s %s", vakit.adi, VakitCalculator.clock(time)) +
                            if (isCurrent) t(", şu anki vakit") else ""
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(vakit.emoji, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                Text(
                    vakit.adi,
                    fontSize = if (large) 18.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    VakitCalculator.clock(time),
                    fontSize = if (large) 21.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
            if (vakit != Vakit.YATSI) {
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.12f))
            }
        }
    }
}

/** Vakitler yan yana kutucuklarda: 2 sütun × 3 satır ya da 3 sütun × 2 satır. */
@Composable
private fun VakitIzgarasi(today: DailyTimes, durum: VakitDurumu, now: Long, large: Boolean, sutun: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Vakit.entries.chunked(sutun).forEach { satir ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                satir.forEach { vakit ->
                    val time = today.time(vakit)
                    val isCurrent = vakit == durum.current &&
                        VakitCalculator.epochDay(durum.currentStart) == today.epochDay
                    val isPast = time <= now && !isCurrent
                    Column(
                        Modifier
                            .weight(1f)
                            .background(
                                Color.White.copy(alpha = if (isCurrent) 0.30f else 0.14f),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(vertical = if (large) 14.dp else 10.dp, horizontal = 6.dp)
                            .alpha(if (isPast) 0.55f else 1f)
                            .clearAndSetSemantics {
                                contentDescription = t("%s %s", vakit.adi, VakitCalculator.clock(time)) +
                                    if (isCurrent) t(", şu anki vakit") else ""
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            vakit.emoji + " " + vakit.adi,
                            fontSize = if (sutun == 3) 13.sp else 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            VakitCalculator.clock(time),
                            fontSize = if (sutun == 3) 19.sp else 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
