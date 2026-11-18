package com.example.ezanvakti.data

import com.example.ezanvakti.model.Place

/** Android'in döndürdüğü yer adlarını ("İstanbul", "Kadıköy") Diyanet listesiyle ("İSTANBUL", "KADIKÖY") eşleştirir. */
object PlaceMatcher {
    private val map = mapOf(
        'ı' to 'i', 'İ' to 'i', 'I' to 'i', 'ş' to 's', 'Ş' to 's', 'ğ' to 'g', 'Ğ' to 'g',
        'ü' to 'u', 'Ü' to 'u', 'ö' to 'o', 'Ö' to 'o', 'ç' to 'c', 'Ç' to 'c',
        'â' to 'a', 'Â' to 'a', 'î' to 'i', 'Î' to 'i', 'û' to 'u', 'Û' to 'u',
    )

    private val aksanlar = Regex("\\p{Mn}+")

    fun normalize(text: String): String =
        java.text.Normalizer.normalize(text.map { map[it] ?: it }.joinToString(""), java.text.Normalizer.Form.NFD)
            .replace(aksanlar, "")
            .lowercase(java.util.Locale.ROOT)
            .replace(" ili", "")
            .replace(" province", "")
            .replace(" state", "")
            .replace(" governorate", "")
            .filter { it.isLetterOrDigit() }

    /** Önce Türkçe, sonra İngilizce adla eşleştirir (yurt dışında Android adları İngilizce döner). */
    fun bestMatchAny(names: List<String?>, places: List<Place>): Place? {
        for (n in names) {
            if (n.isNullOrBlank()) continue
            bestMatch(n, places)?.let { return it }
            bestMatch(n, places.filter { !it.nameEn.isNullOrBlank() }.map { it.copy(name = it.nameEn!!) })
                ?.let { en -> return places.first { it.id == en.id } }
        }
        return null
    }

    fun bestMatch(name: String?, places: List<Place>): Place? {
        if (name.isNullOrBlank()) return null
        val target = normalize(name)
        if (target.isEmpty()) return null
        places.firstOrNull { normalize(it.name) == target }?.let { return it }
        // "Kadıköy İlçesi", "Merkez" gibi farklılıklar için kısmi eşleşme
        return places.firstOrNull {
            val n = normalize(it.name)
            n.isNotEmpty() && (n.startsWith(target) || target.startsWith(n))
        }
    }

    /** İlçe bulunamazsa il merkezini (il ile aynı adlı ilçe) seçer. */
    fun center(state: Place, cities: List<Place>): Place? = bestMatch(state.name, cities) ?: cities.firstOrNull()
}
