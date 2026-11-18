package com.example.ezanvakti.model

import com.example.ezanvakti.data.PlaceMatcher
import kotlinx.serialization.Serializable

/** Günün altı vakti (Diyanet sıralaması). */
enum class Vakit(val ad: String, val namazAdi: String?, val ezanAdi: String, val emoji: String) {
    IMSAK("İmsak", "Sabah Namazı", "sabah ezanı (imsak)", "🌙"),
    GUNES("Güneş", null, "güneş doğuşu", "🌅"),
    OGLE("Öğle", "Öğle Namazı", "öğle ezanı", "☀️"),
    IKINDI("İkindi", "İkindi Namazı", "ikindi ezanı", "🌤️"),
    AKSAM("Akşam", "Akşam Namazı", "akşam ezanı", "🌇"),
    YATSI("Yatsı", "Yatsı Namazı", "yatsı ezanı", "✨");

    companion object {
        /** Bildirim gönderilen vakitler (güneş doğuşu hariç). */
        val bildirimli = listOf(IMSAK, OGLE, IKINDI, AKSAM, YATSI)
    }
}

/** İl veya ilçe. */
@Serializable
data class Place(val id: Int, val name: String)

/** Seçili konum (il + ilçe). */
@Serializable
data class SelectedLocation(val state: Place, val city: Place) {
    val title: String
        get() = if (PlaceMatcher.normalize(city.name) == PlaceMatcher.normalize(state.name)) {
            state.name.trTitle()
        } else {
            "${city.name.trTitle()}, ${state.name.trTitle()}"
        }
}

/** Bir günün vakitleri. [times] Vakit sırasıyla epoch milisaniye. */
@Serializable
data class DailyTimes(
    val epochDay: Long,
    val times: List<Long>,
    val hijri: String? = null,
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
