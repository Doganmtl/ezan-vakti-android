package com.example.ezanvakti.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ezanvakti.location.LocationResolver
import com.example.ezanvakti.model.Kible
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Yesil = Color(0xFF6EE7A8)
private const val HIZALI_DERECE = 3.0

private data class KibleKonumu(val enlem: Double, val boylam: Double, val kaynak: String)

@Composable
fun KibleEkrani(yerAdi: String?) {
    val context = LocalContext.current
    var konum by remember { mutableStateOf<KibleKonumu?>(null) }
    var konumHata by remember { mutableStateOf<String?>(null) }
    var deneme by remember { mutableIntStateOf(0) }

    LaunchedEffect(deneme) {
        konumHata = null
        val resolver = LocationResolver(context)
        val loc = resolver.koordinat()
        konum = when {
            loc != null -> KibleKonumu(loc.latitude, loc.longitude, "Bulunduğunuz konum")
            yerAdi != null -> resolver.adresKoordinati(yerAdi)?.let { (en, boy) -> KibleKonumu(en, boy, "$yerAdi (yaklaşık)") }
            else -> null
        }
        if (konum == null) konumHata = "Konum alınamadı. Konum servisini açıp tekrar deneyin."
    }

    val pusula = rememberPusula()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val k = konum
            when {
                k == null && konumHata == null -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(top = 60.dp))
                    Text("Konumunuz bulunuyor…")
                }
                k == null -> {
                    Text("🧭", fontSize = 48.sp, modifier = Modifier.padding(top = 40.dp))
                    Text(konumHata.orEmpty(), textAlign = TextAlign.Center)
                    TextButton(onClick = { deneme++ }) { Text("Tekrar dene", color = Color.White) }
                }
                else -> {
                    val kibleAcisi = Kible.aci(k.enlem, k.boylam)
                    // Manyetik kuzeyden coğrafi kuzeye düzeltme (Türkiye'de ~5-6°)
                    val sapma = remember(k) {
                        GeomagneticField(k.enlem.toFloat(), k.boylam.toFloat(), 0f, System.currentTimeMillis()).declination
                    }
                    val yon = pusula.azimut?.let { (it + sapma + 360f) % 360f }
                    val fark = yon?.let { Kible.fark(kibleAcisi, it.toDouble()) }
                    val hizali = fark != null && abs(fark) <= HIZALI_DERECE

                    // Hizalanınca bir kez titret
                    var oncekiHizali by remember { mutableStateOf(false) }
                    LaunchedEffect(hizali) {
                        if (hizali && !oncekiHizali) Titresim.kisa(context)
                        oncekiHizali = hizali
                    }

                    Text(
                        when {
                            yon == null -> "Kıble ${kibleAcisi.roundToInt()}°"
                            hizali -> "Kıbleye yöneldiniz ✓"
                            fark!! > 0 -> "${abs(fark).roundToInt()}° sağa dönün"
                            else -> "${abs(fark!!).roundToInt()}° sola dönün"
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hizali) Yesil else Color.White,
                    )

                    Pusula(yon = yon, kibleAcisi = kibleAcisi.toFloat(), hizali = hizali)

                    Column(
                        Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Kıble açısı: ${kibleAcisi.roundToInt()}° (${Kible.yonAdi(kibleAcisi)}, kuzeyden saat yönünde)", fontSize = 15.sp)
                        Text("Kâbe'ye uzaklık: ${"%,d".format(Kible.uzaklikKm(k.enlem, k.boylam).roundToInt()).replace(',', '.')} km", fontSize = 15.sp)
                        Text("Konum: ${k.kaynak}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                    }

                    when {
                        !pusula.sensorVar -> UyariKutusu(
                            "Bu cihazda pusula sensörü yok. Kıble, kuzeyden saat yönünde ${kibleAcisi.roundToInt()}° açıdadır; " +
                                "kuzeyi başka bir pusulayla bulabilirsiniz."
                        )
                        pusula.dusukDogruluk -> UyariKutusu(
                            "Pusula kalibre değil. Telefonu havada birkaç kez 8 çizerek hareket ettirin ve " +
                                "mıknatıs, hoparlör, metal eşyalardan uzak tutun."
                        )
                        else -> Text(
                            "Telefonu yere paralel tutun. Mıknatıslı kılıf ve metal eşyalar pusulayı saptırabilir.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UyariKutusu(metin: String) {
    Text(
        "⚠️ $metin",
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFC857).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    )
}

@Composable
private fun Pusula(yon: Float?, kibleAcisi: Float, hizali: Boolean) {
    val olcer = rememberTextMeasurer()
    val telefonYonu = yon ?: 0f
    Box(
        Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 340.dp)
            .aspectRatio(1f)
            .semantics {
                contentDescription = if (yon == null) "Pusula hazırlanıyor" else "Pusula. Telefon ${yon.roundToInt()} derece yönünde, kıble ${kibleAcisi.roundToInt()} derece."
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.White.copy(alpha = 0.10f), r, c)
            drawCircle(if (hizali) Yesil else Color.White.copy(alpha = 0.35f), r - 2.dp.toPx(), c, style = Stroke(3.dp.toPx()))

            // Kadran telefonla birlikte döner: kuzey, telefonun baktığı yönün tersine çevrilir
            rotate(-telefonYonu, c) {
                for (d in 0 until 360 step 15) {
                    val buyuk = d % 90 == 0
                    val a = Math.toRadians(d.toDouble() - 90)
                    val r1 = r - 8.dp.toPx()
                    val r2 = r1 - (if (buyuk) 14.dp else 7.dp).toPx()
                    drawLine(
                        Color.White.copy(alpha = if (buyuk) 0.9f else 0.45f),
                        Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                        Offset(c.x + (r2 * cos(a)).toFloat(), c.y + (r2 * sin(a)).toFloat()),
                        strokeWidth = (if (buyuk) 3.dp else 1.5.dp).toPx(),
                    )
                }
                listOf("K" to 0, "D" to 90, "G" to 180, "B" to 270).forEach { (harf, d) ->
                    val a = Math.toRadians(d.toDouble() - 90)
                    val rr = r - 40.dp.toPx()
                    val olcum = olcer.measure(
                        harf,
                        TextStyle(color = if (harf == "K") Color(0xFFFF8A80) else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    )
                    val p = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
                    // Harfler okunur kalsın diye kendi merkezlerinde geri döndürülür
                    rotate(telefonYonu + 0f, p) {
                        drawText(olcum, topLeft = Offset(p.x - olcum.size.width / 2, p.y - olcum.size.height / 2))
                    }
                }

                // Kıble oku: merkezden Kâbe yönüne
                val ka = Math.toRadians(kibleAcisi.toDouble() - 90)
                val uc = Offset(c.x + ((r - 58.dp.toPx()) * cos(ka)).toFloat(), c.y + ((r - 58.dp.toPx()) * sin(ka)).toFloat())
                drawLine(if (hizali) Yesil else Color(0xFFFFC857), c, uc, strokeWidth = 6.dp.toPx())
                val kabe = olcer.measure("🕋", TextStyle(fontSize = 30.sp))
                val kp = Offset(c.x + ((r - 34.dp.toPx()) * cos(ka)).toFloat(), c.y + ((r - 34.dp.toPx()) * sin(ka)).toFloat())
                rotate(telefonYonu, kp) {
                    drawText(kabe, topLeft = Offset(kp.x - kabe.size.width / 2, kp.y - kabe.size.height / 2))
                }
            }

            // Sabit işaret: telefonun üst kenarının baktığı yön
            val ucgen = Path().apply {
                moveTo(c.x, c.y - r - 2.dp.toPx())
                lineTo(c.x - 10.dp.toPx(), c.y - r + 16.dp.toPx())
                lineTo(c.x + 10.dp.toPx(), c.y - r + 16.dp.toPx())
                close()
            }
            drawPath(ucgen, if (hizali) Yesil else Color.White)
            drawCircle(Color.White, 6.dp.toPx(), c)
        }
    }
}

/** Pusula sensörünün anlık durumu. */
private class PusulaDurumu {
    var azimut by mutableStateOf<Float?>(null)          // manyetik kuzeyden, derece
    var dusukDogruluk by mutableStateOf(false)
    var sensorVar by mutableStateOf(true)
}

@Composable
private fun rememberPusula(): PusulaDurumu {
    val context = LocalContext.current
    val view = LocalView.current
    val durum = remember { PusulaDurumu() }
    var yumusakSin by remember { mutableFloatStateOf(0f) }
    var yumusakCos by remember { mutableFloatStateOf(1f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotasyon = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val ivme = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val manyetik = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (rotasyon == null && (ivme == null || manyetik == null)) {
            durum.sensorVar = false
            return@DisposableEffect onDispose { }
        }
        view.keepScreenOn = true

        val r = FloatArray(9)
        val rr = FloatArray(9)
        val o = FloatArray(3)
        var sonIvme: FloatArray? = null

        fun guncelle(matris: FloatArray) {
            // Ekran döndürülmüşse eksenleri ekrana göre düzelt
            val (x, y) = when (view.display?.rotation ?: Surface.ROTATION_0) {
                Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            }
            SensorManager.remapCoordinateSystem(matris, x, y, rr)
            SensorManager.getOrientation(rr, o)
            val aci = o[0]   // radyan, −π..π
            // Titremeyi azaltmak için açıyı sin/cos üzerinden yumuşat
            val k = 0.15f
            yumusakSin += k * (sin(aci) - yumusakSin)
            yumusakCos += k * (cos(aci) - yumusakCos)
            durum.azimut = ((Math.toDegrees(atan2(yumusakSin, yumusakCos).toDouble()) + 360.0) % 360.0).toFloat()
        }

        val dinleyici = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(r, e.values)
                        guncelle(r)
                    }
                    Sensor.TYPE_ACCELEROMETER -> sonIvme = e.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (rotasyon == null) {
                            val a = sonIvme ?: return
                            if (SensorManager.getRotationMatrix(r, null, a, e.values)) guncelle(r)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    durum.dusukDogruluk = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                        accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }

        rotasyon?.let { sm.registerListener(dinleyici, it, SensorManager.SENSOR_DELAY_UI) }
        // Manyetik sensör her durumda dinlenir: kalibrasyon durumunu buradan öğreniyoruz.
        manyetik?.let { sm.registerListener(dinleyici, it, SensorManager.SENSOR_DELAY_UI) }
        if (rotasyon == null) ivme?.let { sm.registerListener(dinleyici, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sm.unregisterListener(dinleyici)
            view.keepScreenOn = false
        }
    }
    return durum
}
