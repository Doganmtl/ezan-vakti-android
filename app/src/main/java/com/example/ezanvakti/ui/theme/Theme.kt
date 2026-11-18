package com.example.ezanvakti.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ezanvakti.model.Vakit

val Teal = Color(0xFF0D8C73)

private val LightColors = lightColorScheme(primary = Teal, secondary = Color(0xFF268C8C))
private val DarkColors = darkColorScheme(primary = Color(0xFF4FD1B5), secondary = Color(0xFF6CC5C5))

@Composable
fun EzanVaktiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

/** Vakte göre arka plan degradesi (üst, alt). */
fun vakitColors(vakit: Vakit): Pair<Color, Color> = when (vakit) {
    Vakit.IMSAK -> Color(0xFF1A1F54) to Color(0xFF5C458C)
    Vakit.GUNES -> Color(0xFFF28C59) to Color(0xFF8C5999)
    Vakit.OGLE -> Color(0xFF2980D9) to Color(0xFF1AB3BF)
    Vakit.IKINDI -> Color(0xFFD99940) to Color(0xFF268C8C)
    Vakit.AKSAM -> Color(0xFFE65940) to Color(0xFF592E73)
    Vakit.YATSI -> Color(0xFF0A0F2E) to Color(0xFF1F2959)
}

/** Ana ekranın arka plan teması (Ayarlar → Görünüm). Kod değerleri ayarlarda saklanır. */
enum class ArkaPlan(val kod: String, val ad: String) {
    VAKTE_GORE("vakit", "Vakte göre"),
    GECE("gece", "Gece mavisi"),
    ZUMRUT("zumrut", "Zümrüt"),
    MOR("mor", "Mor"),
    GUN_BATIMI("gunbatimi", "Gün batımı"),
    DENIZ("deniz", "Deniz"),
    SIYAH("siyah", "Siyah (OLED)");

    companion object {
        fun from(kod: String?) = entries.firstOrNull { it.kod == kod } ?: VAKTE_GORE
    }
}

/** Seçilen temaya göre arka plan degradesi (üst, alt). "Vakte göre" seçiliyse vakte göre değişir. */
fun arkaPlanRenkleri(tema: ArkaPlan, vakit: Vakit): Pair<Color, Color> = when (tema) {
    ArkaPlan.VAKTE_GORE -> vakitColors(vakit)
    ArkaPlan.GECE -> Color(0xFF0B1437) to Color(0xFF233A7A)
    ArkaPlan.ZUMRUT -> Color(0xFF064E3B) to Color(0xFF0F8A6A)
    ArkaPlan.MOR -> Color(0xFF2E1065) to Color(0xFF7C3AED)
    ArkaPlan.GUN_BATIMI -> Color(0xFFB45309) to Color(0xFF9D174D)
    ArkaPlan.DENIZ -> Color(0xFF075985) to Color(0xFF0E7490)
    ArkaPlan.SIYAH -> Color(0xFF000000) to Color(0xFF0A0A0A)
}

/** Vakitlerin ana ekrandaki dizilişi. */
enum class VakitDuzeni(val kod: String, val ad: String, val sutun: Int) {
    LISTE("liste", "Alt alta", 1),
    IKILI("ikili", "İkili", 2),
    UCLU("uclu", "Üçlü", 3);

    companion object {
        fun from(kod: String?) = entries.firstOrNull { it.kod == kod } ?: LISTE
    }
}
