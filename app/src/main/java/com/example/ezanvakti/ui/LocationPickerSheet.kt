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
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.model.trTitle
import java.text.Collator

/** İl → ilçe seçimi ve "Konumumu kullan". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(vm: PrayerViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by vm.state.collectAsStateWithLifecycle()

    var selectedState by remember { mutableStateOf<Place?>(null) }
    var query by remember { mutableStateOf("") }
    var states by remember { mutableStateOf<List<Place>>(emptyList()) }
    var cities by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val collator = remember { Collator.getInstance(VakitCalculator.tr) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.useCurrentLocation()
        onDismiss()
    }

    LaunchedEffect(reloadKey) {
        if (states.isNotEmpty()) return@LaunchedEffect
        loading = true; error = null
        runCatching { vm.states() }
            .onSuccess { list -> states = list.sortedWith(compareBy(collator) { it.name }) }
            .onFailure { error = it.message ?: "İller alınamadı." }
        loading = false
    }

    LaunchedEffect(selectedState, reloadKey) {
        val s = selectedState ?: return@LaunchedEffect
        cities = emptyList(); loading = true; error = null
        runCatching { vm.cities(s.id) }
            .onSuccess { list ->
                val center = PlaceMatcher.normalize(s.name)
                // İl merkezi en üstte, gerisi alfabetik
                cities = list.sortedWith(
                    compareBy<Place> { PlaceMatcher.normalize(it.name) != center }.thenBy(collator) { it.name }
                )
            }
            .onFailure { error = it.message ?: "İlçeler alınamadı." }
        loading = false
    }

    BackHandler(enabled = selectedState != null) {
        selectedState = null; query = ""
    }

    val source = if (selectedState == null) states else cities
    val filtered = remember(source, query) {
        if (query.isBlank()) source
        else PlaceMatcher.normalize(query).let { q -> source.filter { PlaceMatcher.normalize(it.name).contains(q) } }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (selectedState != null) {
                    TextButton(onClick = { selectedState = null; query = "" }) { Text("‹ İller") }
                }
                Text(
                    selectedState?.name?.trTitle() ?: "Konum Seç",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
            }

            if (selectedState == null) {
                TextButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.phase is Phase.Locating) "Konum bulunuyor…" else "➤  Konumumu kullan", fontSize = 17.sp)
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
                placeholder = { Text(if (selectedState == null) "İl ara" else "İlçe ara") },
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
                        Text("Tekrar dene")
                    }
                }
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.id }) { place ->
                        val parent = selectedState
                        val isSelected = parent != null && uiState.location?.city?.id == place.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (parent == null) {
                                        selectedState = place; query = ""
                                    } else {
                                        vm.select(SelectedLocation(parent, place))
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(place.name.trTitle(), fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Text(
                                if (parent == null) "›" else if (isSelected) "✓" else "",
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
            "Kayıtlı konumlar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (kayitli.isEmpty()) {
            Text(
                "Sık kullandığınız konumları kaydedip tek dokunuşla geçebilirsiniz.",
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
                TextButton(onClick = { onSil(k) }) { Text("Sil") }
            }
        }
        if (mevcut != null && kayitli.none { it.city.id == mevcut.city.id }) {
            TextButton(onClick = { onKaydet(mevcut) }) { Text("☆  ${mevcut.title} konumunu kaydet") }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}
