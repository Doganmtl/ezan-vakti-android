package com.example.ezanvakti.model

/**
 * Widget'ın ve kilit ekranındaki canlı sayacın gösterdiği geri sayım.
 * Ramazan'da iftar ya da sahura (imsak), diğer günlerde sıradaki vakte sayar.
 */
data class GeriSayim(
    val baslik: String,     // "İkindi vaktine", "İftara"
    val hedef: Long,        // epoch ms
    val alt: String,        // "İkindi 15:14"
    val ramazanGunu: Int?,  // Ramazan'daysa kaçıncı gün
)

object GeriSayimHesabi {
    fun hesapla(days: List<DailyTimes>, now: Long, dini: DiniGunlerVerisi): GeriSayim? {
        RamazanHesabi.durum(days, now, dini)?.let { r ->
            val saat = VakitCalculator.clock(r.hedefZamani)
            return if (r.hedef == RamazanHesabi.Hedef.IFTAR) {
                GeriSayim("İftara", r.hedefZamani, "İftar $saat", r.gun.gun)
            } else {
                GeriSayim("Sahura (imsaka)", r.hedefZamani, "İmsak $saat", r.gun.gun)
            }
        }
        val d = VakitCalculator.durum(days, now) ?: return null
        val baslik = if (d.next == Vakit.GUNES) "Güneşin doğmasına" else "${d.next.ad} vaktine"
        return GeriSayim(baslik, d.nextTime, "${d.next.ad} ${VakitCalculator.clock(d.nextTime)}", null)
    }
}
