package com.example.ezanvakti.model

import com.example.ezanvakti.i18n.t

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
    private val resmi: Map<MonthDay, Mesaj> get() = mapOf(
        MonthDay.of(1, 1) to Mesaj(
            t("Yeni yılınız kutlu olsun"),
            t("Yeni bir yıla birlikte uyandık. Bu yıl sofranızdan bereket, evinizden huzur, sevdiklerinizden sağlık eksik olmasın."),
        ),
        // Resmî tatil değil; kullanıcı isteğiyle eklendi.
        MonthDay.of(3, 8) to Mesaj(
            t("Dünya Kadınlar Günü kutlu olsun"),
            t("Hayatımıza emek, sevgi ve güç katan annelerimizin, eşlerimizin, kızlarımızın, kardeşlerimizin ve tüm kadınların günü kutlu olsun. Hak ettikleri değeri her gün gördükleri bir dünya dileriz."),
        ),
        MonthDay.of(4, 23) to Mesaj(
            t("23 Nisan kutlu olsun"),
            t("Bu bayramı çocuklarımıza armağan eden Gazi Mustafa Kemal Atatürk'ü ve ilk Meclis'in kurucularını rahmet ve minnetle anıyoruz. Evinizin neşesi çocuklarınızın bayramı kutlu olsun."),
        ),
        MonthDay.of(5, 1) to Mesaj(
            t("Emek ve Dayanışma Günü"),
            t("Alın teriyle evine ekmek götüren herkesin emeğine sağlık. Emeğin hak ettiği değeri gördüğü güzel günler dileriz."),
        ),
        MonthDay.of(5, 19) to Mesaj(
            t("19 Mayıs kutlu olsun"),
            t("1919'da Samsun'a çıkarak bir milletin umudunu yeniden yeşerten Gazi Mustafa Kemal Atatürk'ü ve silah arkadaşlarını saygıyla anıyoruz. Gençliğimizin bayramı kutlu olsun."),
        ),
        MonthDay.of(7, 15) to Mesaj(
            t("Demokrasi ve Millî Birlik Günü"),
            t("O gece milletimizin iradesine sahip çıkarken canını veren şehitlerimizi rahmetle, gazilerimizi minnetle anıyoruz."),
        ),
        MonthDay.of(8, 30) to Mesaj(
            t("Zafer Bayramı kutlu olsun"),
            t("Bu vatanı bize emanet eden Başkomutan Gazi Mustafa Kemal Atatürk'ü, silah arkadaşlarını ve tüm şehitlerimizi rahmet ve minnetle anıyoruz. Zafer Bayramımız kutlu olsun."),
        ),
        MonthDay.of(10, 29) to Mesaj(
            t("Cumhuriyet Bayramımız kutlu olsun"),
            t("Cumhuriyetimizin kurucusu Gazi Mustafa Kemal Atatürk'ü, silah arkadaşlarını ve bu toprakları bize yurt eden tüm şehitlerimizi rahmet ve minnetle anıyoruz. Nice yıllara, Cumhuriyet!"),
        ),
    )

    private fun dini(gun: DiniGun): Mesaj? = when {
        gun.ad.startsWith("Ramazan Bayramı 1") -> Mesaj(
            t("Ramazan Bayramınız mübarek olsun"),
            t("Bir ay boyunca tutulan oruçların, kılınan namazların ve uzanan yardım ellerinin bereketi bayramınıza da yansısın. Küslerin barıştığı, büyüklerin elinin öpüldüğü, sofraların paylaşıldığı güzel bir bayram geçirmenizi dileriz."),
        )
        gun.ad.startsWith("Kurban Bayramı 1") -> Mesaj(
            t("Kurban Bayramınız mübarek olsun"),
            t("Paylaştıkça çoğalan bir bayram dileriz. Kurbanlarınız kabul, sofralarınız bereketli, gönülleriniz huzurlu olsun. Sevdiklerinizle nice bayramlara."),
        )
        gun.ad.startsWith("Regaib") -> Mesaj(
            t("Regaib Kandiliniz mübarek olsun"),
            t("Üç ayların müjdecisi bu gece, gönüllerimize huzur, evlerimize bereket getirsin. Dualarınızın kabul olduğu, sevdiklerinizle birlikte nice mübarek gecelere."),
        )
        gun.ad.startsWith("Miraç") -> Mesaj(
            t("Miraç Kandiliniz mübarek olsun"),
            t("Bu mübarek gecede gönlünüz huzurla, eviniz rahmetle dolsun. Ettiğiniz dualar kabul, açtığınız eller boş dönmesin."),
        )
        gun.ad.startsWith("Berat") -> Mesaj(
            t("Berat Kandiliniz mübarek olsun"),
            t("Af ve bağışlanma dilediğimiz bu gecede kalplerimiz arınsın, dualarımız kabul olsun. Sevdiklerinizle birlikte huzurlu nice gecelere."),
        )
        gun.ad.startsWith("Kadir") -> Mesaj(
            t("Kadir Geceniz mübarek olsun"),
            t("Ramazan'ın en kıymetli gecesinde dualarınız kabul, gönlünüz huzurlu olsun. Bu gecenin rahmeti üzerinizden eksik olmasın."),
        )
        gun.ad.startsWith("Mevlid") -> Mesaj(
            t("Mevlid Kandiliniz mübarek olsun"),
            t("Sevgili Peygamberimizin (s.a.v.) dünyayı şereflendirdiği bu gece, gönüllerimize sevgi ve merhamet getirsin. Mübarek geceniz hayırlara vesile olsun."),
        )
        else -> null
    }

    /**
     * O günün tebrik mesajı; yoksa null. Resmî günler (ve 8 Mart) yalnızca Türkiye'deki konumlar için;
     * dini bayram ve kandil tebrikleri her ülkede, o ülkenin hicri takvimine göre gönderilir.
     */
    fun gunun(tarih: LocalDate, dini: DiniGunlerVerisi, turkiye: Boolean = true): Mesaj? {
        val resmiMesaj = if (turkiye) resmi[MonthDay.from(tarih)] else null
        val mesajlar = listOfNotNull(resmiMesaj) + dini.on(tarih).mapNotNull(::dini)
        if (mesajlar.isEmpty()) return null
        if (mesajlar.size == 1) return mesajlar.first()
        return Mesaj(mesajlar.joinToString(" · ") { it.baslik }, mesajlar.joinToString("\n\n") { it.metin })
    }
}
