package com.example.ezanvakti.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.ezanvakti.model.VakitCalculator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Tek seferlik konum alır ve il/ilçe adına çevirir. */
class LocationResolver(private val context: Context) {

    data class Area(val il: String, val ilce: String?)

    class LocationException(message: String) : Exception(message)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun resolve(): Area {
        if (!hasPermission()) {
            throw LocationException("Konum izni verilmedi. İl ve ilçenizi elle seçebilirsiniz.")
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        // Konum servisi yanıt vermezse (ör. GPS kapalı, emülatörde konum yok) en fazla 10 sn bekle.
        val cancel = CancellationTokenSource()
        val location: Location = runCatching {
            withTimeoutOrNull(10_000L) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
            } ?: client.lastLocation.await()
        }.getOrNull().also { cancel.cancel() }
            ?: throw LocationException("Konum alınamadı. Konum servisinin açık olduğundan emin olun ya da il ve ilçenizi elle seçin.")

        val address = reverseGeocode(location)
            ?: throw LocationException("Konum adresi çözülemedi. İl ve ilçenizi elle seçebilirsiniz.")
        if (address.countryCode != "TR") {
            throw LocationException("Konumunuz Türkiye dışında görünüyor. Uygulama şimdilik yalnızca Türkiye'yi destekliyor.")
        }
        val il = address.adminArea ?: throw LocationException("İl bilgisi bulunamadı. Lütfen elle seçin.")
        // Android, ilçeyi bazen subAdminArea, bazen locality olarak döndürür.
        return Area(il, address.subAdminArea ?: address.locality)
    }

    /** Kıble için anlık koordinat; alınamazsa null. */
    @SuppressLint("MissingPermission")
    suspend fun koordinat(): Location? {
        if (!hasPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = CancellationTokenSource()
        return runCatching {
            withTimeoutOrNull(10_000L) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
            } ?: client.lastLocation.await()
        }.getOrNull().also { cancel.cancel() }
    }

    /** Konum alınamazsa seçili il/ilçenin adından yaklaşık koordinat (enlem, boylam). */
    @Suppress("DEPRECATION")
    suspend fun adresKoordinati(adres: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, VakitCalculator.tr).getFromLocationName("$adres, Türkiye", 1)
                ?.firstOrNull()
                ?.let { it.latitude to it.longitude }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, VakitCalculator.tr)
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
        }.getOrNull()
    }
}
