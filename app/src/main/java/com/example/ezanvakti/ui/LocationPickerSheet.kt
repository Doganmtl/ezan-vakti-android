package com.example.ezanvakti.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ezanvakti.data.PlaceMatcher
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import java.text.Collator

/** Ülke → il/eyalet → ilçe/şehir seçimi ve "Konumumu kullan". Açılışta mevcut konumun ülkesinden başlar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(vm: PrayerViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by vm.state.collectAsStateWithLifecycle()

    val turkiye = remember { Place(SelectedLocation.TURKIYE_ID, "TÜRKİYE", "Türkiye") }
    var country by remember { mutableStateOf(uiState.location?.country ?: turkiye) }
    var ulkeSeciliyor by remember { mutableStateOf(false) }
    var selectedState by remember { mutableStateOf<Place?>(null) }
    var query by remember { mutableStateOf("") }
    var countries by remember { mutableStateOf<List<Place>>(emptyList()) }
    var states by remember { mutableStateOf<List<Place>>(emptyList()) }
    var cities by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val collator = remember { Collator.getInstance(Dil.locale) }
    val turkiyede = country.id == SelectedLocation.TURKIYE_ID
    /** Tek bölgeli ülkelerde (ör. Endonezya) il adımı atlanır. */
    val tekBolge = !turkiyede && states.size == 1

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.useCurrentLocation()
        onDismiss()
    }

    LaunchedEffect(ulkeSeciliyor, reloadKey) {
        if (!ulkeSeciliyor || countries.isNotEmpty()) return@LaunchedEffect
        loading = true; error = null
        runCatching { vm.countries() }
            .onSuccess { list ->
                // Türkiye en üstte, gerisi seçili dilde alfabetik
                countries = list.sortedWith(
                    compareBy<Place> { it.id != SelectedLocation.TURKIYE_ID }.thenBy(collator) { it.gorunenAd }
                )
            }
            .onFailure { error = it.message ?: t("Ülkeler alınamadı.") }
        loading = false
    }

    LaunchedEffect(country, reloadKey) {
        states = emptyList(); selectedState = null
        loading = true; error = null
        runCatching { vm.states(country.id) }
            .onSuccess { list ->
                states = list.sortedWith(compareBy(collator) { it.gorunenAd })
                if (country.id != SelectedLocation.TURKIYE_ID && list.size == 1) selectedState = list.first()
            }
            .onFailure { error = it.message ?: t("İller alınamadı.") }
        loading = false
    }

    LaunchedEffect(selectedState, reloadKey) {
        val s = selectedState ?: return@LaunchedEffect
        cities = emptyList(); loading = true; error = null
        runCatching { vm.cities(s.id, country.id) }
            .onSuccess { list ->
                val center = PlaceMatcher.normalize(s.name)
                // İl merkezi en üstte, gerisi alfabetik
                cities = list.sortedWith(
                    compareBy<Place> { PlaceMatcher.normalize(it.name) != center }.thenBy(collator) { it.gorunenAd }
                )
            }
            .onFailure { error = it.message ?: t("İlçeler alınamadı.") }
        loading = false
    }

    fun geri() {
        query = ""
        when {
            ulkeSeciliyor -> ulkeSeciliyor = false
            selectedState != null && !tekBolge -> selectedState = null
            else -> ulkeSeciliyor = true
        }
    }

    BackHandler(enabled = ulkeSeciliyor || selectedState != null) { geri() }

    val source = when {
        ulkeSeciliyor -> countries
        selectedState == null -> states
        else -> cities
    }
    val filtered = remember(source, query) {
        if (query.isBlank()) source
        else PlaceMatcher.normalize(query).let { q ->
            source.filter { PlaceMatcher.normalize(it.name).contains(q) || PlaceMatcher.normalize(it.nameEn.orEmpty()).contains(q) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                when {
                    ulkeSeciliyor -> TextButton(onClick = { geri() }) { Text(t("‹ Geri")) }
                    selectedState != null && !tekBolge -> TextButton(onClick = { geri() }) {
                        Text(if (turkiyede) t("‹ İller") else t("‹ Bölgeler"))
                    }
                }
                Text(
                    when {
                        ulkeSeciliyor -> t("Ülke seçin")
                        selectedState != null -> if (tekBolge) country.gorunenAd else selectedState!!.gorunenAd
                        else -> t("Konum Seç")
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
            }

            if (!ulkeSeciliyor && (selectedState == null || tekBolge)) {
                // Seçili ülke; dokununca ülke listesi açılır.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { ulkeSeciliyor = true; query = "" }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t("Ülke"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    Text(
                        country.gorunenAd,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                    Text(t("Değiştir ›"), color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                }
                HorizontalDivider()
            }

            if (!ulkeSeciliyor && selectedState == null) {
                TextButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.phase is Phase.Locating) t("Konum bulunuyor…") else t("➤  Konumumu kullan"), fontSize = 17.sp)
                }
                (uiState.phase as? Phase.NeedsLocation)?.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                KayitliKonumlar(
                    kayitli = uiState.kayitliKonumlar,
                    mevcut = uiState.location,
                    onSec = { vm.select(it); onDismiss() },
                    onKaydet = vm::konumuKaydet,
                    onSil = vm::konumuSil,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        when {
                            ulkeSeciliyor -> t("Ülke ara")
                            selectedState == null -> if (turkiyede) t("İl ara") else t("Bölge ara")
                            else -> if (turkiyede) t("İlçe ara") else t("Şehir ara")
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error.orEmpty())
                    TextButton(onClick = { reloadKey++ }) {
                        Text(t("Tekrar dene"))
                    }
                }
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.id }) { place ->
                        val parent = selectedState
                        val secim = ulkeSeciliyor
                        val isSelected = when {
                            secim -> place.id == country.id
                            parent != null -> uiState.location?.city?.id == place.id
                            else -> false
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        secim -> {
                                            if (place.id != country.id) {
                                                country = if (place.id == SelectedLocation.TURKIYE_ID) turkiye else place
                                            }
                                            ulkeSeciliyor = false; query = ""
                                        }
                                        parent == null -> {
                                            selectedState = place; query = ""
                                        }
                                        else -> {
                                            val ulke = if (country.id == SelectedLocation.TURKIYE_ID) null else country
                                            vm.select(SelectedLocation(parent, place, ulke))
                                            onDismiss()
                                        }
                                    }
                                }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(place.gorunenAd, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Text(
                                if (isSelected) "✓" else if (secim || parent == null) "›" else "",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Kayıtlı konumlar: dokununca o konuma geçer, ✕ ile silinir; mevcut konum kaydedilebilir. */
@Composable
private fun KayitliKonumlar(
    kayitli: List<SelectedLocation>,
    mevcut: SelectedLocation?,
    onSec: (SelectedLocation) -> Unit,
    onKaydet: (SelectedLocation) -> Unit,
    onSil: (SelectedLocation) -> Unit,
) {
    if (kayitli.isEmpty() && mevcut == null) return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            t("Kayıtlı konumlar"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (kayitli.isEmpty()) {
            Text(
                t("Sık kullandığınız konumları kaydedip tek dokunuşla geçebilirsiniz."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        kayitli.forEach { k ->
            val secili = mevcut?.city?.id == k.city.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !secili) { onSec(k) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("★  ", color = MaterialTheme.colorScheme.primary)
                Text(k.title, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (secili) Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                TextButton(onClick = { onSil(k) }) { Text(t("Sil")) }
            }
        }
        if (mevcut != null && kayitli.none { it.city.id == mevcut.city.id }) {
            TextButton(onClick = { onKaydet(mevcut) }) { Text(t("☆  %s konumunu kaydet", mevcut.title)) }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}
