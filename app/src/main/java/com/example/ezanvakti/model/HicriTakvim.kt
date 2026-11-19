package com.example.ezanvakti.model

import com.example.ezanvakti.data.PlaceMatcher
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters

/**
 * Hicri takvimin ülkeye göre uyarlanması.
 *
 * Diyanet'in dini günler takvimi ve günlük hicri tarihleri Türkiye'nin (hesaba dayalı) takvimidir.
 * Bazı ülkeler farklı takvim izler: Suudi Arabistan ve Körfez ülkeleri Ümmülkurâ takvimini,
 * birçok ülke ise hilalin gözlemlenmesini esas alır ve tarihler bir-iki gün kayabilir.
 *
 *  - DIYANET: Diyanet takvimi (Türkiye, Avrupa'daki DİTİB camileri, Balkanlar, Orta Asya…).
 *  - UMMULKURA: Suudi Arabistan'ın resmî takvimi; Android'in yerleşik HijrahDate (Umm al-Qura) hesabı.
 *  - [Ayar.duzeltme]: hilal gözlemine göre ilan edilen tarih farklıysa −2…+2 gün kaydırma.
 *
 * Dini günlerin kendisi (hangi gün, adı) her zaman Diyanet listesinden gelir; yalnızca miladi karşılığı değişir.
 */
object HicriTakvim {
    enum class Tur(val kod: String) { DIYANET("diyanet"), UMMULKURA("ummulkura") }

    data class Ayar(val tur: Tur, val duzeltme: Int) {
        val varsayilan: Boolean get() = tur == Tur.DIYANET && duzeltme == 0
    }

    /** Varsayılan olarak Ümmülkurâ takvimini izleyen ülkeler (Diyanet ülke kimlikleri). */
    private val ummulkuraUlkeleri = setOf(
        64,   // Suudi Arabistan
        93,   // Birleşik Arap Emirlikleri
        94,   // Katar
        132,  // Bahreyn
        133,  // Kuveyt
        148,  // Yemen
    )

    /** [secim]: "" (ülkeye göre otomatik), "diyanet" ya da "ummulkura". */
    fun ayar(secim: String, ulkeId: Int?, duzeltme: Int): Ayar {
        val tur = when (secim) {
            Tur.DIYANET.kod -> Tur.DIYANET
            Tur.UMMULKURA.kod -> Tur.UMMULKURA
            else -> if (ulkeId != null && ulkeId in ummulkuraUlkeleri) Tur.UMMULKURA else Tur.DIYANET
        }
        return Ayar(tur, duzeltme.coerceIn(-2, 2))
    }

    /** Diyanet'in kullandığı ay adları (1 = Muharrem). */
    val aylar = listOf(
        "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
        "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce",
    )
    private val ayAnahtarlari = aylar.map { PlaceMatcher.normalize(it) }
    private val ayEsAdlari = mapOf("rebiulsani" to 4, "cemaziyelula" to 5, "cemaziyelahire" to 6, "zilkada" to 11)

    data class Tarih(val gun: Int, val ay: Int, val yil: Int) {
        /** Diyanet biçiminde Türkçe: "26 Recep 1448" (uygulama içinde bu biçim saklanır). */
        val metin: String get() = "$gun ${aylar[ay - 1]} $yil"
    }

    /** Saklanan Türkçe hicri tarihi etkin dilde gösterir: "26 Recep 1448" → "26 Rajab 1448". */
    fun goster(metin: String?): String? {
        if (metin.isNullOrBlank()) return null
        if (Dil.turkce) return metin
        val h = coz(metin) ?: return metin
        return t("%s %s %s", h.gun, t(aylar[h.ay - 1]), h.yil)
    }

    /** "26 Recep 1448" → Tarih */
    fun coz(metin: String?): Tarih? {
        val p = metin?.trim()?.split(Regex("\\s+")) ?: return null
        if (p.size < 3) return null
        val gun = p[0].toIntOrNull() ?: return null
        val yil = p.last().toIntOrNull() ?: return null
        val adN = PlaceMatcher.normalize(p.subList(1, p.size - 1).joinToString(" "))
        val ay = ayAnahtarlari.indexOf(adN).takeIf { it >= 0 }?.plus(1) ?: ayEsAdlari[adN] ?: return null
        return Tarih(gun, ay, yil)
    }

