package com.example.ezanvakti.notify

import com.example.ezanvakti.i18n.t

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.DiniGunTuru
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.RamazanHesabi
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.model.Tebrikler
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import java.time.LocalDate

/**
 * Yerel bildirimleri planlar (ilk sürümde varsayılan bildirim sesi; ezan sesi yok):
 *  - Vakit girdiğinde (imsak, öğle, ikindi, akşam, yatsı)
 *  - Ramazan'da: imsak → "Oruç başladı", akşam → "İftar vakti", isteğe bağlı sahur hatırlatıcısı
 *  - Kandil akşamı: akşam ezanıyla "Bu gece … Kandili"
 *  - Resmî/dini bayram ve kandil sabahı tebrik mesajı (bkz. [Tebrikler])
 *  - İsteğe bağlı: her vakitten N dakika önce hatırlatma
 *  - İsteğe bağlı: vakit girince telefonu N dakika sessize alma (bkz. [SessizReceiver])
 * Uygulama her açıldığında ve telefon yeniden başladığında yenilenir.
 */
object NotificationScheduler {
    const val CHANNEL_ID = "vakit_bildirimleri"
    private const val MAX_ALARMS = 100
    private const val SAHUR_ONCESI_DK = 45
    private const val CUMA_ONCESI_DK = 45
    /** Kehf hatırlatması Cuma sabahı bu saatte (güneş doğduktan sonraysa). */
    private val KEHF_SAATI = java.time.LocalTime.of(9, 30)
    /** Bayram ve kandil tebrikleri bu saatte (bayram sabahı, bayram namazından sonra). */
    private val TEBRIK_SAATI = java.time.LocalTime.of(9, 0)

    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_ID = "id"

    data class Ayarlar(
        val vakitBildirimleri: Boolean,
        val kandilBildirimi: Boolean,
        val sahurHatirlatici: Boolean,
        val cumaHatirlatici: Boolean = true,
        val kehfHatirlatici: Boolean = true,
        /** Vakitten kaç dakika önce hatırlatılsın; 0 = kapalı. */
        val onceDk: Int = 0,
        /** Vakit girince kaç dakika sessize alınsın; 0 = kapalı. */
        val sessizDk: Int = 0,
        /** Resmî/dini bayram ve kandil sabahı tebrik mesajı. */
        val tebrikler: Boolean = true,
    ) {
        companion object {
            fun from(storage: com.example.ezanvakti.data.Storage) = Ayarlar(
                vakitBildirimleri = storage.notificationsEnabled,
                kandilBildirimi = storage.kandilBildirimi,
                sahurHatirlatici = storage.sahurHatirlatici,
                cumaHatirlatici = storage.cumaHatirlatici,
                kehfHatirlatici = storage.kehfHatirlatici,
                onceDk = storage.onceHatirlatmaDk,
                sessizDk = storage.sessizDk,
                tebrikler = storage.tebrikler,
            )
        }
    }

