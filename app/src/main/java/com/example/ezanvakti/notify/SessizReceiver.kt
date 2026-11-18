package com.example.ezanvakti.notify

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Vakit

/**
 * Namaz vakti girince telefonu "Rahatsız Etmeyin" (öncelikli) moduna alır, süre dolunca geri açar.
 *  - Kullanıcının izni gerekir (Ayarlar › Rahatsız Etmeyin erişimi).
 *  - Telefon zaten sessizdeyse/Rahatsız Etmeyin açıksa dokunmaz.
 *  - Geri açarken yalnızca kendi açtığı sessizliği kapatır; kullanıcı arada kendisi değiştirdiyse ona dokunmaz.
 */
class SessizReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        val prefs = context.getSharedPreferences("ezan_sessiz", Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_SESSIZ -> {
                if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    prefs.edit().putBoolean(ANAHTAR_BIZ_ACTIK, true).apply()
                }
            }
            ACTION_GERI_AC -> {
                if (prefs.getBoolean(ANAHTAR_BIZ_ACTIK, false) &&
                    nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
                ) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                prefs.edit().putBoolean(ANAHTAR_BIZ_ACTIK, false).apply()
            }
        }
    }

    companion object {
        const val ACTION_SESSIZ = "com.example.ezanvakti.SESSIZ"
        const val ACTION_GERI_AC = "com.example.ezanvakti.SESSIZ_GERI_AC"
        private const val ANAHTAR_BIZ_ACTIK = "biz_actik"
        private const val ISTEK_BASLANGIC = 1000
        private const val MAX_ALARM = 60   // 5 vakit × 2 (aç/kapat) × 6 gün

        fun izinVar(context: Context): Boolean =
            context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

        private fun intent(context: Context, action: String, index: Int, flags: Int): PendingIntent? =
            PendingIntent.getBroadcast(
                context, ISTEK_BASLANGIC + index,
                Intent(context, SessizReceiver::class.java).setAction(action),
                flags or PendingIntent.FLAG_IMMUTABLE,
            )

        /** Önümüzdeki günlerin beş vakti için sessize alma ve geri açma alarmlarını kurar. */
        fun planla(context: Context, days: List<DailyTimes>, dakika: Int) {
            iptal(context)
            if (dakika <= 0) {
                // Özellik kapatıldıysa ve şu an bizim açtığımız sessizlik sürüyorsa hemen geri aç
                if (izinVar(context)) SessizReceiver().onReceive(context, Intent(ACTION_GERI_AC))
                return
            }
            if (!izinVar(context)) return
            val now = System.currentTimeMillis()
            val alarm = context.getSystemService(AlarmManager::class.java)
            val exact = NotificationScheduler.canScheduleExact(context)
            val olaylar = days.sortedBy { it.epochDay }
                .flatMap { day -> Vakit.bildirimli.map { day.time(it) } }
                .filter { it + dakika * 60_000L > now }
                .flatMap { t -> listOf(t to ACTION_SESSIZ, t + dakika * 60_000L to ACTION_GERI_AC) }
                .filter { it.first > now }
                .take(MAX_ALARM)
            olaylar.forEachIndexed { i, (zaman, action) ->
                val pi = intent(context, action, i, PendingIntent.FLAG_UPDATE_CURRENT) ?: return@forEachIndexed
                if (exact) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
            }
        }

        fun iptal(context: Context) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            for (i in 0 until MAX_ALARM) {
                for (action in listOf(ACTION_SESSIZ, ACTION_GERI_AC)) {
                    val pi = intent(context, action, i, PendingIntent.FLAG_NO_CREATE) ?: continue
                    alarm.cancel(pi)
                    pi.cancel()
                }
            }
        }
    }
}
