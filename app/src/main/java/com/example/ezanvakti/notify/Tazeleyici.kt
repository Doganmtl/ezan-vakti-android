package com.example.ezanvakti.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.data.DiniGunlerRepository
import com.example.ezanvakti.data.Storage
import com.example.ezanvakti.model.GeriSayimHesabi
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.widget.VakitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Her vakit değişiminde çalışır: widget'ı ve kilit ekranı sayacını yeniler, bildirim/sessize alma
 * planının bitmesine 2 günden az kaldıysa kayıtlı vakitlerle yeniden kurar ve bir sonraki vakte kendini kurar.
 * Uygulama günlerce açılmasa da bildirimler bu sayede kesilmez.
 */
object Tazeleyici {
    private const val ACTION = "com.example.ezanvakti.TAZELE"
    private const val PLAN_UFKU_MS = 2 * 24 * 3_600_000L
    /** Kayıtlı vakitlerin bitmesine bu kadar gün kalınca arka planda yenileri indirilir. */
    private const val YENILEME_GUN = 10
    private const val YENILEME_ARALIGI_MS = 12 * 3_600_000L

    /**
     * Uygulama uzun süre açılmazsa kayıtlı vakitler (~30 gün) biter ve bildirimler/widget durur.
     * Bunu önlemek için vakitler bitmeye yaklaşınca arka planda yenileri indirilir (en çok 12 saatte bir dener).
     * Yeni veri kaydedildiyse true döner.
     */
    suspend fun veriyiYenile(context: Context): Boolean {
        val storage = Storage(context)
        val location = storage.location ?: return false
        val now = System.currentTimeMillis()
        if (now - storage.arkaPlanYenileme < YENILEME_ARALIGI_MS) return false
        val sonGun = storage.cachedTimes(location.city.id)?.maxOfOrNull { it.epochDay }
        if (sonGun != null && sonGun >= VakitCalculator.epochDay(now) + YENILEME_GUN) return false
        storage.arkaPlanYenileme = now
        val ham = AppConfig.makeProvider().times(location.city.id, location.ulkeId).sortedBy { it.epochDay }
        if (ham.isEmpty()) return false
        storage.saveTimes(ham, location.city.id)
        return true
    }

    private fun intent(context: Context, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, 950,
        Intent(context, TazeleReceiver::class.java).setAction(ACTION),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    /** [planlariDaKur] true ise bildirim planı ufka bakılmadan yeniden kurulur (açılış, ayar değişikliği). */
    fun calistir(context: Context, planlariDaKur: Boolean = false) {
        val storage = Storage(context)
        val location = storage.location
        val days = location?.let { storage.cachedTimes(it.city.id) }.orEmpty()
        val dini = DiniGunlerRepository(context).loadLocal()
        val now = System.currentTimeMillis()

        if (location != null && days.isNotEmpty()) {
            val sonGun = days.maxOf { it.epochDay }
            val ufukBitti = VakitCalculator.epochDay(now + PLAN_UFKU_MS) >= sonGun ||
                storage.planKurulma + 24 * 3_600_000L < now
            if (planlariDaKur || ufukBitti) {
                NotificationScheduler.schedule(context, days, location, dini, NotificationScheduler.Ayarlar.from(storage))
                storage.planKurulma = now
            }
        }

        VakitWidget.ciz(context, days, location, dini, now)

        val sayim = if (location != null) GeriSayimHesabi.hesapla(days, now, dini) else null
        val mod = storage.canliSayacModu
        val gosterilsin = sayim != null && (mod != "ramazan" || sayim.ramazanGunu != null)
        if (storage.canliSayac && gosterilsin && sayim != null && location != null) {
            val tum = if (mod == "tum") VakitCalculator.today(days, now)?.let { gun ->
                com.example.ezanvakti.model.Vakit.entries.chunked(3).joinToString("\n") { satir ->
                    satir.joinToString("   ") { "${it.adi} ${VakitCalculator.clock(gun.time(it))}" }
                }
            } else null
            CanliSayac.goster(context, sayim, location.title, tum)
        } else {
            CanliSayac.kaldir(context)
        }

        // Bir sonraki vakit (ya da Ramazan hedefi) gelince yeniden çalış
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = intent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val sonraki = listOfNotNull(VakitCalculator.durum(days, now)?.nextTime, sayim?.hedef).minOrNull()
        if (sonraki == null) {
            alarm.cancel(pi)
            return
        }
        val zaman = sonraki + 2_000L
        if (NotificationScheduler.canScheduleExact(context)) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC, zaman, pi)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC, zaman, pi)
        }
    }
}

class TazeleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sonuc = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // İnternet yoksa ya da kaynak yavaşsa kayıtlı vakitlerle devam edilir.
                val yenilendi = runCatching { withTimeout(8_000) { Tazeleyici.veriyiYenile(app) } }.getOrDefault(false)
                Tazeleyici.calistir(app, planlariDaKur = yenilendi)
            } finally {
                sonuc.finish()
            }
        }
    }
}
