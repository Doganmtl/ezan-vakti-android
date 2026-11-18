package com.example.ezanvakti.data

import com.example.ezanvakti.model.Place

/** Android'in döndürdüğü yer adlarını ("İstanbul", "Kadıköy") Diyanet listesiyle ("İSTANBUL", "KADIKÖY") eşleştirir. */
object PlaceMatcher {
    private val map = mapOf(
        'ı' to 'i', 'İ' to 'i', 'I' to 'i', 'ş' to 's', 'Ş' to 's', 'ğ' to 'g', 'Ğ' to 'g',
        'ü' to 'u', 'Ü' to 'u', 'ö' to 'o', 'Ö' to 'o', 'ç' to 'c', 'Ç' to 'c',
        'â' to 'a', 'Â' to 'a', 'î' to 'i', 'Î' to 'i', 'û' to 'u', 'Û' to 'u',
    )

    fun normalize(text: String): String =
        text.map { map[it] ?: it }.joinToString("")
            .lowercase(java.util.Locale.ROOT)
            .replace(" ili", "")
            .replace(" province", "")
            .filter { it.isLetterOrDigit() }

    fun bestMatch(name: String?, places: List<Place>): Place? {
        if (name.isNullOrBlank()) return null
        val target = normalize(name)
        places.firstOrNull { normalize(it.name) == target }?.let { return it }
        // "Kadıköy İlçesi", "Merkez" gibi farklılıklar için kısmi eşleşme
        return places.firstOrNull {
            val n = normalize(it.name)
            n.startsWith(target) || target.startsWith(n)
        }
    }

    /** İlçe bulunamazsa il merkezini (il ile aynı adlı ilçe) seçer. */
    fun center(state: Place, cities: List<Place>): Place? = bestMatch(state.name, cities) ?: cities.firstOrNull()
}
