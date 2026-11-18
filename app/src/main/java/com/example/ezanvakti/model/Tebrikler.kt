package com.example.ezanvakti.model

import java.time.LocalDate
import java.time.MonthDay

/**
 * Resmî bayram/tatil, dini bayram (ilk gün) ve kandil günlerinde sabah gönderilen tebrik mesajları.
 * Metinler kullanıcıyla birlikte belirlendi (18.11.2026). Ayet ya da hadis içermez.
 * Aynı güne birden fazla gün denk gelirse tek bildirimde birleştirilir.
 */
object Tebrikler {
    data class Mesaj(val baslik: String, val metin: String)

    /** 2429 sayılı Kanun'daki sabit tarihli resmî bayram ve genel tatiller. */
    private val resmi: Map<MonthDay, Mesaj> = mapOf(
        MonthDay.of(1, 1) to Mesaj(
            "Yeni yılınız kutlu olsun",
            "Yeni bir yıla birlikte uyandık. Bu yıl sofranızdan bereket, evinizden huzur, sevdiklerinizden sağlık eksik olmasın.",
        ),
        // Resmî tatil değil; kullanıcı isteğiyle eklendi.
        MonthDay.of(3, 8) to Mesaj(
            "Dünya Kadınlar Günü kutlu olsun",
            "Hayatımıza emek, sevgi ve güç katan annelerimizin, eşlerimizin, kızlarımızın, kardeşlerimizin ve tüm kadınların günü kutlu olsun. Hak ettikleri değeri her gün gördükleri bir dünya dileriz.",
        ),
        MonthDay.of(4, 23) to Mesaj(
            "23 Nisan kutlu olsun",
            "Bu bayramı çocuklarımıza armağan eden Gazi Mustafa Kemal Atatürk'ü ve ilk Meclis'in kurucularını rahmet ve minnetle anıyoruz. Evinizin neşesi çocuklarınızın bayramı kutlu olsun.",
        ),
        MonthDay.of(5, 1) to Mesaj(
            "Emek ve Dayanışma Günü",
            "Alın teriyle evine ekmek götüren herkesin emeğine sağlık. Emeğin hak ettiği değeri gördüğü güzel günler dileriz.",
        ),
        MonthDay.of(5, 19) to Mesaj(
            "19 Mayıs kutlu olsun",
            "1919'da Samsun'a çıkarak bir milletin umudunu yeniden yeşerten Gazi Mustafa Kemal Atatürk'ü ve silah arkadaşlarını saygıyla anıyoruz. Gençliğimizin bayramı kutlu olsun.",
        ),
        MonthDay.of(7, 15) to Mesaj(
            "Demokrasi ve Millî Birlik Günü",
            "O gece milletimizin iradesine sahip çıkarken canını veren şehitlerimizi rahmetle, gazilerimizi minnetle anıyoruz.",
        ),
        MonthDay.of(8, 30) to Mesaj(
            "Zafer Bayramı kutlu olsun",
            "Bu vatanı bize emanet eden Başkomutan Gazi Mustafa Kemal Atatürk'ü, silah arkadaşlarını ve tüm şehitlerimizi rahmet ve minnetle anıyoruz. Zafer Bayramımız kutlu olsun.",
        ),
        MonthDay.of(10, 29) to Mesaj(
            "Cumhuriyet Bayramımız kutlu olsun",
            "Cumhuriyetimizin kurucusu Gazi Mustafa Kemal Atatürk'ü, silah arkadaşlarını ve bu toprakları bize yurt eden tüm şehitlerimizi rahmet ve minnetle anıyoruz. Nice yıllara, Cumhuriyet!",
        ),
    )

    private fun dini(gun: DiniGun): Mesaj? = when {
        gun.ad.startsWith("Ramazan Bayramı 1") -> Mesaj(
            "Ramazan Bayramınız mübarek olsun",
            "Bir ay boyunca tutulan oruçların, kılınan namazların ve uzanan yardım ellerinin bereketi bayramınıza da yansısın. Küslerin barıştığı, büyüklerin elinin öpüldüğü, sofraların paylaşıldığı güzel bir bayram geçirmenizi dileriz.",
        )
        gun.ad.startsWith("Kurban Bayramı 1") -> Mesaj(
            "Kurban Bayramınız mübarek olsun",
            "Paylaştıkça çoğalan bir bayram dileriz. Kurbanlarınız kabul, sofralarınız bereketli, gönülleriniz huzurlu olsun. Sevdiklerinizle nice bayramlara.",
        )
        gun.ad.startsWith("Regaib") -> Mesaj(
            "Regaib Kandiliniz mübarek olsun",
            "Üç ayların müjdecisi bu gece, gönüllerimize huzur, evlerimize bereket getirsin. Dualarınızın kabul olduğu, sevdiklerinizle birlikte nice mübarek gecelere.",
        )
        gun.ad.startsWith("Miraç") -> Mesaj(
            "Miraç Kandiliniz mübarek olsun",
            "Bu mübarek gecede gönlünüz huzurla, eviniz rahmetle dolsun. Ettiğiniz dualar kabul, açtığınız eller boş dönmesin.",
        )
        gun.ad.startsWith("Berat") -> Mesaj(
            "Berat Kandiliniz mübarek olsun",
            "Af ve bağışlanma dilediğimiz bu gecede kalplerimiz arınsın, dualarımız kabul olsun. Sevdiklerinizle birlikte huzurlu nice gecelere.",
        )
        gun.ad.startsWith("Kadir") -> Mesaj(
            "Kadir Geceniz mübarek olsun",
            "Ramazan'ın en kıymetli gecesinde dualarınız kabul, gönlünüz huzurlu olsun. Bu gecenin rahmeti üzerinizden eksik olmasın.",
        )
        gun.ad.startsWith("Mevlid") -> Mesaj(
            "Mevlid Kandiliniz mübarek olsun",
            "Sevgili Peygamberimizin (s.a.v.) dünyayı şereflendirdiği bu gece, gönüllerimize sevgi ve merhamet getirsin. Mübarek geceniz hayırlara vesile olsun.",
        )
        else -> null
    }

    /** O günün tebrik mesajı; yoksa null. */
    fun gunun(tarih: LocalDate, dini: DiniGunlerVerisi): Mesaj? {
        val mesajlar = listOfNotNull(resmi[MonthDay.from(tarih)]) + dini.on(tarih).mapNotNull(::dini)
        if (mesajlar.isEmpty()) return null
        if (mesajlar.size == 1) return mesajlar.first()
        return Mesaj(mesajlar.joinToString(" · ") { it.baslik }, mesajlar.joinToString("\n\n") { it.metin })
    }
}
