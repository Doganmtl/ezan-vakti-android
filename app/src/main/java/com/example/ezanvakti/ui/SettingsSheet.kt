package com.example.ezanvakti.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ezanvakti.BuildConfig
import com.example.ezanvakti.notify.NotificationScheduler
import com.example.ezanvakti.notify.SessizReceiver
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: PrayerViewModel, state: UiState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Sistem ayarlarından dönünce izin durumlarını yeniden oku
    var refreshTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshTick++
        vm.updateNotifications()
    }
    val canExact = remember(refreshTick) { NotificationScheduler.canScheduleExact(context) }
    val sessizIzni = remember(refreshTick) { SessizReceiver.izinVar(context) }
    val notifAllowed = remember(refreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Ayarlar", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

            if (BuildConfig.DEBUG) {
                // Yalnızca test sürümünde: Ramazan arayüzünü yılın herhangi bir gününde denemek için
                AyarSatiri("Ramazan önizlemesi (geliştirici)", state.ramazanOnizleme, true, vm::setRamazanOnizleme)
                Footnote("Bugünü Ramazan'ın 5. günüymüş gibi gösterir. Yayın sürümünde görünmez.")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Vakit bildirimleri", fontSize = 17.sp, modifier = Modifier.weight(1f))
                Switch(checked = state.notificationsEnabled, onCheckedChange = vm::setNotificationsEnabled)
            }
            Footnote("İmsak, öğle, ikindi, akşam ve yatsı vakti girdiğinde bildirim gönderilir. Ramazan'da imsak ve iftar bildirimleri buna göre değişir.")

            AyarSatiri("Kandil gecesi bildirimi", state.kandilBildirimi, state.notificationsEnabled, vm::setKandilBildirimi)
            Footnote("Kandil akşamı, akşam ezanıyla birlikte \"Bu gece … Kandili\" bildirimi.")

            AyarSatiri("Sahur hatırlatıcısı", state.sahurHatirlatici, state.notificationsEnabled, vm::setSahurHatirlatici)
            Footnote("Ramazan'da imsaktan 45 dakika önce hatırlatır.")

            AyarSatiri("Cuma hatırlatması", state.cumaHatirlatici, state.notificationsEnabled, vm::setCumaHatirlatici)
            Footnote("Cuma günü, vakit girmeden 45 dakika önce hatırlatır.")

            AyarSatiri("Kehf suresi hatırlatması", state.kehfHatirlatici, state.notificationsEnabled, vm::setKehfHatirlatici)
            Footnote("Cuma sabahı Kehf suresini okumayı hatırlatır.")

            AyarSatiri("Bayram ve kandil tebrikleri", state.tebrikler, state.notificationsEnabled, vm::setTebrikler)
            Footnote("Resmî bayram ve tatillerde, Ramazan ve Kurban Bayramı'nın ilk günü ve kandillerde sabah 09:00'da tebrik mesajı gönderir.")

            Text("Vakitten önce hatırlat", fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            SecenekSatiri(listOf(0, 10, 15, 30, 45), state.onceDk, state.notificationsEnabled, vm::setOnceDk)
            Footnote("Her vakit girmeden seçtiğiniz kadar önce haber verir; güneş doğuşu için \"sabah namazı için son dakikalar\" hatırlatması gelir.")

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Namazda telefonu sessize al", fontSize = 17.sp)
            SecenekSatiri(listOf(0, 20, 30, 45), state.sessizDk, true) { dk ->
                vm.setSessizDk(dk)
                if (dk > 0 && !sessizIzni) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            Footnote("Vakit girince telefon \"Rahatsız Etmeyin\" moduna geçer, süre dolunca eski hâline döner. Telefon zaten sessizdeyse dokunulmaz. Güneş doğuşunda sessize alınmaz.")
            if (state.sessizDk > 0 && !sessizIzni) {
                Footnote("Bunun için \"Rahatsız Etmeyin\" erişimi gerekiyor: açılan listede Ezan Vakti'ye izin verin.")
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }) { Text("Rahatsız Etmeyin erişimi ver") }
            }

            AyarSatiri("Kilit ekranında geri sayım", state.canliSayac, true, vm::setCanliSayac)
            Footnote("Bildirim alanında ve kilit ekranında sıradaki vakte (Ramazan'da iftar ya da sahura) kalan süre sessizce görünür.")

            if (state.notificationsEnabled && !notifAllowed) {
                Footnote("Bildirim izni kapalı. Bildirim alabilmek için izin verin.")
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text("Bildirim iznini aç") }
            }
            if (state.notificationsEnabled && !canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Footnote("Bildirimlerin tam vakit girdiği dakikada gelmesi için \"Alarm ve hatırlatıcı\" iznini açın.")
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }) { Text("Tam zamanlı bildirime izin ver") }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Konum", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.location?.title ?: "—", fontSize = 17.sp)
            TextButton(onClick = { vm.useCurrentLocation(); onDismiss() }) { Text("Konumumu yeniden bul") }
            AyarSatiri("Seyahat modu", state.seyahatModu, true, vm::setSeyahatModu)
            Footnote("Başka bir ile gittiğinizde uygulama açılınca vakitlerin yeni konuma göre güncellenmesini önerir.")

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Veri", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.lastUpdated?.let {
                Text("Son güncelleme: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
            }
            TextButton(onClick = { vm.reload() }) { Text("Vakitleri şimdi yenile") }
            Footnote("Namaz vakitleri T.C. Diyanet İşleri Başkanlığı verileridir.")
        }
    }
}

@Composable
private fun Footnote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "Kapalı · 10 dk · 15 dk …" seçenekleri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecenekSatiri(secenekler: List<Int>, secili: Int, etkin: Boolean, onSec: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        secenekler.forEach { dk ->
            FilterChip(
                selected = secili == dk,
                onClick = { onSec(dk) },
                enabled = etkin,
                label = { Text(if (dk == 0) "Kapalı" else "$dk dk") },
            )
        }
    }
}

@Composable
private fun AyarSatiri(baslik: String, acik: Boolean, etkin: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(baslik, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = acik && etkin, onCheckedChange = onChange, enabled = etkin)
    }
}
