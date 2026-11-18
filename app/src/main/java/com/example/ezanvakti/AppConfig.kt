package com.example.ezanvakti

import com.example.ezanvakti.data.DiyanetAwqatProvider
import com.example.ezanvakti.data.EmushafProvider
import com.example.ezanvakti.data.FallbackProvider
import com.example.ezanvakti.data.PrayerTimesProvider
import com.example.ezanvakti.data.StaticDataProvider

/**
 * Veri kaynakları — uygulama bunları sırayla dener:
 *
 * 1. Kendi veri deponuz (önerilen, yayın için): DATA_BASE_URL'i GitHub Pages adresinizle doldurun,
 *    ör. "https://KULLANICI_ADI.github.io/ezan-vakti-data". Vakitler haftada iki kez Diyanet
 *    verisinden otomatik üretilir; kullanıcı başına giriş/jeton yoktur, kullanıcı sayısı arttıkça maliyet artmaz.
 * 2. Diyanet Awqat Salah API (isteğe bağlı): istek sınırı çok düşük olduğu için uygulamadan doğrudan
 *    kullanım önerilmez; e-posta/şifre boş bırakılırsa atlanır.
 * 3. ezanvakti.emushaf.net: Diyanet verisinin aynası; yedek olarak her zaman en sonda denenir.
 *
 * İl/ilçe kimlikleri tüm kaynaklarda aynıdır (Diyanet kimlikleri), bu yüzden kaynak değişse de
 * kullanıcının seçtiği konum geçerli kalır.
 */
object AppConfig {
    /** Kendi veri deponuzun GitHub Pages adresi. Boşsa bu kaynak atlanır. */
    const val DATA_BASE_URL = "https://doganmtl.github.io/ezan-vakti-data"

    const val DIYANET_EMAIL = ""      // Awqat Salah hesabı e-postası (isteğe bağlı)
    const val DIYANET_PASSWORD = ""   // Awqat Salah hesabı şifresi (isteğe bağlı)

    /**
     * Geri bildirimlerin gideceği e-posta adresi. Kullanıcı Ayarlar → Geri bildirim'den en fazla
     * 100 karakter yazar; telefonundaki e-posta uygulaması mesajı bu adrese hazır açar. Boşsa bölüm gizlenir.
     */
    const val GERI_BILDIRIM_EPOSTA = "doganmutlu.apps@outlook.com"

    /** Türkiye'nin Diyanet sistemindeki ülke kimliği. */
    const val TURKEY_COUNTRY_ID = 2

    fun makeProvider(): PrayerTimesProvider {
        val chain = buildList {
            if (DATA_BASE_URL.isNotBlank()) add(StaticDataProvider(DATA_BASE_URL))
            if (DIYANET_EMAIL.isNotBlank() && DIYANET_PASSWORD.isNotBlank()) {
                add(DiyanetAwqatProvider(DIYANET_EMAIL, DIYANET_PASSWORD))
            }
            add(EmushafProvider())
        }
        return if (chain.size == 1) chain.first() else FallbackProvider(chain)
    }
}
