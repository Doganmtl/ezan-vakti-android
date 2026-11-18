package com.example.ezanvakti.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.data.DiniGunlerRepository
import com.example.ezanvakti.data.PlaceMatcher
import com.example.ezanvakti.data.PrayerTimesProvider
import com.example.ezanvakti.data.Storage
import com.example.ezanvakti.location.LocationResolver
import com.example.ezanvakti.model.DailyTimes
import com.example.ezanvakti.model.DiniGunlerVerisi
import com.example.ezanvakti.model.Place
import com.example.ezanvakti.model.SelectedLocation
import com.example.ezanvakti.model.VakitCalculator
import com.example.ezanvakti.notify.CanliSayac
import com.example.ezanvakti.notify.NotificationScheduler
import com.example.ezanvakti.notify.Tazeleyici
import com.example.ezanvakti.widget.VakitWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Phase {
    data object Idle : Phase
    data object Locating : Phase
    data object Loading : Phase
    data object Ready : Phase
    data class NeedsLocation(val message: String?) : Phase
    data class Failed(val message: String) : Phase
}

data class UiState(
    val phase: Phase = Phase.Idle,
    val location: SelectedLocation? = null,
    val days: List<DailyTimes> = emptyList(),
    val lastUpdated: Long? = null,
    val notificationsEnabled: Boolean = true,
    /** Ekranda kısa süre gösterilecek tek seferlik bilgi mesajı (Snackbar). */
    val message: String? = null,
    /** Diyanet dini günler takvimi (kandiller, bayramlar, Ramazan dönemleri). */
    val dini: DiniGunlerVerisi = DiniGunlerVerisi.BOS,
    val kandilBildirimi: Boolean = true,
    val sahurHatirlatici: Boolean = true,
    val cumaHatirlatici: Boolean = true,
    val kehfHatirlatici: Boolean = true,
    val onceDk: Int = 0,
    val tebrikler: Boolean = true,
    val sessizDk: Int = 0,
    val canliSayac: Boolean = false,
    val seyahatModu: Boolean = true,
    val kayitliKonumlar: List<SelectedLocation> = emptyList(),
    /** Seyahat modu: başka bir ilde olunduğu anlaşılınca önerilen konum. */
    val konumOnerisi: SelectedLocation? = null,
    /** Yalnızca geliştirici sürümünde: bugünü Ramazan'ın 5. günü gibi gösterir (arayüz testi için). */
    val ramazanOnizleme: Boolean = false,
) {
    /** Önizleme açıksa sahte Ramazan'ın ilk günü (bugünden 4 gün önce). */
    val onizlemeBaslangic: LocalDate?
        get() = if (ramazanOnizleme) LocalDate.now(VakitCalculator.zone).minusDays(4) else null
}

private const val MAX_KAYITLI = 8

class PrayerViewModel(app: Application) : AndroidViewModel(app) {
    val provider: PrayerTimesProvider = AppConfig.makeProvider()
    private val storage = Storage(app)
    private val locationResolver = LocationResolver(app)
    private val diniRepo = DiniGunlerRepository(app)

