package com.example.ezanvakti.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Uygulama dili. Kaynak metinler Türkçedir; her dil için [Ceviriler] tablosu Türkçe metni anahtar olarak kullanır.
 * Bir çeviri eksikse Türkçe metin gösterilir, böylece eksik çeviri hiçbir zaman çökme ya da boş metin yaratmaz.
 */
object Dil {
    data class Secenek(val kod: String, val ad: String)

    /** "" = telefonun dili. Adlar her dilin kendi yazımıyla. */
    val secenekler = listOf(
        Secenek("", "Telefon dili"),
        Secenek("tr", "Türkçe"),
        Secenek("en", "English"),
        Secenek("ar", "العربية"),
        Secenek("de", "Deutsch"),
        Secenek("fr", "Français"),
        Secenek("es", "Español"),
        Secenek("pt", "Português"),
        Secenek("ru", "Русский"),
        Secenek("id", "Bahasa Indonesia"),
        Secenek("ur", "اردو"),
        Secenek("fa", "فارسی"),
        Secenek("bn", "বাংলা"),
    )
    private val desteklenen = secenekler.map { it.kod }.filter { it.isNotEmpty() }.toSet()

    private const val PREFS = "ezan_dil"
    private const val ANAHTAR = "dil"

    /** Etkin dil kodu ("tr", "en", …). */
    @Volatile
    var kod: String = "tr"
        private set

    /** Kullanıcının seçimi ("" = telefon dili). */
    @Volatile
    var secim: String = ""
        private set

    val locale: Locale get() = Locale.forLanguageTag(kod)
    val turkce: Boolean get() = kod == "tr"
    val sagdanSola: Boolean get() = kod == "ar" || kod == "ur" || kod == "fa"

    /** Uygulama açılırken (EzanApp) ve dil değişince çağrılır. */
    fun baslat(context: Context) {
        secim = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ANAHTAR, "") ?: ""
        kod = cozumle(secim)
    }

    fun kaydet(context: Context, yeni: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ANAHTAR, yeni).commit()
        baslat(context)
    }

    private fun cozumle(secim: String): String {
        if (secim in desteklenen) return secim
        val telefon = Locale.getDefault().language.let { if (it == "in") "id" else it }  // eski Endonezce kodu
        return if (telefon in desteklenen) telefon else "en"
    }

    /** Etkinliğin bağlamını seçili dile (ve sağdan sola yöne) çevirir. Telefon dili seçiliyse dokunmaz. */
    fun sar(base: Context): Context {
        if (secim.isEmpty()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun ceviri(metin: String): String = if (kod == "tr") metin else Ceviriler.tablo(kod)?.get(metin) ?: metin
}

/** Türkçe metni etkin dile çevirir; [args] varsa %s/%d biçimlendirmesi uygulanır. */
fun t(metin: String, vararg args: Any?): String {
    val c = Dil.ceviri(metin)
    return if (args.isEmpty()) c else runCatching { String.format(Locale.ROOT, c, *args) }
        .getOrElse { String.format(Locale.ROOT, metin, *args) }
}
