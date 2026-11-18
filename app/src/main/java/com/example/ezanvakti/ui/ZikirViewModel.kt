package com.example.ezanvakti.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.example.ezanvakti.data.EkstraDepo
import com.example.ezanvakti.model.Tesbihat
import com.example.ezanvakti.model.ZikirModu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ZikirDurumu(
    val sayac: Int,
    val hedef: Int,          // 0 = hedefsiz
    val ad: String,
    val mod: ZikirModu,
    val tesbihatAdim: Int,   // 0..2, 3 = tamamlandı
    val toplam: Long,
    val titresim: Boolean,
    val sesTusu: Boolean,
) {
    val tesbihatBitti: Boolean get() = mod == ZikirModu.TESBIHAT && tesbihatAdim >= Tesbihat.adimlar.size

    /** Ekranda gösterilen zikir adı. */
    val gosterilenAd: String
        get() = if (mod == ZikirModu.TESBIHAT) Tesbihat.adimlar.getOrNull(tesbihatAdim)?.ad ?: "Tesbihat tamamlandı" else ad

    val gecerliHedef: Int
        get() = if (mod == ZikirModu.TESBIHAT) Tesbihat.adimlar.getOrNull(tesbihatAdim)?.hedef ?: 33 else hedef

    /** Hedefli serbest zikirde kaçıncı turda olunduğu (1'den başlar). */
    val tur: Int get() = if (mod == ZikirModu.SERBEST && hedef > 0) sayac / hedef + 1 else 1

    /** Halkadaki ilerleme (0..1). */
    val ilerleme: Float
        get() {
            val h = gecerliHedef
            if (h <= 0) return 0f
            if (tesbihatBitti) return 1f
            val icinde = if (mod == ZikirModu.SERBEST) sayac % h else sayac
            return icinde.toFloat() / h
        }
}

/** Zikirmatik durumu; kayıtlar [EkstraDepo]'da tutulur, ekran kapansa da kaybolmaz. */
class ZikirViewModel(app: Application) : AndroidViewModel(app) {
    private val depo = EkstraDepo(app)

    private val _durum = MutableStateFlow(oku())
    val durum: StateFlow<ZikirDurumu> = _durum.asStateFlow()

    private fun oku() = ZikirDurumu(
        sayac = depo.zikirSayac,
        hedef = depo.zikirHedef,
        ad = depo.zikirAdi,
        mod = depo.zikirModu,
        tesbihatAdim = depo.tesbihatAdim,
        toplam = depo.zikirToplam,
        titresim = depo.zikirTitresim,
        sesTusu = depo.zikirSesTusu,
    )

    private fun kaydet(d: ZikirDurumu) {
        depo.zikirSayac = d.sayac
        depo.zikirHedef = d.hedef
        depo.zikirAdi = d.ad
        depo.zikirModu = d.mod
        depo.tesbihatAdim = d.tesbihatAdim
        depo.zikirToplam = d.toplam
        depo.zikirTitresim = d.titresim
        depo.zikirSesTusu = d.sesTusu
        _durum.value = d
    }

    /** Bir zikir sayar; hedefe ulaşıldıysa uzun, değilse kısa titreşim. */
    fun say() {
        val d = _durum.value
        if (d.tesbihatBitti) return
        val yeni = d.sayac + 1
        var sonraki = d.copy(sayac = yeni, toplam = d.toplam + 1)
        var hedefeUlasti = false
        when (d.mod) {
            ZikirModu.SERBEST -> hedefeUlasti = d.hedef > 0 && yeni % d.hedef == 0
            ZikirModu.TESBIHAT -> if (yeni >= d.gecerliHedef) {
                hedefeUlasti = true
                sonraki = sonraki.copy(sayac = 0, tesbihatAdim = d.tesbihatAdim + 1)
            }
        }
        kaydet(sonraki)
        if (d.titresim) {
            if (hedefeUlasti) Titresim.uzun(getApplication()) else Titresim.kisa(getApplication())
        }
    }

    /** Yanlışlıkla fazla sayılanı geri alır. */
    fun geriAl() {
        val d = _durum.value
        if (d.sayac > 0) kaydet(d.copy(sayac = d.sayac - 1, toplam = (d.toplam - 1).coerceAtLeast(0)))
    }

    fun sifirla() {
        val d = _durum.value
        kaydet(d.copy(sayac = 0, tesbihatAdim = 0))
    }

    fun setHedef(hedef: Int) = kaydet(_durum.value.copy(hedef = hedef.coerceAtLeast(0)))

    fun setMod(mod: ZikirModu) {
        val d = _durum.value
        if (d.mod == mod) return
        kaydet(d.copy(mod = mod, sayac = 0, tesbihatAdim = 0))
    }

    /** Serbest modda yeni bir zikre sıfırdan başlar. */
    fun zikreBasla(ad: String, hedef: Int? = null) {
        val d = _durum.value
        kaydet(d.copy(ad = ad, mod = ZikirModu.SERBEST, sayac = 0, tesbihatAdim = 0, hedef = hedef ?: d.hedef))
    }

    fun setTitresim(acik: Boolean) = kaydet(_durum.value.copy(titresim = acik))
    fun setSesTusu(acik: Boolean) = kaydet(_durum.value.copy(sesTusu = acik))
}

/** Kısa ve uzun titreşim. */
object Titresim {
    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun kisa(context: Context) {
        vibrator(context)?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun uzun(context: Context) {
        vibrator(context)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 120, 300), -1))
    }
}

/**
 * Ses tuşlarıyla sayma: Zikir ekranı açıkken [dinleyici] atanır,
 * MainActivity ses tuşu olaylarını buraya yönlendirir.
 */
object ZikirTuslari {
    @Volatile
    var dinleyici: (() -> Unit)? = null
}
