package com.example.ezanvakti.data

import android.content.Context
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.HicriTakvim
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.model.VakitCalculator
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

    /**
     * İlk açılış tanıtım turu görüldü mü. Tur bu sürümle geldi; önceden konum seçmiş (eski sürümden
     * güncelleyen) kullanıcılara gösterilmez.
     */
    var tanitimGoruldu: Boolean
        get() = prefs.getBoolean("tanitim_goruldu", prefs.contains("location"))
        set(value) = prefs.edit().putBoolean("tanitim_goruldu", value).apply()

    /** Vakitlerin arka planda (uygulama açılmadan) en son indirilmeye çalışıldığı an (epoch ms). */
    var arkaPlanYenileme: Long
        get() = prefs.getLong("arka_plan_yenileme", 0)
        set(value) = prefs.edit().putLong("arka_plan_yenileme", value).apply()

    /** Kullanıcının kaydettiği konumlar (ev, iş, memleket…). */
    var kayitliKonumlar: List<SelectedLocation>
        get() = prefs.getString("kayitli_konumlar", null)?.let {
            runCatching { Http.json.decodeFromString<List<SelectedLocation>>(it) }.getOrNull()
        }.orEmpty()
        set(value) = prefs.edit().putString("kayitli_konumlar", Http.json.encodeToString(value)).apply()

    /** Ana ekran arka planı (bkz. ArkaPlan): "vakit", "gece", "zumrut"… */
    var arkaPlan: String
        get() = prefs.getString("arka_plan", "vakit") ?: "vakit"
        set(value) = prefs.edit().putString("arka_plan", value).apply()

    /** Vakitlerin dizilişi: "liste", "ikili", "uclu". */
    var vakitDuzeni: String
        get() = prefs.getString("vakit_duzeni", "liste") ?: "liste"
        set(value) = prefs.edit().putString("vakit_duzeni", value).apply()

    /** Widget arka planı: "koyu", "seffaf", "tema". */
    var widgetStili: String
        get() = prefs.getString("widget_stili", "koyu") ?: "koyu"
        set(value) = prefs.edit().putString("widget_stili", value).apply()

    /** Kilit ekranı sayacının içeriği: "siradaki", "tum" (sıradaki + günün vakitleri), "ramazan" (yalnızca Ramazan'da). */
    var canliSayacModu: String
        get() = prefs.getString("canli_sayac_modu", "siradaki") ?: "siradaki"
        set(value) = prefs.edit().putString("canli_sayac_modu", value).apply()

    /** Hicri takvim seçimi: "" = ülkeye göre otomatik, "diyanet", "ummulkura". */
    var hicriTakvim: String
        get() = prefs.getString("hicri_takvim", "") ?: ""
        set(value) = prefs.edit().putString("hicri_takvim", value).apply()

    /** Hilal gözlemine göre ilan edilen tarih farklıysa gün kaydırma (−2…+2). */
    var hicriDuzeltme: Int
        get() = prefs.getInt("hicri_duzeltme", 0)
        set(value) = prefs.edit().putInt("hicri_duzeltme", value.coerceIn(-2, 2)).apply()

    /** Seçili konuma göre etkin hicri takvim ayarı. */
    fun takvimAyari(location: SelectedLocation? = this.location): HicriTakvim.Ayar =
        HicriTakvim.ayar(hicriTakvim, location?.ulkeId, hicriDuzeltme)

    private fun timesFile(cityId: Int) = File(cacheDir, "vakitler-$cityId.json")

    /**
     * Kayıtlı vakitler; hicri tarihler seçili takvime göre uyarlanmış olarak döner ve
     * konumun saat dilimi ayarlanır. Dosyada Diyanet'in ham verisi durur.
     */
    fun cachedTimes(cityId: Int): List<DailyTimes>? = runCatching {
        Http.json.decodeFromString<List<DailyTimes>>(timesFile(cityId).readText())
    }.getOrNull()?.let { hazirla(it) }

    fun saveTimes(days: List<DailyTimes>, cityId: Int) {
        runCatching { timesFile(cityId).writeText(Http.json.encodeToString(days)) }
    }

    /** Ham Diyanet verisini kullanıma hazırlar: saat dilimi + seçili hicri takvim. */
    fun hazirla(days: List<DailyTimes>): List<DailyTimes> {
        VakitCalculator.bolgeyiAyarla(days)
        return HicriTakvim.gunleriUyarla(days, takvimAyari())
    }
}
