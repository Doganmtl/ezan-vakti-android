package com.example.ezanvakti.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Dini günün türü; takvimde renk ve simge seçimi buna göre yapılır. */
enum class DiniGunTuru(val key: String, val etiket: String, val emoji: String) {
    KANDIL("kandil", "Kandil", "🕯️"),
    BAYRAM("bayram", "Bayram", "🎉"),
    AREFE("arefe", "Arefe", "🌙"),
    RAMAZAN("ramazan", "Ramazan", "🌙"),
    GUN("gun", "Mübarek gün", "✨");

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key } ?: GUN
    }
}

/**
 * Diyanet dini günler takvimindeki bir kayıt.
 * Kandillerde [tarih], kandilin idrak edildiği akşamın tarihidir (Diyanet'in yayımladığı gibi).
 */
@Serializable
data class DiniGun(
    val tarih: String,          // "2027-01-04"
    val tur: String,            // kandil | bayram | arefe | ramazan | gun
    val ad: String,             // "Miraç Kandili"
    val hicri: String? = null,  // "26 Recep 1448"
) {
    val date: LocalDate get() = LocalDate.parse(tarih)
    val turu: DiniGunTuru get() = DiniGunTuru.from(tur)

    /** "Miraç Kandiliniz mübarek olsun", "Kadir Geceniz mübarek olsun", "Bayramınız mübarek olsun" */
    val tebrik: String
        get() = when {
            ad.endsWith("Kandili") -> "${ad}niz mübarek olsun"
            ad.endsWith("Gecesi") -> "${ad.dropLast(3)}niz mübarek olsun"
            turu == DiniGunTuru.BAYRAM -> "Bayramınız mübarek olsun"
            else -> "Hayırlı olsun"
        }
}

@Serializable
data class RamazanDonemi(
    val baslangic: String,      // ilk oruç günü
    val bitis: String,          // son oruç günü (bayram arefesi)
    val hicriYil: Int,
) {
    val start: LocalDate get() = LocalDate.parse(baslangic)
    val end: LocalDate get() = LocalDate.parse(bitis)
    val gunSayisi: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
    fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    fun gun(d: LocalDate) = ChronoUnit.DAYS.between(start, d).toInt() + 1
}

@Serializable
data class DiniGunlerVerisi(
    val guncelleme: String,
    val kaynak: String? = null,
    val gecerliOlduguSon: String? = null,
    val gunler: List<DiniGun> = emptyList(),
    val ramazanlar: List<RamazanDonemi> = emptyList(),
) {
    fun on(d: LocalDate): List<DiniGun> = gunler.filter { it.date == d }

    /** [from] dahil, ileriye doğru sıralı. */
    fun upcoming(from: LocalDate, limit: Int = 12): List<DiniGun> =
        gunler.filter { !it.date.isBefore(from) }.sortedBy { it.tarih }.take(limit)

    fun ramazan(d: LocalDate): RamazanDonemi? = ramazanlar.firstOrNull { it.contains(d) }

    /** Ramazan şu an değilse, başlamasına kalan en yakın dönem. */
    fun sonrakiRamazan(from: LocalDate): RamazanDonemi? =
        ramazanlar.filter { !it.end.isBefore(from) }.minByOrNull { it.baslangic }

    companion object {
        val BOS = DiniGunlerVerisi(guncelleme = "0000-00-00")
    }
}

/** Ramazan içindeki gün bilgisi. [toplam] bilinmiyorsa null. */
data class RamazanGunu(val gun: Int, val toplam: Int?)

object RamazanHesabi {
    private val hicriRamazan = Regex("""^\s*(\d{1,2})\s+Ramazan\b""")

    /**
     * [date] bir oruç günü mü? Önce Diyanet dini günler listesine, liste o yılı kapsamıyorsa
     * o günün Diyanet hicri tarihine ("5 Ramazan 1449") bakar.
     * [onizlemeBaslangic] yalnızca geliştirici önizlemesi içindir.
     */
    fun bilgi(
        dini: DiniGunlerVerisi,
        date: LocalDate,
        hicri: String?,
        onizlemeBaslangic: LocalDate? = null,
    ): RamazanGunu? {
        dini.ramazan(date)?.let { return RamazanGunu(it.gun(date), it.gunSayisi) }
        hicri?.let { h -> hicriRamazan.find(h)?.let { return RamazanGunu(it.groupValues[1].toInt(), null) } }
        if (onizlemeBaslangic != null) {
            val g = ChronoUnit.DAYS.between(onizlemeBaslangic, date).toInt() + 1
            if (g in 1..29) return RamazanGunu(g, 29)
        }
        return null
    }

    enum class Hedef { SAHUR, IFTAR }

    data class Durum(
        val gun: RamazanGunu,
        val orucGunu: DailyTimes,     // sahuru/iftarı söz konusu olan gün
        val hedef: Hedef,
        val hedefZamani: Long,
    ) {
        val sahur: Long get() = orucGunu.time(Vakit.IMSAK)
        val iftar: Long get() = orucGunu.time(Vakit.AKSAM)
        val ilkSahur: Boolean get() = gun.gun == 1 && hedef == Hedef.SAHUR
    }

    /**
     * Şu an için Ramazan durumu:
     *  - imsaktan önce → bugünün sahuru
     *  - imsak–akşam arası → bugünün iftarı
     *  - akşamdan sonra → yarının sahuru (yarın oruç günüyse; ör. Ramazan'dan önceki gece "ilk sahur")
     */
    fun durum(
        days: List<DailyTimes>,
        now: Long,
        dini: DiniGunlerVerisi,
        onizlemeBaslangic: LocalDate? = null,
    ): Durum? {
        val today = VakitCalculator.today(days, now) ?: return null
        val (gunVerisi, hedef) = when {
            now < today.time(Vakit.IMSAK) -> today to Hedef.SAHUR
            now < today.time(Vakit.AKSAM) -> today to Hedef.IFTAR
            else -> (days.firstOrNull { it.epochDay == today.epochDay + 1 } ?: return null) to Hedef.SAHUR
        }
        val gun = bilgi(dini, LocalDate.ofEpochDay(gunVerisi.epochDay), gunVerisi.hijri, onizlemeBaslangic) ?: return null
        val hedefZamani = if (hedef == Hedef.SAHUR) gunVerisi.time(Vakit.IMSAK) else gunVerisi.time(Vakit.AKSAM)
        return Durum(gun, gunVerisi, hedef, hedefZamani)
    }
}
