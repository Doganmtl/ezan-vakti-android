package com.example.ezanvakti.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon yeniden başlayınca (ya da uygulama güncellenince) alarmlar silinir; kayıtlı vakitlerle yeniden kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Tazeleyici.calistir(context, planlariDaKur = true)
    }
}
