package com.example.ezanvakti.data

import android.content.Context
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.HicriTakvim
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * Dini günler takvimi:
 *  1. Uygulamayla gelen kopya (assets/dini_gunler.json) — internet olmasa da çalışır.
 *  2. Veri deposundaki güncel kopya ({DATA_BASE_URL}/v1/dini-gunler.json) — Diyanet yeni yılın
 *     takvimini yayımladığında uygulama güncellemesi gerekmeden gelir.
 * Hangisinin "guncelleme" tarihi daha yeniyse o kullanılır.
 */
class DiniGunlerRepository(private val context: Context) {
    private val cacheFile = File(context.filesDir, "dini-gunler.json")

    /** Seçili konumun hicri takvimine uyarlanmış dini günler (bkz. [HicriTakvim]). */
    fun loadLocal(): DiniGunlerVerisi = HicriTakvim.uyarla(loadRaw(), Storage(context).takvimAyari())

    /** Diyanet'in yayımladığı haliyle (Türkiye takvimi). */
    fun loadRaw(): DiniGunlerVerisi {
        val bundled = runCatching {
            context.assets.open("dini_gunler.json").bufferedReader().use { Http.json.decodeFromString<DiniGunlerVerisi>(it.readText()) }
        }.getOrNull()
        val cached = runCatching { Http.json.decodeFromString<DiniGunlerVerisi>(cacheFile.readText()) }.getOrNull()
        return listOfNotNull(bundled, cached).maxByOrNull { it.guncelleme } ?: DiniGunlerVerisi.BOS
    }

    /** Uzak kopyayı indirir; yerelden yeniyse kaydedip döndürür, değilse null. */
    suspend fun refresh(baseUrl: String, current: DiniGunlerVerisi): DiniGunlerVerisi? {
        if (baseUrl.isBlank()) return null
        val remote = runCatching {
            Http.json.decodeFromString<DiniGunlerVerisi>(Http.request(baseUrl.trimEnd('/') + "/v1/dini-gunler.json"))
        }.getOrNull() ?: return null
        if (remote.gunler.isEmpty() || remote.guncelleme <= current.guncelleme) return null
        runCatching { cacheFile.writeText(Http.json.encodeToString(DiniGunlerVerisi.serializer(), remote)) }
        return loadLocal()
    }
}
