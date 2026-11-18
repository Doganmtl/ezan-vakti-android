package com.example.ezanvakti.data

import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.VakitCalculator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
internal data class StaticPlace(val id: Int, val ad: String)

@Serializable
internal data class StaticDay(val t: String, val v: List<String>, val h: String? = null)

@Serializable
internal data class StaticTimes(val guncelleme: String? = null, val gunler: List<StaticDay>)

/**
 * Kendi veri deponuzdaki (ezan-vakti-data, GitHub Pages) hazır JSON dosyalarını okur.
 * Dosyalar haftada iki kez Diyanet verisinden otomatik üretilir; kullanıcı başına giriş/jeton gerekmez.
 *
 *   {base}/v1/iller.json
 *   {base}/v1/ilceler/{ilId}.json
 *   {base}/v1/vakitler/{ilceId}.json
 */
class StaticDataProvider(baseUrl: String) : PrayerTimesProvider {
    private val base = baseUrl.trimEnd('/') + "/v1"

    override suspend fun states(): List<Place> =
        Http.json.decodeFromString<List<StaticPlace>>(Http.request("$base/iller.json"))
            .map { Place(it.id, it.ad) }

    override suspend fun cities(stateId: Int): List<Place> =
        Http.json.decodeFromString<List<StaticPlace>>(Http.request("$base/ilceler/$stateId.json"))
            .map { Place(it.id, it.ad) }

    override suspend fun times(cityId: Int): List<DailyTimes> {
        val data = Http.json.decodeFromString<StaticTimes>(Http.request("$base/vakitler/$cityId.json"))
        val days = data.gunler.mapNotNull { VakitCalculator.parse(it.t, it.v, it.h) }
        // Dosya var ama bugünden sonrasını kapsamıyorsa (güncelleme durmuşsa) yedeğe düşülsün.
        val today = VakitCalculator.epochDay(System.currentTimeMillis())
        if (days.none { it.epochDay >= today + 1 }) throw ProviderException("Veri dosyası güncel değil.")
        return days
    }
}

/**
 * Sırayla kaynakları dener: ilk başarılı yanıtı döndürür.
 * Örn. kendi veri dosyalarınız → ayna servis. Hepsi başarısız olursa ilk hatayı fırlatır.
 */
class FallbackProvider(private val providers: List<PrayerTimesProvider>) : PrayerTimesProvider {
    init {
        require(providers.isNotEmpty())
    }

    private suspend fun <T> firstSuccess(block: suspend (PrayerTimesProvider) -> T): T {
        var firstError: Exception? = null
        for (p in providers) {
            try {
                return block(p)
            } catch (e: Exception) {
                if (firstError == null) firstError = e
            }
        }
        throw firstError ?: ProviderException("Veri alınamadı.")
    }

    override suspend fun states() = firstSuccess { it.states() }
    override suspend fun cities(stateId: Int) = firstSuccess { it.cities(stateId) }
    override suspend fun times(cityId: Int) = firstSuccess { it.times(cityId) }
}
