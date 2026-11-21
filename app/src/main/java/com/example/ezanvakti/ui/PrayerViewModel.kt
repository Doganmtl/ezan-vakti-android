package com.example.ezanvakti.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ezanvakti.AppConfig
import com.example.ezanvakti.data.DiniGunlerRepository
import com.example.ezanvakti.data.PlaceMatcher
import com.example.ezanvakti.data.PrayerTimesProvider
import com.example.ezanvakti.data.Storage
import com.example.ezanvakti.i18n.Dil
import com.example.ezanvakti.i18n.t
import com.example.ezanvakti.location.LocationResolver
import com.example.ezanvakti.model.HicriTakvim
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
    /** Hicri takvim seçimi ("" = ülkeye göre otomatik) ve gün düzeltmesi. */
    val hicriTakvim: String = "",
    val hicriDuzeltme: Int = 0,
    /** Seçimden ve ülkeden çıkan etkin takvim. */
    val takvimTuru: HicriTakvim.Tur = HicriTakvim.Tur.DIYANET,
    /** Uygulama dili seçimi ("" = telefon dili). */
    val dil: String = "",
    /** Görünüm: arka plan teması, vakit dizilişi, widget arka planı, kilit ekranı sayacı içeriği. */
    val arkaPlan: String = "vakit",
    val vakitDuzeni: String = "liste",
    val widgetStili: String = "koyu",
    val canliSayacModu: String = "siradaki",
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
            hicriTakvim = storage.hicriTakvim,
            hicriDuzeltme = storage.hicriDuzeltme,
            takvimTuru = storage.takvimAyari().tur,
            dil = Dil.secim,
            arkaPlan = storage.arkaPlan,
            vakitDuzeni = storage.vakitDuzeni,
            widgetStili = storage.widgetStili,
            canliSayacModu = storage.canliSayacModu,
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

    /** Tanıtım turu gösterilmeli mi (ilk açılış). */
    fun tanitimGerekli(): Boolean = !storage.tanitimGoruldu

    fun tanitimBitti() {
        storage.tanitimGoruldu = true
    }

    /** Tanıtım turundaki "Konumumu bul": ilk kez ise uygulamayı başlatır, değilse konumu yeniden arar. */
    fun konumuBul() {
        if (!bootstrapped) bootstrap() else useCurrentLocation()
    }

    /** Konumdan il/ilçe bulur ve vakitleri yükler. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.Locating) }
            try {
                val area = locationResolver.resolve()
                val found = konumuEslestir(area) ?: return@launch
                val unchanged = _state.value.location == found
                select(found)
                // Konum değişmediyse de kullanıcıya bulunduğunu bildir (değiştiyse select() zaten bildiriyor).
                if (unchanged) _state.update { it.copy(message = t("Konumunuz doğrulandı: %s", found.title)) }
            } catch (e: Exception) {
                val message = e.message ?: t("Konum alınamadı.")
                _state.update {
                    when {
                        it.location == null -> it.copy(phase = Phase.NeedsLocation(message))
                        it.days.isNotEmpty() -> it.copy(
                            phase = Phase.Ready,
                            message = t("Konum bulunamadı. %s vakitleri gösterilmeye devam ediyor.", it.location.title),
                        )
                        else -> it.copy(phase = Phase.Failed(message))
                    }
                }
            }
        }
    }

    /**
     * Android'in verdiği adresi Diyanet listesindeki konuma çevirir. Bulunamazsa ekranı
     * "elle seçin" durumuna alır ve null döner. [sessiz] ise (seyahat modu) ekrana dokunmaz.
     */
    private suspend fun konumuEslestir(area: LocationResolver.Area, sessiz: Boolean = false): SelectedLocation? {
        fun eksik(mesaj: String?): SelectedLocation? {
            if (!sessiz) _state.update { it.copy(phase = if (it.days.isNotEmpty()) Phase.Ready else Phase.NeedsLocation(mesaj), message = if (it.days.isNotEmpty()) mesaj else null) }
            return null
        }
        if (area.turkiye) {
            val state = PlaceMatcher.bestMatch(area.il, provider.states())
                ?: return eksik(t("\"%s\" ili listede bulunamadı. Lütfen elle seçin.", area.il))
            val cities = provider.cities(state.id)
            val city = PlaceMatcher.bestMatch(area.ilce, cities) ?: PlaceMatcher.center(state, cities) ?: return eksik(null)
            return SelectedLocation(state, city)
        }
        val ulke = PlaceMatcher.bestMatchAny(listOf(area.ulke), provider.countries())
            ?: return eksik(t("%s için Diyanet vakitleri bulunamadı. Lütfen listeden en yakın şehri seçin.", area.ulke ?: area.ulkeKodu))
        val states = provider.states(ulke.id)
        val state = (if (states.size == 1) states.first() else PlaceMatcher.bestMatchAny(area.ilAdaylari, states))
            ?: return eksik(t("Bölgeniz listede bulunamadı. Lütfen listeden en yakın şehri seçin."))
        val cities = provider.cities(state.id)
        val city = PlaceMatcher.bestMatchAny(area.ilceAdaylari, cities) ?: PlaceMatcher.center(state, cities) ?: return eksik(null)
        return SelectedLocation(state, city, ulke)
    }

    fun select(location: SelectedLocation) {
        val previous = _state.value.location
        if (previous != null && previous != location) {
            _state.update { it.copy(message = t("Konum güncellendi: %s", location.title)) }
        }
        storage.location = location
        // Ülke değiştiyse saat dilimi ve hicri takvim de değişir.
        _state.update {
            it.copy(
                location = location,
                days = storage.cachedTimes(location.city.id).orEmpty(),
                dini = diniRepo.loadLocal(),
                takvimTuru = storage.takvimAyari().tur,
            )
        }
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
                val ham = provider.times(location.city.id, location.ulkeId).sortedBy { it.epochDay }
                storage.saveTimes(ham, location.city.id)
                val fresh = storage.hazirla(ham)
                VakitWidget.guncelle(getApplication())
                _state.update {
                    it.copy(days = fresh, lastUpdated = System.currentTimeMillis(), phase = Phase.Ready)
                }
                updateNotifications()
            } catch (e: Exception) {
                // Önbellekte veri varsa onu kullanmaya devam et ve kullanıcıyı bilgilendir.
                _state.update {
                    if (it.days.isEmpty()) {
                        it.copy(phase = Phase.Failed(e.message ?: t("Vakitler alınamadı.")))
                    } else {
                        it.copy(phase = Phase.Ready, message = t("Vakitler güncellenemedi; kayıtlı vakitler gösteriliyor."))
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
        _state.update { it.copy(kayitliKonumlar = liste, message = t("Kaydedildi: %s", location.title)) }
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
                val bulunan = konumuEslestir(area, sessiz = true) ?: return@runCatching
                if (bulunan.state.id == mevcut.state.id || bulunan.state.id == reddedilenIlId) return@runCatching
                _state.update { it.copy(konumOnerisi = bulunan) }
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

    // MARK: Görünüm

    fun setArkaPlan(kod: String) {
        storage.arkaPlan = kod
        _state.update { it.copy(arkaPlan = kod) }
        if (storage.widgetStili == "tema") VakitWidget.guncelle(getApplication())
    }

    fun setVakitDuzeni(kod: String) {
        storage.vakitDuzeni = kod
        _state.update { it.copy(vakitDuzeni = kod) }
    }

    fun setWidgetStili(kod: String) {
        storage.widgetStili = kod
        _state.update { it.copy(widgetStili = kod) }
        VakitWidget.guncelle(getApplication())
    }

    fun setCanliSayacModu(kod: String) {
        storage.canliSayacModu = kod
        _state.update { it.copy(canliSayacModu = kod) }
        Tazeleyici.calistir(getApplication())
    }

    // MARK: Hicri takvim ve dil

    fun setHicriTakvim(secim: String) {
        storage.hicriTakvim = secim
        takvimiYenidenUygula()
    }

    fun setHicriDuzeltme(gun: Int) {
        storage.hicriDuzeltme = gun
        takvimiYenidenUygula()
    }

    private fun takvimiYenidenUygula() {
        val location = _state.value.location
        _state.update {
            it.copy(
                hicriTakvim = storage.hicriTakvim,
                hicriDuzeltme = storage.hicriDuzeltme,
                takvimTuru = storage.takvimAyari().tur,
                dini = diniRepo.loadLocal(),
                days = location?.let { l -> storage.cachedTimes(l.city.id) } ?: it.days,
            )
        }
        updateNotifications()
    }

    /** Dili kaydeder; ekran yeniden oluşturulunca (Activity.recreate) yeni dil görünür. */
    fun setDil(kod: String) {
        Dil.kaydet(getApplication(), kod)
        NotificationScheduler.createChannel(getApplication())
        _state.update { it.copy(dil = kod) }
        updateNotifications()
    }

    // Konum seçici için
    suspend fun countries(): List<Place> = provider.countries()
    suspend fun states(countryId: Int = SelectedLocation.TURKIYE_ID): List<Place> = provider.states(countryId)
    suspend fun cities(stateId: Int, countryId: Int = SelectedLocation.TURKIYE_ID): List<Place> =
        provider.cities(stateId).let { list ->
            // Türkiye'deki ilçe adları her dilde Türkçe yazılır.
            if (countryId == SelectedLocation.TURKIYE_ID) list.map { it.copy(nameEn = null) } else list
        }
}
