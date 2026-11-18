package com.example.ezanvakti.ui

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
    val headline = durum.current.namazAdi?.let { "$it vakti" } ?: "Kuşluk vakti"
    val countdownLabel = durum.current.namazAdi?.let { "${it}nı kaçırmamak için kalan süre" }
        ?: "${durum.next.ad} vaktine kalan süre"
    val progress by animateFloatAsState(durum.progress(now), tween(1000), label = "progress")

    val ringSize = if (large) 340.dp else 250.dp
    val stroke = if (large) 14.dp else 10.dp

    Column(
        modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .padding(if (large) 40.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (large) 24.dp else 16.dp),
    ) {
        AnimatedVisibility(justEntered, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Text(
                "🔔 ${durum.current.ad} vakti girdi",
                fontSize = if (large) 18.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(durum.current.emoji, fontSize = if (large) 60.sp else 42.sp)
            Text(headline, fontSize = if (large) 34.sp else 24.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            Modifier
                .size(ringSize)
                .clearAndSetSemantics {
                    contentDescription = "$countdownLabel: ${VakitCalculator.spoken(remaining)}"
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
                    fontSize = if (large) 60.sp else 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum"),   // sabit genişlikli rakamlar, sayı titremesin
                    maxLines = 1,
                )
                Text(
                    "${durum.next.ad} · ${VakitCalculator.clock(durum.nextTime)}",
                    fontSize = if (large) 20.sp else 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Text(
            countdownLabel,
            fontSize = if (large) 20.sp else 16.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}
