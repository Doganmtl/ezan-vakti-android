package com.example.ezanvakti.data

import android.content.Context
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.SelectedLocation
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

/** Seçili konumu ve son indirilen vakitleri saklar; internet yokken de uygulama çalışır. */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("ezan_vakti", Context.MODE_PRIVATE)
    private val cacheDir = context.filesDir

    var location: SelectedLocation?
        get() = prefs.getString("location", null)?.let {
            runCatching { Http.json.decodeFromString<SelectedLocation>(it) }.getOrNull()
        }
        set(value) {
            prefs.edit().putString("location", value?.let { Http.json.encodeToString(it) }).apply()
        }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    /** Kandil akşamı, akşam ezanıyla birlikte "Bu gece … Kandili" bildirimi. */
    var kandilBildirimi: Boolean
        get() = prefs.getBoolean("kandil_bildirimi", true)
        set(value) = prefs.edit().putBoolean("kandil_bildirimi", value).apply()

    /** Ramazan'da imsaktan 45 dk önce sahur hatırlatması. */
    var sahurHatirlatici: Boolean
        get() = prefs.getBoolean("sahur_hatirlatici", true)
        set(value) = prefs.edit().putBoolean("sahur_hatirlatici", value).apply()

    /** Cuma günü, öğle (Cuma) vaktinden 45 dk önce hatırlatma. */
    var cumaHatirlatici: Boolean
        get() = prefs.getBoolean("cuma_hatirlatici", true)
        set(value) = prefs.edit().putBoolean("cuma_hatirlatici", value).apply()

    /** Cuma sabahı Kehf suresi hatırlatması. */
    var kehfHatirlatici: Boolean
        get() = prefs.getBoolean("kehf_hatirlatici", true)
        set(value) = prefs.edit().putBoolean("kehf_hatirlatici", value).apply()

    /** Vakitten kaç dakika önce hatırlatılsın (0 = kapalı). */
    var onceHatirlatmaDk: Int
        get() = prefs.getInt("once_hatirlatma_dk", 0)
        set(value) = prefs.edit().putInt("once_hatirlatma_dk", value).apply()

    /** Vakit girince telefon kaç dakika sessize alınsın (0 = kapalı). */
    var sessizDk: Int
        get() = prefs.getInt("sessiz_dk", 0)
        set(value) = prefs.edit().putInt("sessiz_dk", value).apply()

    /** Resmî/dini bayram ve kandil tebrikleri. */
    var tebrikler: Boolean
        get() = prefs.getBoolean("tebrikler", true)
        set(value) = prefs.edit().putBoolean("tebrikler", value).apply()

    /** Kilit ekranında canlı geri sayım. */
    var canliSayac: Boolean
        get() = prefs.getBoolean("canli_sayac", false)
        set(value) = prefs.edit().putBoolean("canli_sayac", value).apply()

    /** Başka bir ile gidildiğinde konumu güncellemeyi öner. */
    var seyahatModu: Boolean
        get() = prefs.getBoolean("seyahat_modu", true)
        set(value) = prefs.edit().putBoolean("seyahat_modu", value).apply()

    /** Bildirim planının en son kurulduğu an (epoch ms). */
    var planKurulma: Long
        get() = prefs.getLong("plan_kurulma", 0)
        set(value) = prefs.edit().putLong("plan_kurulma", value).apply()

    /** Kullanıcının kaydettiği konumlar (ev, iş, memleket…). */
    var kayitliKonumlar: List<SelectedLocation>
        get() = prefs.getString("kayitli_konumlar", null)?.let {
            runCatching { Http.json.decodeFromString<List<SelectedLocation>>(it) }.getOrNull()
        }.orEmpty()
        set(value) = prefs.edit().putString("kayitli_konumlar", Http.json.encodeToString(value)).apply()

    private fun timesFile(cityId: Int) = File(cacheDir, "vakitler-$cityId.json")

    fun cachedTimes(cityId: Int): List<DailyTimes>? = runCatching {
        Http.json.decodeFromString<List<DailyTimes>>(timesFile(cityId).readText())
    }.getOrNull()

    fun saveTimes(days: List<DailyTimes>, cityId: Int) {
        runCatching { timesFile(cityId).writeText(Http.json.encodeToString(days)) }
    }
}
