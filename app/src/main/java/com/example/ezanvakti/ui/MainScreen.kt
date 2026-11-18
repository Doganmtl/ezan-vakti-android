package com.example.ezanvakti.ui

import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.model.HicriTakvim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ezanvakti.model.RamazanHesabi
import com.example.ezanvakti.model.Vakit
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.notify.NotificationScheduler
import com.example.ezanvakti.ui.theme.ArkaPlan
import com.example.ezanvakti.ui.theme.VakitDuzeni
import com.example.ezanvakti.ui.theme.arkaPlanRenkleri
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: PrayerViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Saniyede bir güncellenen "şimdi"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val durum = remember(state.days, now) { VakitCalculator.durum(state.days, now) }
    val today = remember(state.days, now / 60_000) { VakitCalculator.today(state.days, now) }
    val bugunTarih = remember(now / 60_000) { LocalDate.ofEpochDay(VakitCalculator.epochDay(now)) }
    val bugununGunleri = remember(state.dini, bugunTarih) { state.dini.on(bugunTarih) }
    val siradaki = remember(state.dini, bugunTarih) { state.dini.upcoming(bugunTarih.plusDays(1), 1).firstOrNull() }
    val ramazan = remember(state.days, state.dini, state.ramazanOnizleme, now / 1000) {
        RamazanHesabi.durum(state.days, now, state.dini, state.onizlemeBaslangic)
    }

    // Tablet / katlanabilir / yatay geniş ekran: iki sütunlu düzen
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showImsakiye by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }   // 0 Vakitler, 1 Takvim, 2 Zikir, 3 Kıble, 4 Diğer
    var digerSayfa by rememberSaveable { mutableStateOf(DigerSayfa.MENU) }
    val zikirVm: ZikirViewModel = viewModel()
    // Geri tuşu: diğer sekmelerden Vakitler'e döner
    BackHandler(enabled = tab != 0) { tab = 0 }

    // İlk açılışta konum + bildirim izni, ardından başlat
    var permissionsAsked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.bootstrap() }
    LaunchedEffect(Unit) {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isEmpty() || permissionsAsked) {
            vm.bootstrap()
        } else {
            permissionsAsked = true
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Android 12+: "Alarm ve hatırlatıcılar" izni yoksa bildirimler birkaç dakika gecikebilir.
    var canExact by remember { mutableStateOf(NotificationScheduler.canScheduleExact(context)) }
    var exactBannerDismissed by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshIfNeeded()
        vm.seyahatKontrol()
        val nowExact = NotificationScheduler.canScheduleExact(context)
        if (nowExact && !canExact) vm.updateNotifications()   // izin yeni verildiyse tam zamanlı yeniden kur
        canExact = nowExact
    }
    val showExactBanner = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        state.notificationsEnabled && !canExact && !exactBannerDismissed && state.days.isNotEmpty()

    // Tek seferlik bilgi mesajları (ör. "Konum bulunamadı")
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.messageShown()
    }

    LaunchedEffect(state.phase) {
        if (state.phase is Phase.NeedsLocation) showPicker = true
    }

    val (top, bottom) = arkaPlanRenkleri(ArkaPlan.from(state.arkaPlan), durum?.current ?: Vakit.OGLE)
    val duzen = VakitDuzeni.from(state.vakitDuzeni)
    val topColor by animateColorAsState(top, tween(1500), label = "top")
    val bottomColor by animateColorAsState(bottom, tween(1500), label = "bottom")

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter)
                    .widthIn(max = 1100.dp)
            ) {
                Header(
                    title = state.location?.title ?: t("Konum seçin"),
                    dateLine = buildString {
                        append(VakitCalculator.longDate(now))
                        HicriTakvim.goster(today?.hijri)?.let { append(" · ").append(it) }
                    },
                    wide = wide,
                    onLocation = { showPicker = true },
                    onSettings = { showSettings = true },
                )

                if (showExactBanner) {
                    ExactAlarmBanner(
                        wide = wide,
                        onAllow = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            )
                        },
                        onDismiss = { exactBannerDismissed = true },
                    )
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (tab == 2) {
                        ZikirEkrani(zikirVm)
                    } else if (tab == 3) {
                        KibleEkrani(
                            state.location?.title,
                            state.location?.takeIf { !it.turkiyede }?.country?.let { it.nameEn ?: it.name } ?: "Türkiye",
                        )
                    } else if (tab == 4) {
                        DigerEkrani(
                            sayfa = digerSayfa,
                            onSayfa = { digerSayfa = it },
                            zikirVm = zikirVm,
                            onZikreGit = { tab = 2 },
                            onAyarlar = { showSettings = true },
                        )
                    } else if (tab == 1) {
                        TakvimEkrani(
                            dini = state.dini,
                            days = state.days,
                            bugun = bugunTarih,
                            onizlemeBaslangic = state.onizlemeBaslangic,
                            wide = wide,
                            onImsakiye = { showImsakiye = true },
                        )
                    } else {
                        val phase = state.phase
                        when {
                            state.days.isEmpty() && (phase is Phase.Idle || phase is Phase.Locating || phase is Phase.Loading) ->
                                StatusView(
                                    icon = "📍",
                                    message = if (phase is Phase.Locating) t("Konumunuz bulunuyor…") else t("Vakitler yükleniyor…"),
                                    showsProgress = true,
                                )

                            state.days.isEmpty() && phase is Phase.Failed ->
                                StatusView("📶", phase.message, actionLabel = t("Tekrar dene")) { vm.reload() }

                            state.days.isEmpty() && phase is Phase.NeedsLocation ->
                                StatusView("🗺️", phase.message ?: t("Vakitleri görmek için il ve ilçenizi seçin."), actionLabel = t("Konum seç")) {
                                    showPicker = true
                                }

                            durum != null && today != null -> {
                                if (wide) {
                                    // Tablet: solda geri sayım (+ Ramazan), sağda vakit listesi
                                    Row(
                                        Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                                    ) {
                                        Column(
                                            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(20.dp),
                                        ) {
                                            BugununDiniGunu(bugununGunleri, large = true, modifier = Modifier.fillMaxWidth())
                                            CountdownCard(durum, now, large = ramazan == null, modifier = Modifier.fillMaxWidth())
                                            ramazan?.let { RamazanKarti(it, now, large = true, onImsakiye = { showImsakiye = true }, modifier = Modifier.fillMaxWidth()) }
                                        }
                                        Column(
                                            Modifier.width(380.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(20.dp),
                                        ) {
                                            PrayerList(today, durum, now, large = true, modifier = Modifier.fillMaxWidth(), duzen = duzen)
                                            siradaki?.let { SiradakiDiniGun(it, bugunTarih, onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) }
                                        }
                                    }
                                } else {
                                    PullToRefreshBox(
                                        isRefreshing = false,
                                        onRefresh = { vm.reload() },
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Column(
                                            Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(20.dp),
                                        ) {
                                            BugununDiniGunu(bugununGunleri, large = false, modifier = Modifier.fillMaxWidth())
                                            CountdownCard(durum, now, large = false, modifier = Modifier.fillMaxWidth())
                                            ramazan?.let { RamazanKarti(it, now, large = false, onImsakiye = { showImsakiye = true }, modifier = Modifier.fillMaxWidth()) }
                                            PrayerList(today, durum, now, large = false, modifier = Modifier.fillMaxWidth(), duzen = duzen)
                                            siradaki?.let { SiradakiDiniGun(it, bugunTarih, onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth()) }
                                        }
                                    }
                                }
                            }

                            else -> StatusView("📅", t("Bugünün vakitleri bulunamadı."), actionLabel = t("Yenile")) { vm.reload() }
                        }
                    }
                }

                val navRenkleri = NavigationBarItemDefaults.colors(
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.7f),
                    indicatorColor = Color.White.copy(alpha = 0.22f),
                )
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    windowInsets = WindowInsets(0),
                ) {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Text("🕌", fontSize = 20.sp) }, label = { Text(t("Vakitler")) }, colors = navRenkleri,
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Text("📅", fontSize = 20.sp) }, label = { Text(t("Takvim")) }, colors = navRenkleri,
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Text("📿", fontSize = 20.sp) }, label = { Text(t("Zikir")) }, colors = navRenkleri,
                    )
                    NavigationBarItem(
                        selected = tab == 3, onClick = { tab = 3 },
                        icon = { Text("🧭", fontSize = 20.sp) }, label = { Text(t("Kıble")) }, colors = navRenkleri,
                    )
                    NavigationBarItem(
                        selected = tab == 4, onClick = { if (tab == 4) digerSayfa = DigerSayfa.MENU; tab = 4 },
                        icon = { Text("☰", fontSize = 20.sp) }, label = { Text(t("Diğer")) }, colors = navRenkleri,
                    )
                }
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            )
        }
    }

    if (showPicker) LocationPickerSheet(vm, onDismiss = { showPicker = false })

    // Seyahat modu: başka bir ilde olunduğu anlaşılınca sor
    state.konumOnerisi?.let { oneri ->
        AlertDialog(
            onDismissRequest = vm::konumOnerisiniReddet,
            title = { Text(t("Konumunuz değişmiş")) },
            text = {
                Text(t("Şu an %s civarında görünüyorsunuz. Vakitler bu konuma göre güncellensin mi?", oneri.title) +
                    (state.location?.let { t("\n\nŞu anki seçim: %s", it.title) } ?: ""))
            },
            confirmButton = { TextButton(onClick = vm::konumOnerisiniKabulEt) { Text(t("Güncelle")) } },
            dismissButton = { TextButton(onClick = vm::konumOnerisiniReddet) { Text(t("Hayır")) } },
        )
    }
    if (showSettings) SettingsSheet(vm, state, onDismiss = { showSettings = false })
    if (showImsakiye) {
        ImsakiyeSheet(
            yer = state.location?.title.orEmpty(),
            days = state.days,
            dini = state.dini,
            bugun = bugunTarih,
            onizlemeBaslangic = state.onizlemeBaslangic,
            onDismiss = { showImsakiye = false },
        )
    }
}

