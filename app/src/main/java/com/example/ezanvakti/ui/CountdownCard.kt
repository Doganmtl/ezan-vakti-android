package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.model.VakitDurumu

/** Ana kart: şu anki vakit, "vakit girdi" uyarısı ve sıradaki vakte geri sayım. */
@Composable
fun CountdownCard(durum: VakitDurumu, now: Long, large: Boolean, modifier: Modifier = Modifier) {
    val remaining = durum.remaining(now)
    // Vakit girdikten sonraki ilk 15 dakika "ezan vakti" olarak vurgulanır.
    val justEntered = durum.current != Vakit.GUNES && durum.elapsed(now) < 15 * 60_000L
    val headline = durum.current.namazAdi?.let { t("%s vakti", t(it)) } ?: t("Kuşluk vakti")
    val countdownLabel = durum.current.namazAdi?.let { t("%snı kaçırmamak için kalan süre", t(it)) }
        ?: t("%s vaktine kalan süre", durum.next.adi)
    val progress by animateFloatAsState(durum.progress(now), tween(1000), label = "progress")

    val ringSize = if (large) 290.dp else 205.dp
    val stroke = if (large) 12.dp else 8.dp

    Column(
        modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .padding(if (large) 32.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (large) 18.dp else 10.dp),
    ) {
        AnimatedVisibility(justEntered, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Text(
                t("🔔 %s vakti girdi", durum.current.adi),
                fontSize = if (large) 18.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(durum.current.emoji, fontSize = if (large) 48.sp else 32.sp)
            Text(headline, fontSize = if (large) 28.sp else 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Box(
            Modifier
                .size(ringSize)
                .clearAndSetSemantics {
                    contentDescription = t("%s: %s", countdownLabel, VakitCalculator.spoken(remaining))
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = stroke.toPx()
                val arcSize = Size(size.width - w, size.height - w)
                val topLeft = Offset(w / 2, w / 2)
                drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
                drawArc(Color.White, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    VakitCalculator.countdown(remaining),
                    fontSize = if (large) 50.sp else 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum"),   // sabit genişlikli rakamlar, sayı titremesin
                    maxLines = 1,
                )
                Text(
                    t("%s · %s", durum.next.adi, VakitCalculator.clock(durum.nextTime)),
                    fontSize = if (large) 18.sp else 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Text(
            countdownLabel,
            fontSize = if (large) 18.sp else 15.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}
