package com.example.ezanvakti.widget

import com.example.ezanvakti.i18n.t

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
import com.example.ezanvakti.data.Storage
import com.example.ezanvakti.ui.theme.ArkaPlan
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

        /** Ayarlardaki widget görünümüne göre arka plan. */
        private fun arkaPlan(context: Context, vakit: Vakit?): Int {
            val storage = Storage(context)
            return when (storage.widgetStili) {
                "seffaf" -> R.drawable.widget_bg_seffaf
                "tema" -> when (ArkaPlan.from(storage.arkaPlan)) {
                    ArkaPlan.VAKTE_GORE -> when (vakit ?: Vakit.OGLE) {
                        Vakit.IMSAK -> R.drawable.widget_bg_v_imsak
                        Vakit.GUNES -> R.drawable.widget_bg_v_gunes
                        Vakit.OGLE -> R.drawable.widget_bg_v_ogle
                        Vakit.IKINDI -> R.drawable.widget_bg_v_ikindi
                        Vakit.AKSAM -> R.drawable.widget_bg_v_aksam
                        Vakit.YATSI -> R.drawable.widget_bg_v_yatsi
                    }
                    ArkaPlan.GECE -> R.drawable.widget_bg_gece
                    ArkaPlan.ZUMRUT -> R.drawable.widget_bg_zumrut
                    ArkaPlan.MOR -> R.drawable.widget_bg_mor
                    ArkaPlan.GUN_BATIMI -> R.drawable.widget_bg_gunbatimi
                    ArkaPlan.DENIZ -> R.drawable.widget_bg_deniz
                    ArkaPlan.SIYAH -> R.drawable.widget_bg_siyah
                }
                else -> R.drawable.widget_bg
            }
        }

        /** Üç widget türünü (büyük, küçük, şerit) verilen vakitlerle çizer. Ramazan'da iftar/sahura sayar. */
        fun ciz(context: Context, days: List<DailyTimes>, location: SelectedLocation?, dini: DiniGunlerVerisi, now: Long) {
            val manager = AppWidgetManager.getInstance(context)
            val turler = listOf(
                VakitWidget::class.java to R.layout.widget_vakit,
                KucukWidget::class.java to R.layout.widget_kucuk,
                SeritWidget::class.java to R.layout.widget_serit,
            )
            val durum = VakitCalculator.durum(days, now)
            val sayim = GeriSayimHesabi.hesapla(days, now, dini)
            val bugun = VakitCalculator.today(days, now)
            val arka = arkaPlan(context, durum?.current)
            val ac = PendingIntent.getActivity(
                context, 501, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            for ((sinif, layout) in turler) {
                val ids = manager.getAppWidgetIds(ComponentName(context, sinif))
                if (ids.isEmpty()) continue
                val views = RemoteViews(context.packageName, layout)
                views.setOnClickPendingIntent(R.id.kok, ac)
                views.setInt(R.id.kok, "setBackgroundResource", arka)
                val buyuk = layout == R.layout.widget_vakit
                val kucuk = layout == R.layout.widget_kucuk

                if (location == null || durum == null || sayim == null) {
                    if (!kucuk) views.setTextViewText(R.id.yer, t("Ezan Vakti"))
                    views.setTextViewText(R.id.baslik, t("Vakitleri görmek için dokunun"))
                    views.setViewVisibility(R.id.kalan, View.GONE)
                    if (!layout.equals(R.layout.widget_serit)) views.setTextViewText(R.id.sonraki, "")
                    if (!kucuk) views.setViewVisibility(R.id.vakitler, View.GONE)
                    manager.updateAppWidget(ids, views)
                    continue
                }

                val ramazanEki = sayim.ramazanGunu?.let { t(" · Ramazan'ın %s. günü", it) } ?: ""
                if (!kucuk) views.setTextViewText(R.id.yer, location.title + ramazanEki)
                views.setTextViewText(R.id.baslik, sayim.baslik)
                views.setViewVisibility(R.id.kalan, View.VISIBLE)
                views.setChronometer(R.id.kalan, SystemClock.elapsedRealtime() + (sayim.hedef - now).coerceAtLeast(0), null, true)
                views.setChronometerCountDown(R.id.kalan, true)
                if (buyuk || kucuk) views.setTextViewText(R.id.sonraki, sayim.alt)

                if (!kucuk) {
                    if (bugun != null) {
                        views.setViewVisibility(R.id.vakitler, View.VISIBLE)
                        Vakit.entries.forEachIndexed { i, v ->
                            val simdi = v == durum.current
                            views.setTextViewText(adIdleri[i], v.adi)
                            views.setTextViewText(saatIdleri[i], VakitCalculator.clock(bugun.time(v)))
                            views.setTextColor(adIdleri[i], if (simdi) VURGU else SOLUK)
                            views.setTextColor(saatIdleri[i], if (simdi) VURGU else BEYAZ)
                        }
                    } else {
                        views.setViewVisibility(R.id.vakitler, View.GONE)
                    }
                }
                manager.updateAppWidget(ids, views)
            }
        }
    }
}

/** Küçük widget (2×1): sıradaki vakit ve geri sayım. Çizimi [VakitWidget.ciz] yapar. */
class KucukWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Tazeleyici.calistir(context)
    }
}

/** Şerit widget (4×1): günün altı vakti yan yana ve sıradaki vakte sayaç. Çizimi [VakitWidget.ciz] yapar. */
class SeritWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Tazeleyici.calistir(context)
    }
}
