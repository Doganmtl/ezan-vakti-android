package com.example.ezanvakti.data

import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.model.VakitCalculator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/*
 * Diyanet İşleri Başkanlığı'nın resmi geliştirici servisi: Awqat Salah.
 * Hesap başvurusu: https://awqatsalah.diyanet.gov.tr
 *
 *   POST /Auth/Login                       { email, password } → data.accessToken / refreshToken
 *   GET  /Auth/RefreshToken/{refresh}
 *   GET  /api/Place/States/{countryId}     → iller
 *   GET  /api/Place/Cities/{stateId}       → ilçeler
 *   GET  /api/PrayerTime/Monthly/{cityId}  → 30 günlük vakitler
 * Erişim jetonu 30 dk, yenileme jetonu 45 dk geçerlidir.
 */

@Serializable
internal data class AwqatEnvelope<T>(val data: T? = null, val success: Boolean? = null, val message: String? = null)

@Serializable
internal data class AwqatLogin(val email: String, val password: String)

@Serializable
internal data class AwqatToken(val accessToken: String, val refreshToken: String? = null)

@Serializable
internal data class AwqatPlace(val id: Int, val name: String)

@Serializable
internal data class AwqatTime(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val gregorianDateShort: String,   // "18.11.2026"
    val hijriDateLong: String? = null,
)

class DiyanetAwqatProvider(
    private val email: String,
    private val password: String,
) : PrayerTimesProvider {

    private val base = "https://awqatsalah.diyanet.gov.tr"
    private val mutex = Mutex()
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var issuedAt = 0L

    // Bu sınıf yalnızca Türkiye için kullanılır (yanıtta yerel saat farkı yok); yurt dışı sıradaki kaynaktan gelir.
    override suspend fun countries(): List<Place> = throw ProviderException(t("Veri alınamadı."))

    override suspend fun states(countryId: Int): List<Place> {
        if (countryId != SelectedLocation.TURKIYE_ID) throw ProviderException(t("Veri alınamadı."))
        return get<List<AwqatPlace>>("/api/Place/States/$countryId").map { Place(it.id, it.name) }
    }

    override suspend fun cities(stateId: Int): List<Place> =
        get<List<AwqatPlace>>("/api/Place/Cities/$stateId").map { Place(it.id, it.name) }

    override suspend fun times(cityId: Int, countryId: Int): List<DailyTimes> {
        if (countryId != SelectedLocation.TURKIYE_ID) throw ProviderException(t("Veri alınamadı."))
        val list = get<List<AwqatTime>>("/api/PrayerTime/Monthly/$cityId")
        return list.mapNotNull {
            VakitCalculator.parse(
                it.gregorianDateShort,
                listOf(it.fajr, it.sunrise, it.dhuhr, it.asr, it.maghrib, it.isha),
                it.hijriDateLong,
            )
        }.ifEmpty { throw ProviderException(t("Vakit bilgisi alınamadı.")) }
    }

    private suspend inline fun <reified T> get(path: String): T {
        val text = try {
            Http.request(base + path, bearer = token())
        } catch (e: HttpException) {
            if (e.code != 401) throw e
            mutex.withLock { accessToken = null }
            Http.request(base + path, bearer = token())
        }
        return Http.json.decodeFromString<AwqatEnvelope<T>>(text).data
            ?: throw ProviderException(t("Vakit bilgisi alınamadı."))
    }

    private suspend fun token(): String = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = accessToken
        if (cached != null && now - issuedAt < 25 * 60_000L) return@withLock cached

        val refresh = refreshToken
        if (refresh != null && now - issuedAt < 40 * 60_000L) {
            val refreshed = runCatching { storeToken(Http.request("$base/Auth/RefreshToken/$refresh")) }.getOrNull()
            if (refreshed != null) return@withLock refreshed
        }

        val body = Http.json.encodeToString(AwqatLogin(email, password))
        try {
            storeToken(Http.request("$base/Auth/Login", method = "POST", body = body))
        } catch (e: HttpException) {
            throw ProviderException(t("Diyanet servisine giriş yapılamadı. Hesap bilgilerini kontrol edin."))
        }
    }

    private fun storeToken(responseText: String): String {
        val token = Http.json.decodeFromString<AwqatEnvelope<AwqatToken>>(responseText).data
            ?: throw ProviderException(t("Diyanet servisine giriş yapılamadı."))
        accessToken = token.accessToken
        refreshToken = token.refreshToken
        issuedAt = System.currentTimeMillis()
        return token.accessToken
    }
}
