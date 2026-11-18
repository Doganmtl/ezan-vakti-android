package com.example.ezanvakti.data

import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.model.VakitCalculator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
internal data class EmCountryDto(
    @SerialName("UlkeAdi") val name: String,
    @SerialName("UlkeAdiEn") val nameEn: String? = null,
    @SerialName("UlkeID") val id: String,
)

@Serializable
internal data class EmStateDto(
    @SerialName("SehirAdi") val name: String,
    @SerialName("SehirAdiEn") val nameEn: String? = null,
    @SerialName("SehirID") val id: String,
)

@Serializable
internal data class EmCityDto(
    @SerialName("IlceAdi") val name: String,
    @SerialName("IlceAdiEn") val nameEn: String? = null,
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
    /** "2026-11-14T00:00:00.0000000-05:00": o günün yerel UTC farkı (yaz saati dahil). */
    @SerialName("MiladiTarihUzunIso8601") val iso: String? = null,
)

/** "…-05:00" → -300, "…+03:30" → 210 */
internal fun isoOfsetDk(iso: String?): Int? {
    val m = Regex("""([+-])(\d{2}):?(\d{2})$""").find(iso?.trim() ?: return null) ?: return null
    val dk = m.groupValues[2].toInt() * 60 + m.groupValues[3].toInt()
    return if (m.groupValues[1] == "-") -dk else dk
}

/**
 * Diyanet verisini birebir yansıtan, kimlik doğrulama gerektirmeyen ayna servis.
 * İl/ilçe kimlikleri Diyanet'inkilerle aynıdır (ör. İstanbul merkez = 9541).
 * Sadece geliştirme ve deneme içindir; yayında resmi Awqat Salah servisini kullanın.
 */
class EmushafProvider : PrayerTimesProvider {
    private val base = "https://ezanvakti.emushaf.net"

    override suspend fun countries(): List<Place> =
        Http.json.decodeFromString<List<EmCountryDto>>(Http.request("$base/ulkeler"))
            .mapNotNull { dto -> dto.id.toIntOrNull()?.let { Place(it, dto.name, dto.nameEn) } }

    override suspend fun states(countryId: Int): List<Place> =
        Http.json.decodeFromString<List<EmStateDto>>(Http.request("$base/sehirler/$countryId"))
            .mapNotNull { dto -> dto.id.toIntOrNull()?.let { Place(it, dto.name, dto.nameEn.yurtDisi(countryId)) } }

    override suspend fun cities(stateId: Int): List<Place> =
        Http.json.decodeFromString<List<EmCityDto>>(Http.request("$base/ilceler/$stateId"))
            .mapNotNull { dto -> dto.id.toIntOrNull()?.let { Place(it, dto.name, dto.nameEn) } }

    override suspend fun times(cityId: Int, countryId: Int): List<DailyTimes> {
        val turkiye = countryId == SelectedLocation.TURKIYE_ID
        return Http.json.decodeFromString<List<EmTimeDto>>(Http.request("$base/vakitler/$cityId"))
            .mapNotNull {
                VakitCalculator.parse(
                    it.date,
                    listOf(it.imsak, it.gunes, it.ogle, it.ikindi, it.aksam, it.yatsi),
                    it.hijri,
                    if (turkiye) null else isoOfsetDk(it.iso),
                )
            }
            .ifEmpty { throw ProviderException(t("Vakit bilgisi alınamadı.")) }
    }

    /** Türkiye'deki yerlerin İngilizce adı saklanmaz; il/ilçe adları her dilde Türkçe yazılır. */
    private fun String?.yurtDisi(countryId: Int): String? = if (countryId == SelectedLocation.TURKIYE_ID) null else this
}
