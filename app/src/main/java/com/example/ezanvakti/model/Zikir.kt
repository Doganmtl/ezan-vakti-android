package com.example.ezanvakti.model

/** Zikirmatiğin çalışma biçimi. */
enum class ZikirModu { SERBEST, TESBIHAT }

/** Namaz sonrası tesbihatın üç adımı (her biri 33 kez). */
object Tesbihat {
    data class Adim(val ad: String, val anlam: String, val hedef: Int = 33)

    val adimlar = listOf(
        Adim("Sübhânallah", "Allah her türlü eksiklikten münezzehtir"),
        Adim("Elhamdülillah", "Hamd Allah'a mahsustur"),
        Adim("Allâhu Ekber", "Allah en büyüktür"),
    )
}

/** Serbest zikirde hızlı seçim için hazır zikirler. */
val hazirZikirler = listOf(
    "Sübhânallah",
    "Elhamdülillah",
    "Allâhu Ekber",
    "Lâ ilâhe illallah",
    "Estağfirullah",
    "Salavat-ı şerîfe",
)

/** Zikirmatiğin hızlı hedefleri; 0 = hedefsiz (serbest sayım). */
val hazirHedefler = listOf(33, 99, 100, 0)