    private fun ummulkura(tarih: LocalDate): Tarih? = runCatching {
        val h = HijrahDate.from(tarih)
        Tarih(h.get(ChronoField.DAY_OF_MONTH), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.YEAR))
    }.getOrNull()

    private fun ummulkura(t: Tarih): LocalDate? = runCatching {
        LocalDate.from(HijrahDate.of(t.yil, t.ay, t.gun))
    }.getOrNull()

    /** Bir hicri tarihin bu takvimdeki miladi günü. */
    private fun miladi(t: Tarih, diyanetTarihi: LocalDate, ayar: Ayar): LocalDate {
        val temel = when (ayar.tur) {
            Tur.DIYANET -> diyanetTarihi
            Tur.UMMULKURA -> ummulkura(t) ?: diyanetTarihi
        }
        return temel.plusDays(ayar.duzeltme.toLong())
    }

    /** Dini günler listesini ve Ramazan dönemlerini seçili takvime taşır. */
    fun uyarla(veri: DiniGunlerVerisi, ayar: Ayar): DiniGunlerVerisi {
        if (ayar.varsayilan) return veri
        val gunler = veri.gunler.map { g ->
            val h = coz(g.hicri)
            val yeni = when {
                h == null -> g.date.plusDays(ayar.duzeltme.toLong())
                // Regaib: Recep ayının ilk Cuma'sından önceki gece (Perşembe akşamı)
                g.ad.startsWith("Regaib") -> {
                    val recep1 = when (ayar.tur) {
                        Tur.DIYANET -> veri.gunler.firstOrNull { coz(it.hicri) == Tarih(1, 7, h.yil) }?.date
                        Tur.UMMULKURA -> ummulkura(Tarih(1, 7, h.yil))
                    }
                    if (recep1 == null) g.date.plusDays(ayar.duzeltme.toLong())
                    else recep1.plusDays(ayar.duzeltme.toLong())
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).minusDays(1)
                }
                else -> miladi(h, g.date, ayar)
            }
            g.copy(tarih = yeni.toString())
        }
        val ramazanlar = veri.ramazanlar.map { r ->
            when (ayar.tur) {
                Tur.DIYANET -> r.copy(
                    baslangic = r.start.plusDays(ayar.duzeltme.toLong()).toString(),
                    bitis = r.end.plusDays(ayar.duzeltme.toLong()).toString(),
                )
                Tur.UMMULKURA -> {
                    val bas = ummulkura(Tarih(1, 9, r.hicriYil))
                    val sevval = ummulkura(Tarih(1, 10, r.hicriYil))
                    if (bas == null || sevval == null) r.copy(
                        baslangic = r.start.plusDays(ayar.duzeltme.toLong()).toString(),
                        bitis = r.end.plusDays(ayar.duzeltme.toLong()).toString(),
                    ) else r.copy(
                        baslangic = bas.plusDays(ayar.duzeltme.toLong()).toString(),
                        bitis = sevval.minusDays(1).plusDays(ayar.duzeltme.toLong()).toString(),
                    )
                }
            }
        }
        return veri.copy(gunler = gunler.sortedBy { it.tarih }, ramazanlar = ramazanlar)
    }

    /** Günlük vakitlerdeki hicri tarihleri seçili takvime göre yeniden yazar. */
    fun gunleriUyarla(days: List<DailyTimes>, ayar: Ayar): List<DailyTimes> {
        if (ayar.varsayilan) return days
        val diyanet = days.associate { it.epochDay to it.hijri }
        return days.map { d ->
            val kaynak = LocalDate.ofEpochDay(d.epochDay).minusDays(ayar.duzeltme.toLong())
            val yeni = when (ayar.tur) {
                Tur.DIYANET -> diyanet[kaynak.toEpochDay()] ?: komsudan(diyanet, kaynak.toEpochDay())
                Tur.UMMULKURA -> ummulkura(kaynak)?.metin
            }
            d.copy(hijri = yeni)
        }
    }

    /**
     * Gün düzeltmesi verinin dışına taşınca (ilk/son günler) hicri tarihi yakındaki bir günden bulur.
     * Ay sınırı aşılacaksa (30. gün belirsiz) tahmin yapılmaz.
     */
    private fun komsudan(diyanet: Map<Long, String?>, gun: Long): String? {
        for (k in 1..3) for (yon in intArrayOf(-1, 1)) {
            val h = coz(diyanet[gun + yon * k]) ?: continue
            val yeni = h.gun - yon * k
            if (yeni in 1..29) return h.copy(gun = yeni).metin
        }
        return null
    }
}
