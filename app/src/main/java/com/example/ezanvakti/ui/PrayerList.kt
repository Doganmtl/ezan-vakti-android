package com.example.ezanvakti.ui

import androidx.compose.foundation.background
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

/** Günün altı vakti; geçmiş vakitler soluk, içinde bulunulan vakit vurgulu. */
@Composable
fun PrayerList(today: DailyTimes, durum: VakitDurumu, now: Long, large: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp)
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
                    .padding(horizontal = 14.dp, vertical = if (large) 18.dp else 13.dp)
                    .alpha(if (isPast) 0.55f else 1f)
                    .clearAndSetSemantics {
                        contentDescription = "${vakit.ad} ${VakitCalculator.clock(time)}" +
                            if (isCurrent) ", şu anki vakit" else ""
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(vakit.emoji, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
                Text(
                    vakit.ad,
                    fontSize = if (large) 20.sp else 17.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    VakitCalculator.clock(time),
                    fontSize = if (large) 24.sp else 20.sp,
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
