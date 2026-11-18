package com.example.ezanvakti.data

import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.SelectedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Vakit verisi kaynağı. Diyanet resmi API'si ve ayna servis aynı arayüzü uygular. */
interface PrayerTimesProvider {
    /** Diyanet'in vakit yayımladığı ülkeler. */
    suspend fun countries(): List<Place>
    /** Bir ülkenin illeri/eyaletleri (Türkiye'de 81 il). */
    suspend fun states(countryId: Int = SelectedLocation.TURKIYE_ID): List<Place>
    /** Bir ilin ilçeleri (Diyanet'te il merkezi de il adıyla bir ilçe olarak listelenir). */
    suspend fun cities(stateId: Int): List<Place>
    /** İlçenin önümüzdeki günlere ait vakitleri (genelde 30 gün). */
    suspend fun times(cityId: Int, countryId: Int = SelectedLocation.TURKIYE_ID): List<DailyTimes>
}

class ProviderException(message: String) : Exception(message)

class HttpException(val code: Int) : Exception(t("Sunucu beklenmeyen bir yanıt verdi (%d).", code))

object Http {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun request(
        url: String,
        method: String = "GET",
        body: String? = null,
        bearer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/json")
            bearer?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) throw HttpException(code)
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
