package com.example.ezanvakti.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.ezanvakti.MainActivity
import com.example.ezanvakti.R
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.GeriSayimHesabi
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.notify.Tazeleyici

/**
 * Ana ekran widget'ı: konum, sıradaki vakit, canlı geri sayım ve günün altı vakti.
 * Geri sayımı sistemin Chronometer'ı yürütür; widget vakit değişince [Tazeleyici] ile yenilenir.
 */
class VakitWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Tazeleyici.calistir(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Tazeleyici.calistir(context)
        }
    }

    companion object {
        private val adIdleri = intArrayOf(R.id.ad0, R.id.ad1, R.id.ad2, R.id.ad3, R.id.ad4, R.id.ad5)
        private val saatIdleri = intArrayOf(R.id.saat0, R.id.saat1, R.id.saat2, R.id.saat3, R.id.saat4, R.id.saat5)
        private const val VURGU = 0xFF5EEAD4.toInt()
        private const val BEYAZ = 0xFFFFFFFF.toInt()
        private const val SOLUK = 0xCCFFFFFF.toInt()

        /** Widget'ı, kilit ekranı sayacını ve bildirim planını tazeler (bkz. [Tazeleyici]). */
        fun guncelle(context: Context) = Tazeleyici.calistir(context)

        /** Tüm widget'ları verilen vakitlerle çizer. Ramazan'da iftar/sahura sayar. */
        fun ciz(context: Context, days: List<DailyTimes>, location: SelectedLocation?, dini: DiniGunlerVerisi, now: Long) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VakitWidget::class.java))
            if (ids.isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.widget_vakit)
            val ac = PendingIntent.getActivity(
                context, 501, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.kok, ac)

            val durum = VakitCalculator.durum(days, now)
            val sayim = GeriSayimHesabi.hesapla(days, now, dini)
            val bugun = VakitCalculator.today(days, now)

            if (location == null || durum == null || sayim == null) {
                views.setTextViewText(R.id.yer, "Ezan Vakti")
                views.setTextViewText(R.id.baslik, "Vakitleri görmek için dokunun")
                views.setViewVisibility(R.id.kalan, View.GONE)
                views.setTextViewText(R.id.sonraki, "")
                views.setViewVisibility(R.id.vakitler, View.GONE)
                manager.updateAppWidget(ids, views)
                return
            }

            views.setTextViewText(
                R.id.yer,
                location.title + (sayim.ramazanGunu?.let { " · Ramazan'ın $it. günü" } ?: ""),
            )
            views.setTextViewText(R.id.baslik, sayim.baslik)
            views.setViewVisibility(R.id.kalan, View.VISIBLE)
            views.setChronometer(R.id.kalan, SystemClock.elapsedRealtime() + (sayim.hedef - now).coerceAtLeast(0), null, true)
            views.setChronometerCountDown(R.id.kalan, true)
            views.setTextViewText(R.id.sonraki, sayim.alt)

            if (bugun != null) {
                views.setViewVisibility(R.id.vakitler, View.VISIBLE)
                Vakit.entries.forEachIndexed { i, v ->
                    val simdi = v == durum.current
                    views.setTextViewText(adIdleri[i], v.ad)
                    views.setTextViewText(saatIdleri[i], VakitCalculator.clock(bugun.time(v)))
                    views.setTextColor(adIdleri[i], if (simdi) VURGU else SOLUK)
                    views.setTextColor(saatIdleri[i], if (simdi) VURGU else BEYAZ)
                }
            } else {
                views.setViewVisibility(R.id.vakitler, View.GONE)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