    private data class Plan(val time: Long, val title: String, val body: String, val tag: Int)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            t("Vakit bildirimleri"),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = t("Namaz vakitleri, iftar/sahur ve kandil bildirimleri") }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Android 12+ için tam zamanlı alarm izni var mı? */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(
        context: Context,
        days: List<DailyTimes>,
        location: SelectedLocation,
        dini: DiniGunlerVerisi,
        ayarlar: Ayarlar,
    ) {
        cancelAll(context)
        SessizReceiver.planla(context, days, ayarlar.sessizDk)
        if (!ayarlar.vakitBildirimleri) return

        val now = System.currentTimeMillis()
        val yer = location.title
        val planlar = mutableListOf<Plan>()

        for (day in days.sortedBy { it.epochDay }) {
            val date = LocalDate.ofEpochDay(day.epochDay)
            val ramazan = RamazanHesabi.bilgi(dini, date, day.hijri)
            val kandil = dini.on(date).firstOrNull { it.turu == DiniGunTuru.KANDIL }
            val cuma = date.dayOfWeek == java.time.DayOfWeek.FRIDAY

            for (vakit in Vakit.bildirimli) {
                val zaman = day.time(vakit)
                val saat = VakitCalculator.clock(zaman)
                val (title, body) = when {
                    ramazan != null && vakit == Vakit.IMSAK ->
                        t("İmsak vakti") to t("Ramazan'ın %s. günü: sahur sona erdi, oruç başladı (%s · %s).", ramazan.gun, saat, yer)
                    ramazan != null && vakit == Vakit.AKSAM ->
                        t("İftar vakti") to t("%s için iftar: %s. Hayırlı iftarlar.", yer, saat)
                    cuma && vakit == Vakit.OGLE ->
                        t("Cuma vakti girdi") to t("%s için öğle ezanı: %s. Hayırlı Cumalar.", yer, saat)
                    kandil != null && vakit == Vakit.AKSAM && ayarlar.kandilBildirimi ->
                        t("Akşam vakti girdi · Bu gece %s", kandil.adi) to t("%s için akşam ezanı: %s. %s.", yer, saat, kandil.tebrik)
                    else ->
                        t("%s vakti girdi", vakit.adi) to t("%s için %s vakti: %s", yer, vakit.ezanAdiCeviri, saat)
                }
                planlar += Plan(zaman, title, body, vakit.ordinal)
            }

            if (ayarlar.onceDk > 0) {
                val dk = ayarlar.onceDk
                for (vakit in listOf(Vakit.IMSAK, Vakit.GUNES, Vakit.OGLE, Vakit.IKINDI, Vakit.AKSAM, Vakit.YATSI)) {
                    // Aynı dakikaya düşen özel hatırlatıcılar varken ikinciyi gönderme
                    if (vakit == Vakit.IMSAK && ramazan != null && ayarlar.sahurHatirlatici && dk == SAHUR_ONCESI_DK) continue
                    if (vakit == Vakit.OGLE && cuma && ayarlar.cumaHatirlatici && dk == CUMA_ONCESI_DK) continue
                    val zaman = day.time(vakit)
                    val saat = VakitCalculator.clock(zaman)
                    val (title, body) = when {
                        vakit == Vakit.GUNES ->
                            t("Güneşin doğmasına %s dakika", dk) to t("Sabah namazı için son %s dakika (güneş %s · %s).", dk, saat, yer)
                        ramazan != null && vakit == Vakit.AKSAM ->
                            t("İftara %s dakika", dk) to t("%s için iftar: %s.", yer, saat)
                        ramazan != null && vakit == Vakit.IMSAK ->
                            t("İmsaka %s dakika", dk) to t("Sahur için son %s dakika (imsak %s · %s).", dk, saat, yer)
                        else ->
                            t("%s vaktine %s dakika", vakit.adi, dk) to t("%s için %s: %s.", yer, vakit.ezanAdiCeviri, saat)
                    }
                    planlar += Plan(zaman - dk * 60_000L, title, body, 20 + vakit.ordinal)
                }
            }

            if (ayarlar.tebrikler) {
                Tebrikler.gunun(date, dini, location.turkiyede)?.let { m ->
                    val saat = date.atTime(TEBRIK_SAATI).atZone(VakitCalculator.zone).toInstant().toEpochMilli()
                    planlar += Plan(saat, m.baslik, m.metin, 13)
                }
            }

            if (cuma && ayarlar.cumaHatirlatici) {
                val ogle = day.time(Vakit.OGLE)
                planlar += Plan(
                    ogle - CUMA_ONCESI_DK * 60_000L,
                    t("Cuma namazı yaklaşıyor"),
                    t("Cuma vaktine %s dakika kaldı (öğle %s · %s).", CUMA_ONCESI_DK, VakitCalculator.clock(ogle), yer),
                    11,
                )
            }
            if (cuma && ayarlar.kehfHatirlatici) {
                val saat = date.atTime(KEHF_SAATI).atZone(VakitCalculator.zone).toInstant().toEpochMilli()
                planlar += Plan(
                    maxOf(saat, day.time(Vakit.GUNES) + 30 * 60_000L),
                    t("Hayırlı Cumalar"),
                    t("Cuma günü Kehf suresini okumayı unutmayın."),
                    12,
                )
            }

            if (ramazan != null && ayarlar.sahurHatirlatici) {
                val imsak = day.time(Vakit.IMSAK)
                planlar += Plan(
                    imsak - SAHUR_ONCESI_DK * 60_000L,
                    t("Sahur vakti"),
                    t("İmsaka %s dakika kaldı (imsak %s · %s).", SAHUR_ONCESI_DK, VakitCalculator.clock(imsak), yer),
                    10,
                )
            }
        }

        val exact = canScheduleExact(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        planlar.filter { it.time > now }
            .sortedBy { it.time }
            .take(MAX_ALARMS)
            .forEachIndexed { index, plan ->
                val intent = Intent(context, VakitAlarmReceiver::class.java).apply {
                    putExtra(EXTRA_TITLE, plan.title)
                    putExtra(EXTRA_BODY, plan.body)
                    putExtra(EXTRA_ID, plan.tag)
                }
                val pending = PendingIntent.getBroadcast(
                    context, index, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                if (exact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.time, pending)
                } else {
                    // İzin yoksa Android birkaç dakika gecikmeli gönderebilir.
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.time, pending)
                }
            }
    }

    fun cancelAll(context: Context) {
        SessizReceiver.iptal(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        for (index in 0 until MAX_ALARMS) {
            val pending = PendingIntent.getBroadcast(
                context, index, Intent(context, VakitAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }
}