    private val _state = MutableStateFlow(
        UiState(
            notificationsEnabled = storage.notificationsEnabled,
            kandilBildirimi = storage.kandilBildirimi,
            sahurHatirlatici = storage.sahurHatirlatici,
            cumaHatirlatici = storage.cumaHatirlatici,
            kehfHatirlatici = storage.kehfHatirlatici,
            onceDk = storage.onceHatirlatmaDk,
            tebrikler = storage.tebrikler,
            sessizDk = storage.sessizDk,
            canliSayac = storage.canliSayac,
            seyahatModu = storage.seyahatModu,
            kayitliKonumlar = storage.kayitliKonumlar,
            dini = diniRepo.loadLocal(),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var bootstrapped = false

    init {
        NotificationScheduler.createChannel(app)
    }

    // MARK: Akış

    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        refreshDiniGunler()
        val saved = storage.location
        if (saved != null) {
            val cached = storage.cachedTimes(saved.city.id).orEmpty()
            _state.update {
                it.copy(location = saved, days = cached, phase = if (cached.isNotEmpty()) Phase.Ready else Phase.Loading)
            }
            VakitWidget.guncelle(getApplication())
            refreshIfNeeded()
            seyahatKontrol()
        } else {
            useCurrentLocation()
        }
    }

    /** Konumdan il/ilçe bulur ve vakitleri yükler. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.Locating) }
            try {
                val area = locationResolver.resolve()
                val states = provider.states()
                val state = PlaceMatcher.bestMatch(area.il, states)
                if (state == null) {
                    _state.update { it.copy(phase = Phase.NeedsLocation("\"${area.il}\" ili listede bulunamadı. Lütfen elle seçin.")) }
                    return@launch
                }
                val cities = provider.cities(state.id)
                val city = PlaceMatcher.bestMatch(area.ilce, cities) ?: PlaceMatcher.center(state, cities)
                if (city == null) {
                    _state.update { it.copy(phase = Phase.NeedsLocation(null)) }
                    return@launch
                }
                val found = SelectedLocation(state, city)
                val unchanged = _state.value.location == found
                select(found)
                // Konum değişmediyse de kullanıcıya bulunduğunu bildir (değiştiyse select() zaten bildiriyor).
                if (unchanged) _state.update { it.copy(message = "Konumunuz doğrulandı: ${found.title}") }
            } catch (e: Exception) {
                val message = e.message ?: "Konum alınamadı."
                _state.update {
                    when {
                        it.location == null -> it.copy(phase = Phase.NeedsLocation(message))
                        it.days.isNotEmpty() -> it.copy(
                            phase = Phase.Ready,
                            message = "Konum bulunamadı. ${it.location.title} vakitleri gösterilmeye devam ediyor.",
                        )
                        else -> it.copy(phase = Phase.Failed(message))
                    }
                }
            }
        }
    }

    fun select(location: SelectedLocation) {
        val previous = _state.value.location
        if (previous != null && previous != location) {
            _state.update { it.copy(message = "Konum güncellendi: ${location.title}") }
        }
        storage.location = location
        _state.update { it.copy(location = location, days = storage.cachedTimes(location.city.id).orEmpty()) }
        reload()
    }

    /** Önümüzdeki 3 günün verisi yoksa ya da veri 12 saatten eskiyse yeniler. */
    fun refreshIfNeeded() {
        val s = _state.value
        if (s.location == null) return
        val horizon = com.example.ezanvakti.model.VakitCalculator.epochDay(System.currentTimeMillis()) + 3
        val hasEnough = s.days.any { it.epochDay >= horizon }
        val stale = s.lastUpdated?.let { System.currentTimeMillis() - it > 12 * 3_600_000L } ?: true
        if (!hasEnough || stale) reload()
    }

    fun reload() {
        val location = _state.value.location ?: return
        viewModelScope.launch {
            if (_state.value.days.isEmpty()) _state.update { it.copy(phase = Phase.Loading) }
            try {
                val fresh = provider.times(location.city.id).sortedBy { it.epochDay }
                storage.saveTimes(fresh, location.city.id)
                VakitWidget.guncelle(getApplication())
                _state.update {
                    it.copy(days = fresh, lastUpdated = System.currentTimeMillis(), phase = Phase.Ready)
                }
                updateNotifications()
            } catch (e: Exception) {
                // Önbellekte veri varsa onu kullanmaya devam et ve kullanıcıyı bilgilendir.
                _state.update {
                    if (it.days.isEmpty()) {
                        it.copy(phase = Phase.Failed(e.message ?: "Vakitler alınamadı."))
                    } else {
                        it.copy(phase = Phase.Ready, message = "Vakitler güncellenemedi; kayıtlı vakitler gösteriliyor.")
                    }
                }
            }
        }
    }

    /** Dini günler takviminin güncel kopyasını veri deposundan alır (varsa). */
    private fun refreshDiniGunler() {
        viewModelScope.launch {
            val yeni = diniRepo.refresh(AppConfig.DATA_BASE_URL, _state.value.dini) ?: return@launch
            _state.update { it.copy(dini = yeni) }
            updateNotifications()
        }
    }

    fun setKandilBildirimi(enabled: Boolean) {
        storage.kandilBildirimi = enabled
        _state.update { it.copy(kandilBildirimi = enabled) }
        updateNotifications()
    }

    fun setSahurHatirlatici(enabled: Boolean) {
        storage.sahurHatirlatici = enabled
        _state.update { it.copy(sahurHatirlatici = enabled) }
        updateNotifications()
    }

    fun setCumaHatirlatici(enabled: Boolean) {
        storage.cumaHatirlatici = enabled
        _state.update { it.copy(cumaHatirlatici = enabled) }
        updateNotifications()
    }

    fun setKehfHatirlatici(enabled: Boolean) {
        storage.kehfHatirlatici = enabled
        _state.update { it.copy(kehfHatirlatici = enabled) }
        updateNotifications()
    }

    fun setRamazanOnizleme(enabled: Boolean) {
        _state.update { it.copy(ramazanOnizleme = enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        storage.notificationsEnabled = enabled
        _state.update { it.copy(notificationsEnabled = enabled) }
        updateNotifications()
    }

    fun updateNotifications() {
        val s = _state.value
        val app = getApplication<Application>()
        if (s.location != null && s.days.isNotEmpty()) {
            // Bildirimler, sessize alma, widget ve kilit ekranı sayacı birlikte yeniden kurulur.
            Tazeleyici.calistir(app, planlariDaKur = true)
        } else {
            NotificationScheduler.cancelAll(app)
            CanliSayac.kaldir(app)
        }
    }

    fun setOnceDk(dk: Int) {
        storage.onceHatirlatmaDk = dk
        _state.update { it.copy(onceDk = dk) }
        updateNotifications()
    }

    fun setTebrikler(acik: Boolean) {
        storage.tebrikler = acik
        _state.update { it.copy(tebrikler = acik) }
        updateNotifications()
    }

    fun setSessizDk(dk: Int) {
        storage.sessizDk = dk
        _state.update { it.copy(sessizDk = dk) }
        updateNotifications()
    }

    fun setCanliSayac(acik: Boolean) {
        storage.canliSayac = acik
        _state.update { it.copy(canliSayac = acik) }
        updateNotifications()
    }

    fun setSeyahatModu(acik: Boolean) {
        storage.seyahatModu = acik
        _state.update { it.copy(seyahatModu = acik) }
    }

    // MARK: Kayıtlı konumlar

    fun konumuKaydet(location: SelectedLocation) {
        val liste = (storage.kayitliKonumlar.filter { it.city.id != location.city.id } + location).takeLast(MAX_KAYITLI)
        storage.kayitliKonumlar = liste
        _state.update { it.copy(kayitliKonumlar = liste, message = "Kaydedildi: ${location.title}") }
    }

    fun konumuSil(location: SelectedLocation) {
        val liste = storage.kayitliKonumlar.filter { it.city.id != location.city.id }
        storage.kayitliKonumlar = liste
        _state.update { it.copy(kayitliKonumlar = liste) }
    }

    // MARK: Seyahat modu

    private var sonSeyahatKontrol = 0L
    private var reddedilenIlId: Int? = null

    /**
     * Konum izni varsa sessizce bulunulan ili bulur; kayıtlı konumdan farklı bir ildeyse
     * kullanıcıya "Konumunuz değişti, güncellensin mi?" diye sorulur. Saatte bir kez çalışır.
     */
    fun seyahatKontrol() {
        val s = _state.value
        val mevcut = s.location ?: return
        if (!s.seyahatModu || !locationResolver.hasPermission()) return
        val now = System.currentTimeMillis()
        if (now - sonSeyahatKontrol < 3_600_000L) return
        sonSeyahatKontrol = now
        viewModelScope.launch {
            runCatching {
                val area = locationResolver.resolve()
                val il = PlaceMatcher.bestMatch(area.il, provider.states()) ?: return@runCatching
                if (il.id == mevcut.state.id || il.id == reddedilenIlId) return@runCatching
                val ilceler = provider.cities(il.id)
                val ilce = PlaceMatcher.bestMatch(area.ilce, ilceler) ?: PlaceMatcher.center(il, ilceler) ?: return@runCatching
                _state.update { it.copy(konumOnerisi = SelectedLocation(il, ilce)) }
            }
        }
    }

    fun konumOnerisiniKabulEt() {
        val oneri = _state.value.konumOnerisi ?: return
        _state.update { it.copy(konumOnerisi = null) }
        select(oneri)
    }

    fun konumOnerisiniReddet() {
        reddedilenIlId = _state.value.konumOnerisi?.state?.id
        _state.update { it.copy(konumOnerisi = null) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    // Konum seçici için
    suspend fun states(): List<Place> = provider.states()
    suspend fun cities(stateId: Int): List<Place> = provider.cities(stateId)
}
