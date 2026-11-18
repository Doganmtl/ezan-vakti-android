package com.example.ezanvakti.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object VakitCalculator {
    val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    val tr: Locale = Locale.forLanguageTag("tr-TR")

    private val dayFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    private val longDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", tr).withZone(zone)

    /** "18.11.2026" + ["05:57", ...] (6 vakit) → DailyTimes */
    fun parse(day: String, hhmm: List<String>, hijri: String?): DailyTimes? = runCatching {
        val date = LocalDate.parse(day.trim(), dayFormat)
        val times = hhmm.map { t ->
            date.atTime(LocalTime.parse(t.trim().take(5))).atZone(zone).toInstant().toEpochMilli()
        }
        require(times.size == 6)
        DailyTimes(date.toEpochDay(), times, hijri)
    }.getOrNull()

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
            h > 0 && m > 0 -> "$h saat $m dakika"
            h > 0 -> "$h saat"
            m > 0 -> "$m dakika"
            else -> "1 dakikadan az"
        }
    }

    fun clock(ms: Long): String = clockFormat.format(Instant.ofEpochMilli(ms))
    fun longDate(ms: Long): String = longDateFormat.format(Instant.ofEpochMilli(ms))
}
