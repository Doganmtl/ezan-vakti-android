package com.example.ezanvakti.model

import com.example.ezanvakti.i18n.t

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Kıble yönü hesabı (büyük daire üzerinde başlangıç açısı). */
object Kible {
    /** Kâbe'nin koordinatları. */
    const val KABE_ENLEM = 21.422487
    const val KABE_BOYLAM = 39.826206

    /** Coğrafi kuzeyden saat yönünde kıble açısı (0..360). */
    fun aci(enlem: Double, boylam: Double): Double {
        val f1 = Math.toRadians(enlem)
        val f2 = Math.toRadians(KABE_ENLEM)
        val dl = Math.toRadians(KABE_BOYLAM - boylam)
        val y = sin(dl) * cos(f2)
        val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Kâbe'ye kuş uçuşu uzaklık (km). */
    fun uzaklikKm(enlem: Double, boylam: Double): Double {
        val f1 = Math.toRadians(enlem)
        val f2 = Math.toRadians(KABE_ENLEM)
        val df = f2 - f1
        val dl = Math.toRadians(KABE_BOYLAM - boylam)
        val a = sin(df / 2) * sin(df / 2) + cos(f1) * cos(f2) * sin(dl / 2) * sin(dl / 2)
        return 2 * 6371.0 * asin(sqrt(a))
    }

    /** İki açı arasındaki en kısa fark (−180..180). Pozitif: hedef sağda. */
    fun fark(hedef: Double, mevcut: Double): Double = ((hedef - mevcut + 540.0) % 360.0) - 180.0

    /** "Güneydoğu" gibi yön adı. */
    fun yonAdi(aci: Double): String {
        val adlar = listOf(t("Kuzey"), t("Kuzeydoğu"), t("Doğu"), t("Güneydoğu"), t("Güney"), t("Güneybatı"), t("Batı"), t("Kuzeybatı"))
        return adlar[(((aci + 22.5) % 360) / 45).toInt()]
    }
}
