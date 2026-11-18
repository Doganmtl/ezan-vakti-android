package com.example.ezanvakti.i18n

/** Dil kodu → (Türkçe metin → çeviri). Tablolar ilk kullanımda yüklenir. */
internal object Ceviriler {
    fun tablo(kod: String): Map<String, String>? = when (kod) {
        "en" -> ceviri_en
        "ar" -> ceviri_ar
        "de" -> ceviri_de
        "fr" -> ceviri_fr
        "es" -> ceviri_es
        "pt" -> ceviri_pt
        "ru" -> ceviri_ru
        "id" -> ceviri_id
        "ur" -> ceviri_ur
        "fa" -> ceviri_fa
        "bn" -> ceviri_bn
        else -> null
    }
}
