package com.example.ezanvakti.data

import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.VakitCalculator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
internal data class EmStateDto(
    @SerialName("SehirAdi") val name: String,
    @SerialName("SehirID") val id: String,
)

@Serializable
internal data class EmCityDto(
    @SerialName("IlceAdi") val name: String,
    @SerialName("IlceID") val id: String,
)

@Serializable
internal data class EmTimeDto(
    @SerialName("Imsak") val imsak: String,
    @SerialName("Gunes") val gunes: String,
    @SerialName("Ogle") val ogle: String,
    @SerialName("Ikindi") val ikindi: String,
    @SerialName("Aksam") val aksam: String,
    @SerialName("Yatsi") val yatsi: String,
    @SerialName("MiladiTarihKisa") val date: String,       // "18.11.2026"
    @SerialName("HicriTarihUzun") val hijri: String? = null,
)

/**
 * Diyanet verisini birebir yansıtan, kimlik doğrulama gerektirmeyen ayna servis.
 * İl/ilçe kimlikleri Diyanet'inkilerle aynıdır (ör. İstanbul merkez = 9541).
 * Sadece geliştirme ve deneme içindir; yayında resmi Awqat Salah servisini kullanın.
 */
class EmushafProvider : PrayerTimesProvider {
    private val base = "https://ezanvakti.emushaf.net"

    override suspend fun states(): List<Place> =
        Http.json.decodeFromString<List<EmStateDto>>(Http.request("$base/sehirler/${AppConfig.TURKEY_COUNTRY_ID}"))
            .mapNotNull { dto -> dto.id.toIntOrNull()?.let { Place(it, dto.name) } }

    override suspend fun cities(stateId: Int): List<Place> =
        Http.json.decodeFromString<List<EmCityDto>>(Http.request("$base/ilceler/$stateId"))
            .mapNotNull { dto -> dto.id.toIntOrNull()?.let { Place(it, dto.name) } }

    override suspend fun times(cityId: Int): List<DailyTimes> =
        Http.json.decodeFromString<List<EmTimeDto>>(Http.request("$base/vakitler/$cityId"))
            .mapNotNull {
                VakitCalculator.parse(it.date, listOf(it.imsak, it.gunes, it.ogle, it.ikindi, it.aksam, it.yatsi), it.hijri)
            }
            .ifEmpty { throw ProviderException("Vakit bilgisi alınamadı.") }
}
