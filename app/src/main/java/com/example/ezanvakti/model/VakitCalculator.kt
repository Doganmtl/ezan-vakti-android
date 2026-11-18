package com.example.ezanvakti.model

import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object VakitCalculator {
    val istanbul: ZoneId = ZoneId.of("Europe/Istanbul")
    val tr: Locale = Locale.forLanguageTag("tr-TR")

    /**
     * Seçili konumun saat dilimi. Vakitler, günün "gün", saat gösterimi ve sabah 09:00 gibi
     * planlı bildirimler bu dilime göre hesaplanır. Türkiye'de her zaman İstanbul saatidir;
     * yurt dışında Diyanet verisindeki günlük UTC farkından bulunur (bkz. [bolgeyiAyarla]).
     */
    @Volatile
    var zone: ZoneId = istanbul
        private set

    private val dayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

    /** "18.11.2026" + ["05:57", ...] (6 vakit) → DailyTimes. [ofsetDk] boşsa Türkiye saati. */
    fun parse(day: String, hhmm: List<String>, hijri: String?, ofsetDk: Int? = null): DailyTimes? = runCatching {
        val date = LocalDate.parse(day.trim(), dayFormat)
        val gunZone: ZoneId = ofsetDk?.let { ZoneOffset.ofTotalSeconds(it * 60) } ?: istanbul
        val times = hhmm.map { t ->
            date.atTime(LocalTime.parse(t.trim().take(5))).atZone(gunZone).toInstant().toEpochMilli()
        }
        require(times.size == 6)
        DailyTimes(date.toEpochDay(), times, hijri, ofsetDk)
    }.getOrNull()

    /**
     * Vakit listesinden konumun saat dilimini belirler. Telefonun saat dilimi o konumla aynı farka sahipse
     * (genellikle kullanıcı oradadır) telefonun dilimi kullanılır; böylece yaz saati geçişleri de doğru olur.
     * Değilse (ör. Türkiye'deyken Berlin seçilmişse) verideki sabit UTC farkı kullanılır.
     */
    fun bolgeyiAyarla(days: List<DailyTimes>) {
        val now = System.currentTimeMillis()
        val ofset = days.firstOrNull { it.epochDay == Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }?.ofsetDk
            ?: days.firstNotNullOfOrNull { it.ofsetDk }
        if (ofset == null) {
            zone = istanbul
            return
        }
        val telefon = ZoneId.systemDefault()
        zone = if (telefon.rules.getOffset(Instant.ofEpochMilli(now)).totalSeconds == ofset * 60) telefon
        else ZoneOffset.ofTotalSeconds(ofset * 60)
    }

    fun epochDay(now: Long): Long = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()

    fun today(days: List<DailyTimes>, now: Long): DailyTimes? {
        val d = epochDay(now)
        return days.firstOrNull { it.epochDay == d }
    }

    /**
     * Şu an içinde bulunulan vakit ve sıradakini hesaplar.
     * Gece yarısından imsaka kadar olan süre bir önceki günün yatsı vaktine aittir.
     */
    fun durum(days: List<DailyTimes>, now: Long): VakitDurumu? {
        val sorted = days.sortedBy { it.epochDay }
        val timeline = sorted.flatMap { day -> Vakit.entries.map { it to day.time(it) } }
        val nextIndex = timeline.indexOfFirst { it.second > now }
        if (nextIndex < 0) return null
        if (nextIndex == 0) {
            // Veri bugünden başlıyor ve imsaktan önceyiz: dünkü yatsıyı bugünkünden tahmin et.
            val first = sorted.first()
            return VakitDurumu(Vakit.YATSI, first.time(Vakit.YATSI) - 86_400_000L, Vakit.IMSAK, first.time(Vakit.IMSAK))
        }
        val (curVakit, curTime) = timeline[nextIndex - 1]
        val (nextVakit, nextTime) = timeline[nextIndex]
        return VakitDurumu(curVakit, curTime, nextVakit, nextTime)
    }

    /** "01:23:45" veya "23:45" */
    fun countdown(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** "1 saat 23 dakika" (TalkBack için) */
    fun spoken(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        return when {
            h > 0 && m > 0 -> t("%d saat %d dakika", h, m)
            h > 0 -> t("%d saat", h)
            m > 0 -> t("%d dakika", m)
            else -> t("1 dakikadan az")
        }
    }

    fun clock(ms: Long): String = clockFormat.format(Instant.ofEpochMilli(ms).atZone(zone))

    /** "18 Kasım 2026, Çarşamba" — etkin dilde. */
    fun longDate(ms: Long): String =
        DateTimeFormatter.ofPattern(if (Dil.kod == "en") "EEEE, d MMMM yyyy" else "d MMMM yyyy, EEEE", Dil.locale)
            .format(Instant.ofEpochMilli(ms).atZone(zone))
}
