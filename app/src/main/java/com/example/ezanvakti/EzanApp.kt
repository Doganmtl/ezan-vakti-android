package com.example.ezanvakti

import android.app.Application
import com.example.ezanvakti.i18n.Dil

/** Uygulama süreci başlarken dili belirler; bildirim, widget ve alarm alıcıları da aynı dili kullanır. */
class EzanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dil.baslat(this)
    }
}