@Composable
private fun Header(
    title: String,
    dateLine: String,
    wide: Boolean,
    onLocation: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (wide) 40.dp else 20.dp, vertical = if (wide) 16.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(onClick = onLocation)
                .semantics { contentDescription = t("Konum: %s. Değiştirmek için dokunun.", title) }
                .padding(4.dp)
        ) {
            Text(
                t("➤ %s ▾", title),
                fontSize = if (wide) 22.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(dateLine, fontSize = if (wide) 15.sp else 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onSettings)
                .semantics { contentDescription = t("Ayarlar") },
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", fontSize = 20.sp)
        }
    }
}

@Composable
private fun ExactAlarmBanner(wide: Boolean, onAllow: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (wide) 40.dp else 20.dp, vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.18f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            t("🔔 Bildirimlerin vakit girdiği dakikada gelmesi için izin verin."),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text(t("Sonra"), color = Color.White.copy(alpha = 0.75f)) }
        TextButton(onClick = onAllow) { Text(t("İzin ver"), color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StatusView(
    icon: String,
    message: String,
    showsProgress: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(icon, fontSize = 48.sp)
        if (showsProgress) CircularProgressIndicator(color = Color.White)
        Text(message, textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f), contentColor = Color.White),
            ) { Text(actionLabel) }
        }
        Spacer(Modifier.size(1.dp))
    }
}
