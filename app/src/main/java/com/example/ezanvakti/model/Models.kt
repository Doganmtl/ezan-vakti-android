package com.example.ezanvakti.model

import com.example.ezanvakti.data.PlaceMatcher
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import kotlinx.serialization.Serializable
import java.util.Locale

/** Günün altı vakti (Diyanet sıralaması). */
enum class Vakit(val ad: String, val namazAdi: String?, val ezanAdi: String, val emoji: String) {
    IMSAK("İmsak", "Sabah Namazı", "sabah ezanı (imsak)", "🌙"),
    GUNES("Güneş", null, "güneş doğuşu", "🌅"),
    OGLE("Öğle", "Öğle Namazı", "öğle ezanı", "☀️"),
    IKINDI("İkindi", "İkindi Namazı", "ikindi ezanı", "🌤️"),
    AKSAM("Akşam", "Akşam Namazı", "akşam ezanı", "🌇"),
    YATSI("Yatsı", "Yatsı Namazı", "yatsı ezanı", "✨");

    /** Etkin dildeki adı ("İmsak", "Fajr", "الفجر"…). */
    val adi: String get() = t(ad)
    val ezanAdiCeviri: String get() = t(ezanAdi)
    val namazAdiCeviri: String? get() = namazAdi?.let { t(it) }

    companion object {
        /** Bildirim gönderilen vakitler (güneş doğuşu hariç). */
        val bildirimli = listOf(IMSAK, OGLE, IKINDI, AKSAM, YATSI)
    }
}

/** Ülke, il/eyalet ya da ilçe/şehir. [nameEn] Diyanet'in İngilizce yazımı (yurt dışı yerler için). */
@Serializable
data class Place(val id: Int, val name: String, val nameEn: String? = null) {
    /**
     * Türkçede Diyanet'in Türkçe adı, diğer dillerde (varsa) İngilizce adı.
     * Büyük/küçük harf dönüşümü adın diline göre yapılır: Türkçe adlarda Türkçe kural (I → ı, İ → i),
     * yabancı adlarda (ör. "CALIFORNIA", "ILLINOIS") dilden bağımsız kural (I → i). Diyanet yurt dışı
     * eyalet/şehirleri için Türkçe ve İngilizce adı aynı yazar; ad farklıysa (ülkeler, KKTC) Türkçedir.
     */
    val gorunenAd: String
        get() {
            val en = nameEn
            return when {
                en == null || en.isBlank() -> name.trTitle()                  // Türkiye'deki il/ilçe
                !Dil.turkce -> en.baslikHarf()
                PlaceMatcher.normalize(name) == PlaceMatcher.normalize(en) -> name.baslikHarf()
                else -> name.trTitle()
            }
        }
}

/**
 * Seçili konum. [country] boşsa Türkiye'dir (eski sürümlerden kalan kayıtlar böyle okunur).
 * Türkiye'de state/city il/ilçe; yurt dışında eyalet/bölge ve şehirdir.
 */
@Serializable
data class SelectedLocation(val state: Place, val city: Place, val country: Place? = null) {
    val ulkeId: Int get() = country?.id ?: TURKIYE_ID
    val turkiyede: Boolean get() = ulkeId == TURKIYE_ID

    val title: String
        get() {
            val c = city.gorunenAd
            val s = state.gorunenAd
            return when {
                PlaceMatcher.normalize(city.name) == PlaceMatcher.normalize(state.name) ->
                    if (country != null && !turkiyede && PlaceMatcher.normalize(state.name) != PlaceMatcher.normalize(country.name)) {
                        "$s, ${country.gorunenAd}"
                    } else s
                else -> "$c, $s"
            }
        }

    companion object {
        /** Türkiye'nin Diyanet sistemindeki ülke kimliği. */
        const val TURKIYE_ID = 2
    }
}

/** Bir günün vakitleri. [times] Vakit sırasıyla epoch milisaniye. */
@Serializable
data class DailyTimes(
    val epochDay: Long,
    val times: List<Long>,
    val hijri: String? = null,
    /** O günün yerel saatinin UTC farkı (dakika). Boşsa Türkiye saati (eski kayıtlar ve Türkiye verisi). */
    val ofsetDk: Int? = null,
) {
    fun time(vakit: Vakit): Long = times[vakit.ordinal]
}

/** Belirli bir andaki durum: hangi vakitteyiz, sıradaki vakit ne zaman. */
data class VakitDurumu(
    val current: Vakit,
    val currentStart: Long,
    val next: Vakit,
    val nextTime: Long,
) {
    fun remaining(now: Long): Long = (nextTime - now).coerceAtLeast(0)
    fun elapsed(now: Long): Long = now - currentStart
    fun progress(now: Long): Float {
        val total = nextTime - currentStart
        if (total <= 0) return 0f
        return ((now - currentStart).toFloat() / total).coerceIn(0f, 1f)
    }
}

/** "KADIKÖY" → "Kadıköy" (Türkçe kurallarla). */
fun String.trTitle(): String =
    lowercase(VakitCalculator.tr).split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(VakitCalculator.tr) }
    }

/** "NEW YORK" → "New York", "BOSNIA-HERZEGOVINA" → "Bosnia-Herzegovina" */
fun String.baslikHarf(): String =
    replace('İ', 'I').lowercase(Locale.ROOT).split(" ").joinToString(" ") { kelime ->
        kelime.split("-").joinToString("-") { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
    }
