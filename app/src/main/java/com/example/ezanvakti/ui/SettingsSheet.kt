package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t

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
import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.BuildConfig
import androidx.compose.runtime.mutableStateOf
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.model.HicriTakvim
import com.example.ezanvakti.ui.theme.ArkaPlan
import com.example.ezanvakti.ui.theme.VakitDuzeni
import com.example.ezanvakti.ui.theme.arkaPlanRenkleri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var geriBildirim by remember { mutableStateOf(false) }
    var geriBildirimSonuc by remember { mutableStateOf<String?>(null) }
    if (geriBildirim) {
        GeriBildirimDialog(
            konum = state.location,
            onKapat = { geriBildirim = false },
            onSonuc = { geriBildirimSonuc = it; geriBildirim = false },
        )
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
            Text(t("Ayarlar"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

            // Dil: seçilince ekran yeni dille yeniden açılır.
            Text(t("Dil"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Dil.secenekler.forEach { secenek ->
                    FilterChip(
                        selected = state.dil == secenek.kod,
                        onClick = {
                            if (state.dil != secenek.kod) {
                                vm.setDil(secenek.kod)
                                context.etkinlik()?.recreate()
                            }
                        },
                        label = { Text(if (secenek.kod.isEmpty()) t(secenek.ad) else secenek.ad) },
                    )
                }
            }
            Footnote(t("Çeviriler gönüllü kontrolden geçene kadar taslaktır; yer adları Diyanet'in yazımıyla gösterilir."))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Görünüm: arka plan ve vakitlerin dizilişi
            Text(t("Görünüm"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("Arka plan"), fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArkaPlan.entries.forEach { tema ->
                    val (ust, alt) = arkaPlanRenkleri(tema, com.example.ezanvakti.model.Vakit.OGLE)
                    FilterChip(
                        selected = state.arkaPlan == tema.kod,
                        onClick = { vm.setArkaPlan(tema.kod) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tema == ArkaPlan.VAKTE_GORE) {
                                            Brush.sweepGradient(listOf(Color(0xFF1A1F54), Color(0xFFF28C59), Color(0xFF2980D9), Color(0xFFE65940), Color(0xFF1A1F54)))
                                        } else Brush.verticalGradient(listOf(ust, alt))
                                    ),
                            )
                        },
                        label = { Text(t(tema.ad)) },
                    )
                }
            }
            Text(t("Vakitlerin dizilişi"), fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            SecimSatiri(
                VakitDuzeni.entries.map { it.kod to t(it.ad) },
                state.vakitDuzeni,
                vm::setVakitDuzeni,
            )
            Footnote(t("Alt alta liste ya da yan yana ikili/üçlü kutucuklar."))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            if (BuildConfig.DEBUG) {
                // Yalnızca test sürümünde: Ramazan arayüzünü yılın herhangi bir gününde denemek için
                AyarSatiri(t("Ramazan önizlemesi (geliştirici)"), state.ramazanOnizleme, true, vm::setRamazanOnizleme)
                Footnote(t("Bugünü Ramazan'ın 5. günüymüş gibi gösterir. Yayın sürümünde görünmez."))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Vakit bildirimleri"), fontSize = 17.sp, modifier = Modifier.weight(1f))
                Switch(checked = state.notificationsEnabled, onCheckedChange = vm::setNotificationsEnabled)
            }
            Footnote(t("İmsak, öğle, ikindi, akşam ve yatsı vakti girdiğinde bildirim gönderilir. Ramazan'da imsak ve iftar bildirimleri buna göre değişir."))

            AyarSatiri(t("Kandil gecesi bildirimi"), state.kandilBildirimi, state.notificationsEnabled, vm::setKandilBildirimi)
            Footnote(t("Kandil akşamı, akşam ezanıyla birlikte \"Bu gece … Kandili\" bildirimi."))

            AyarSatiri(t("Sahur hatırlatıcısı"), state.sahurHatirlatici, state.notificationsEnabled, vm::setSahurHatirlatici)
            Footnote(t("Ramazan'da imsaktan 45 dakika önce hatırlatır."))

            AyarSatiri(t("Cuma hatırlatması"), state.cumaHatirlatici, state.notificationsEnabled, vm::setCumaHatirlatici)
            Footnote(t("Cuma günü, vakit girmeden 45 dakika önce hatırlatır."))

            AyarSatiri(t("Kehf suresi hatırlatması"), state.kehfHatirlatici, state.notificationsEnabled, vm::setKehfHatirlatici)
            Footnote(t("Cuma sabahı Kehf suresini okumayı hatırlatır."))

            AyarSatiri(t("Bayram ve kandil tebrikleri"), state.tebrikler, state.notificationsEnabled, vm::setTebrikler)
            Footnote(t("Resmî bayram ve tatillerde, Ramazan ve Kurban Bayramı'nın ilk günü ve kandillerde sabah 09:00'da tebrik mesajı gönderir."))

            Text(t("Vakitten önce hatırlat"), fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            SecenekSatiri(listOf(0, 10, 15, 30, 45), state.onceDk, state.notificationsEnabled, vm::setOnceDk)
            Footnote(t("Her vakit girmeden seçtiğiniz kadar önce haber verir; güneş doğuşu için \"sabah namazı için son dakikalar\" hatırlatması gelir."))

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(t("Namazda telefonu sessize al"), fontSize = 17.sp)
            SecenekSatiri(listOf(0, 20, 30, 45), state.sessizDk, true) { dk ->
                vm.setSessizDk(dk)
                if (dk > 0 && !sessizIzni) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            Footnote(t("Vakit girince telefon \"Rahatsız Etmeyin\" moduna geçer, süre dolunca eski hâline döner. Telefon zaten sessizdeyse dokunulmaz. Güneş doğuşunda sessize alınmaz."))
            if (state.sessizDk > 0 && !sessizIzni) {
                Footnote(t("Bunun için \"Rahatsız Etmeyin\" erişimi gerekiyor: açılan listede Ezan Vakti'ye izin verin."))
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }) { Text(t("Rahatsız Etmeyin erişimi ver")) }
            }

            AyarSatiri(t("Kilit ekranında geri sayım"), state.canliSayac, true, vm::setCanliSayac)
            Footnote(t("Bildirim alanında ve kilit ekranında sıradaki vakte (Ramazan'da iftar ya da sahura) kalan süre sessizce görünür."))
            if (state.canliSayac) {
                SecimSatiri(
                    listOf(
                        "siradaki" to t("Yalnızca sıradaki vakit"),
                        "tum" to t("Sıradaki vakit + günün vakitleri"),
                        "ramazan" to t("Yalnızca Ramazan'da"),
                    ),
                    state.canliSayacModu,
                    vm::setCanliSayacModu,
                )
            }

            Text(t("Widget görünümü"), fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            SecimSatiri(
                listOf("koyu" to t("Koyu"), "seffaf" to t("Şeffaf"), "tema" to t("Uygulama temasıyla")),
                state.widgetStili,
                vm::setWidgetStili,
            )
            Footnote(t("Üç widget var: büyük (geri sayım ve günün vakitleri), küçük (yalnızca sıradaki vakit) ve şerit (günün vakitleri yan yana). Ana ekranda boş bir yere basılı tutup Widget'lar'dan ekleyebilirsiniz."))

            if (state.notificationsEnabled && !notifAllowed) {
                Footnote(t("Bildirim izni kapalı. Bildirim alabilmek için izin verin."))
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text(t("Bildirim iznini aç")) }
            }
            if (state.notificationsEnabled && !canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Footnote(t("Bildirimlerin tam vakit girdiği dakikada gelmesi için \"Alarm ve hatırlatıcı\" iznini açın."))
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }) { Text(t("Tam zamanlı bildirime izin ver")) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(t("Konum"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.location?.title ?: "—", fontSize = 17.sp)
            TextButton(onClick = { vm.useCurrentLocation(); onDismiss() }) { Text(t("Konumumu yeniden bul")) }
            AyarSatiri(t("Seyahat modu"), state.seyahatModu, true, vm::setSeyahatModu)
            Footnote(t("Başka bir ile gittiğinizde uygulama açılınca vakitlerin yeni konuma göre güncellenmesini önerir."))

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(t("Hicri takvim"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "" to t("Ülkeye göre"),
                    HicriTakvim.Tur.DIYANET.kod to t("Diyanet"),
                    HicriTakvim.Tur.UMMULKURA.kod to t("Ümmülkurâ (Suudi Arabistan)"),
                ).forEach { (kod, ad) ->
                    FilterChip(selected = state.hicriTakvim == kod, onClick = { vm.setHicriTakvim(kod) }, label = { Text(ad) })
                }
            }
            Footnote(
                t("Kullanılan: %s.", if (state.takvimTuru == HicriTakvim.Tur.UMMULKURA) t("Ümmülkurâ (Suudi Arabistan)") else t("Diyanet")) + " " +
                    t("Kandil, bayram ve Ramazan tarihleri ile imsakiye bu takvime göre hesaplanır. Suudi Arabistan ve Körfez ülkelerinde Ümmülkurâ, diğer ülkelerde Diyanet takvimi varsayılandır.")
            )
            Text(t("Gün düzeltmesi"), fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (-2..2).forEach { g ->
                    FilterChip(
                        selected = state.hicriDuzeltme == g,
                        onClick = { vm.setHicriDuzeltme(g) },
                        label = { Text(if (g == 0) t("Yok") else t("%s gün", if (g > 0) "+$g" else "−${-g}")) },
                    )
                }
            }
            Footnote(t("Bulunduğunuz ülkede Ramazan ve bayram, hilalin görülmesine göre farklı bir günde ilan edildiyse tarihleri buradan bir-iki gün kaydırabilirsiniz."))

            if (AppConfig.GERI_BILDIRIM_EPOSTA.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(t("Geri bildirim"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { geriBildirim = true }) { Text(t("✉  Geri bildirim gönder")) }
                Footnote(t("Öneri ve hataları en fazla 100 karakterle geliştiriciye iletin."))
                geriBildirimSonuc?.let { Footnote(it) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(t("Veri"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.lastUpdated?.let {
                Text(t("Son güncelleme: ") + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
            }
            TextButton(onClick = { vm.reload() }) { Text(t("Vakitleri şimdi yenile")) }
            Footnote(t("Namaz vakitleri T.C. Diyanet İşleri Başkanlığı'nın namazvakitleri.diyanet.gov.tr adresinde yayımladığı vakitlerdir; uygulama bunları kendi veri deposundan ya da Diyanet verisinin aynasından alır."))
        }
    }
}

/** Compose bağlamından Activity'yi bulur (dil değişince yeniden oluşturmak için). */
private fun android.content.Context.etkinlik(): android.app.Activity? {
    var c: android.content.Context? = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}

/** Metinli seçenekler (FilterChip satırı). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecimSatiri(secenekler: List<Pair<String, String>>, secili: String, onSec: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        secenekler.forEach { (kod, ad) ->
            FilterChip(selected = secili == kod, onClick = { onSec(kod) }, label = { Text(ad) })
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
                label = { Text(if (dk == 0) t("Kapalı") else t("%s dk", dk)) },
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
