package com.example.ezanvakti.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ezanvakti.MainActivity
import com.example.ezanvakti.R
import com.example.ezanvakti.model.GeriSayim

/**
 * Kilit ekranında ve bildirim alanında duran sessiz geri sayım kartı ("İkindi vaktine 1:12:40").
 * Sayacı sistem yürütür; kart her vakit değişiminde [Tazeleyici] tarafından yenilenir.
 */
object CanliSayac {
    private const val KANAL = "canli_sayac"
    private const val BILDIRIM_ID = 900

    private fun kanal(context: Context) {
        val ch = NotificationChannel(KANAL, "Canlı geri sayım", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Kilit ekranında sıradaki vakte kalan süre"
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun goster(context: Context, sayim: GeriSayim, yer: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        kanal(context)
        val ac = PendingIntent.getActivity(
            context, 902, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val alt = buildString {
            append(sayim.alt).append(" · ").append(yer)
            sayim.ramazanGunu?.let { append(" · Ramazan'ın $it. günü") }
        }
        val n = NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_stat_vakit)
            .setContentTitle(sayim.baslik)
            .setContentText(alt)
            .setWhen(sayim.hedef)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(ac)
            .build()
        NotificationManagerCompat.from(context).notify(BILDIRIM_ID, n)
    }

    fun kaldir(context: Context) {
        NotificationManagerCompat.from(context).cancel(BILDIRIM_ID)
    }
}
