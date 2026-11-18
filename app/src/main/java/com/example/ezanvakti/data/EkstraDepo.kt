package com.example.ezanvakti.data

import android.content.Context
import com.example.ezanvakti.model.ZikirModu

/** Zikirmatik ve kaza takibinin kalıcı kayıtları (uygulama kapansa da korunur). */
class EkstraDepo(context: Context) {
    private val prefs = context.getSharedPreferences("ezan_ekstra", Context.MODE_PRIVATE)

    // ---------- Zikirmatik ----------

    var zikirSayac: Int
        get() = prefs.getInt("zikir_sayac", 0)
        set(v) = prefs.edit().putInt("zikir_sayac", v).apply()

    /** 0 = hedefsiz. */
    var zikirHedef: Int
        get() = prefs.getInt("zikir_hedef", 33)
        set(v) = prefs.edit().putInt("zikir_hedef", v).apply()

    var zikirAdi: String
        get() = prefs.getString("zikir_adi", "Sübhânallah") ?: "Sübhânallah"
        set(v) = prefs.edit().putString("zikir_adi", v).apply()

    var zikirModu: ZikirModu
        get() = runCatching { ZikirModu.valueOf(prefs.getString("zikir_modu", null) ?: "") }.getOrDefault(ZikirModu.SERBEST)
        set(v) = prefs.edit().putString("zikir_modu", v.name).apply()

    /** Tesbihat modunda hangi adımda olunduğu (0..2; 3 = tamamlandı). */
    var tesbihatAdim: Int
        get() = prefs.getInt("tesbihat_adim", 0)
        set(v) = prefs.edit().putInt("tesbihat_adim", v).apply()

    /** Tüm zamanların toplam zikir sayısı. */
    var zikirToplam: Long
        get() = prefs.getLong("zikir_toplam", 0)
        set(v) = prefs.edit().putLong("zikir_toplam", v).apply()

    var zikirTitresim: Boolean
        get() = prefs.getBoolean("zikir_titresim", true)
        set(v) = prefs.edit().putBoolean("zikir_titresim", v).apply()

    var zikirSesTusu: Boolean
        get() = prefs.getBoolean("zikir_ses_tusu", true)
        set(v) = prefs.edit().putBoolean("zikir_ses_tusu", v).apply()

    // ---------- Kaza takibi ----------

    fun kaza(anahtar: String): Int = prefs.getInt("kaza_$anahtar", 0)

    fun setKaza(anahtar: String, deger: Int) {
        prefs.edit().putInt("kaza_$anahtar", deger.coerceAtLeast(0)).apply()
    }
}
