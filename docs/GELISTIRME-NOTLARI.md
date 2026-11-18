# Ezan Vakti (Android) — v0.6.1

Kullanıcının konumuna göre Diyanet ezan vakitlerini gösteren Android uygulaması: Türkiye'de il/ilçe,
yurt dışında Diyanet'in vakit yayımladığı ~200 ülkede eyalet/şehir. 12 dil.
Kotlin + Jetpack Compose. iOS sürümüyle aynı yapı ve aynı tasarım.

## Windows'ta çalıştırma

1. **Android Studio'yu kurun**: https://developer.android.com/studio (ücretsiz). Kurulumda Android SDK ve emülatör de iner.
2. Bu klasörü açın: *File → Open → EzanVaktiAndroid*. İlk açılışta Gradle bağımlılıkları indirilir (birkaç dakika).
   Android Studio Gradle/AGP sürümünü güncellemeyi önerirse kabul edebilirsiniz.
3. **Emülatör oluşturun**: *Device Manager → Create Virtual Device → Pixel 8 →* bir **"Google Play"** ya da **"Google APIs"** sistem görüntüsü seçin (konum servisi için gerekli).
   Tablet görünümünü denemek için ayrıca *Pixel Tablet* oluşturun.
4. Yeşil ▶ (Run) düğmesine basın.
5. **Emülatöre konum verin**: emülatör yanındaki *⋯ (Extended controls) → Location* → haritada İstanbul/Ankara vb. bir nokta seçip *Set location*.
   Sonra uygulamada *Konumumu kullan*.

### Kendi Android telefonunuzda denemek
Telefonda *Ayarlar → Telefon hakkında → Yapı numarası*na 7 kez dokunun → *Geliştirici seçenekleri → USB hata ayıklama*'yı açın → USB ile bağlayıp Android Studio'da cihazı seçip ▶.

## Neler var
- Konumla otomatik il/ilçe bulma (yaklaşık konum izni yeterli); eşleşmezse il merkezi, izin yoksa elle seçim.
- Şu anki vakit, "… namazını kaçırmamak için kalan süre" geri sayımı ve ilerleme halkası.
- Vakit girdikten sonraki 15 dakika "… vakti girdi" rozeti.
- Yerel bildirimler (imsak, öğle, ikindi, akşam, yatsı; varsayılan bildirim sesi, ezan sesi yok). Telefon yeniden başlayınca kendiliğinden yeniden kurulur.
  Android 12+ için Ayarlar'dan "tam zamanlı bildirim" izni verilirse bildirim tam dakikasında gelir.
- 30 günlük vakitler telefonda saklanır; internetsiz çalışır.
- Telefonda dikey düzen, tablette/yatayda (600dp+) iki sütun. Vakte göre değişen arka plan, TalkBack etiketleri.

## Sürüm notları
**0.6.1**
- **Geri bildirim** (Ayarlar → Geri bildirim): kullanıcı en fazla 100 karakter yazar; "Gönder" telefonun e-posta uygulamasını
  doganmutlu.apps@outlook.com adresine hazır mesajla açar (sürüm, dil, ülke ve Android sürümü altına eklenir). Sunucu ya da şifre yok.

**0.6.0**
- **Harf düzeltmesi**: yurt dışı yer adlarında Türkçe büyük/küçük harf kuralı uygulanmıyor ("Calıfornıa" yerine "California");
  Türkçe adlar (ülkeler, Türkiye ve KKTC'deki yerler) yine Türkçe kuralla yazılıyor.
- **Daha derli toplu ana ekran**: geri sayım halkası ve yazılar küçültüldü, vakit satırları sıkılaştırıldı.
- **Görünüm ayarları**: arka plan teması (vakte göre, gece mavisi, zümrüt, mor, gün batımı, deniz, siyah/OLED) ve
  vakitlerin dizilişi (alt alta, ikili, üçlü kutucuk).
- **Widget'lar**: büyük (mevcut), küçük (2×1: sıradaki vakit ve sayaç) ve şerit (4×1: günün vakitleri yan yana);
  widget arka planı koyu, şeffaf ya da uygulama temasıyla aynı.
- **Kilit ekranı sayacı**: yalnızca sıradaki vakit, sıradaki vakit + günün vakitleri ya da yalnızca Ramazan'da.
- Kaynak notu netleştirildi: vakitler namazvakitleri.diyanet.gov.tr ile birebir karşılaştırıldı (Ankara ve Albany, ABD; 19–21 Kasım 2026, birebir aynı).

**0.5.0**
- **Dünya ülkeleri**: konum seçicide Ülke → il/eyalet → ilçe/şehir; tek bölgeli ülkelerde bölge adımı atlanır.
  "Konumumu kullan" ve seyahat modu yurt dışında da çalışır (Android'in İngilizce adresi Diyanet'in İngilizce adlarıyla eşleştirilir).
  Türkiye vakitleri eskisi gibi veri deposundan, yurt dışı vakitleri Diyanet aynasından gelir.
- **Yerel saat**: her günün UTC farkı Diyanet verisinden okunur (yaz saati dahil); vakitler, geri sayım, bildirimler ve
  09:00 tebriki seçili konumun saatine göredir. Telefon aynı saat dilimindeyse telefonun dilimi kullanılır.
- **12 dil**: Türkçe, İngilizce, Arapça, Almanca, Fransızca, İspanyolca, Portekizce, Rusça, Endonezce, Urduca, Farsça, Bengalce.
  Varsayılan telefonun dili (desteklenmiyorsa İngilizce); Ayarlar → Dil'den değiştirilebilir. Arapça, Urduca ve Farsçada
  sağdan sola düzen. Çeviriler `i18n/` altında (anahtar: Türkçe metin; eksik çeviri Türkçe görünür). Ana dili olan biri
  tarafından gözden geçirilmeleri gerekir.
- **Ülkeye göre hicri takvim**: Ayarlar → Hicri takvim: "Ülkeye göre" (Suudi Arabistan, BAE, Katar, Kuveyt, Bahreyn,
  Yemen'de Ümmülkurâ, diğerlerinde Diyanet), Diyanet ya da Ümmülkurâ; hilal gözlemine göre ±2 gün düzeltme.
  Kandil, bayram, Ramazan, imsakiye ve günlük hicri tarih buna göre kayar; dini günlerin listesi her zaman Diyanet'tendir.
- **Tebrikler**: dini bayram ve kandil tebrikleri her ülkede; Türkiye'nin resmî günleri ve 8 Mart yalnızca Türkiye'deki konumlarda.

**0.4.0**
- **Vakitten önce hatırlatma**: Kapalı / 10 / 15 / 30 / 45 dk (güneş doğuşu için "sabah namazı için son dakikalar").
- **Namazda sessize alma**: vakit girince "Rahatsız Etmeyin" (öncelikli) 20/30/45 dk; telefon zaten sessizse dokunmaz,
  yalnızca kendi açtığı sessizliği geri kapatır. "Rahatsız Etmeyin erişimi" izni ister.
- **Kilit ekranında canlı geri sayım**: sessiz, sürekli bildirim; Ramazan'da iftar/sahura sayar.
- **Kayıtlı konumlar** (en çok 8) ve **seyahat modu**: başka bir ile gidilince "Konumunuz değişmiş, güncellensin mi?" diye sorar.
- **Bayram ve kandil tebrikleri** (sabah 09:00): resmî bayram/tatiller (kurucu liderleri anarak), Ramazan ve Kurban
  Bayramı'nın ilk günü ve kandiller. Metinler `model/Tebrikler.kt`'de; Ayarlar'dan kapatılabilir.
- **Ramazan'da widget** iftar ya da sahura sayar, "Ramazan'ın N. günü" yazar.
- Arka planda tazeleyici: her vakit değişiminde widget ve canlı sayaç yenilenir; bildirim planı günde bir kayıtlı vakitlerle
  yeniden kurulur (uygulama günlerce açılmasa da bildirimler kesilmez).

**0.3.0**
- Alt çubuk beş sekme: Vakitler, Takvim, Zikir, Kıble, Diğer.
- **Zikirmatik**: dokunarak ya da ses tuşlarıyla sayma, titreşim, 33/99/100/hedefsiz/özel hedef, tur sayısı, geri al,
  onaylı sıfırlama, toplam zikir sayısı. **Namaz tesbihatı** modu: 33 Sübhânallah → 33 Elhamdülillah → 33 Allâhu Ekber.
  Sayım her dokunuşta kaydedilir; zikir ekranındayken ekran kapanmaz.
- **Kıble pusulası**: konumdan kıble açısı ve Kâbe'ye uzaklık, manyetik sapma düzeltmesi, hizalanınca yeşil + titreşim,
  kalibrasyon uyarısı. Konum alınamazsa seçili ilçenin koordinatı kullanılır.
- **Diğer**: kaza takibi (5 vakit + vitir + oruç), widget ekleme kısayolu.
  (Esmâ-i Hüsnâ ve günün ayeti/hadisi, metinler Diyanet kaynağından izinli alınabilene kadar bekletiliyor.)
- **Ana ekran widget'ı**: konum, sıradaki vakit, canlı geri sayım ve günün altı vakti (şu anki vakit vurgulu).
- **Cuma**: Cuma vaktinden 45 dk önce hatırlatma, öğle bildirimi "Cuma vakti girdi", Cuma sabahı Kehf suresi hatırlatması
  (Ayarlar'dan ayrı ayrı kapatılabilir).

**0.2.0**
- Dini günler takvimi (kandiller, bayramlar, arefe), Ramazan kartı, imsakiye, sahur hatırlatıcısı, kandil bildirimi.

**0.1.1**
- Konum servisi 10 sn içinde yanıt vermezse bekleme bırakılır (eskiden süresiz bekleyebiliyordu).
- Konum bulunamazsa veya vakitler güncellenemezse alt kısımda kısa bir bilgi mesajı çıkar; kayıtlı vakitler gösterilmeye devam eder.
- Android 12+ cihazlarda "Alarm ve hatırlatıcılar" izni yoksa ana ekranda uyarı şeridi çıkar; "İzin ver" ile ayar açılır, izin verilince bildirimler tam zamanlı olarak yeniden kurulur.
- Android Gradle Plugin, Android Studio'nun yükseltme asistanıyla 9.x'e güncellendi (kök `build.gradle.kts` ve Gradle sürümü).

## Veri kaynağı
`app/src/main/java/com/example/ezanvakti/AppConfig.kt`:
- **Resmi: Diyanet Awqat Salah API** — e-posta/şifre girildiğinde kullanılır (başvuru: awqatsalah.diyanet.gov.tr).
- **Geliştirme yedeği: ezanvakti.emushaf.net** — Diyanet verisinin aynası, şifre gerektirmez. Şifre boşsa bu kullanılır.

> ⚠️ Play Store'a çıkmadan önce Diyanet şifresini uygulamadan çıkarıp küçük bir sunucuya taşıyın.

> 🔎 Doğrulanacak: Awqat Salah alan adları ve Türkiye ülke kimliği (`2`) bu ortamdan canlı test edilemedi; proje de burada derlenemedi (Android SDK yok). İlk derlemede çıkan hataları bana iletmeniz yeterli.

## Proje yapısı
```
app/src/main/java/com/example/ezanvakti/
  AppConfig.kt, MainActivity.kt
  model/      Vakit, DailyTimes, VakitDurumu, VakitCalculator (geri sayım mantığı)
  data/       Diyanet ve ayna sağlayıcılar, il/ilçe eşleştirme, önbellek
  location/   Konum → il/ilçe
  notify/     Bildirim planlama, alarm ve açılış alıcıları
  ui/         Ana ekran, geri sayım kartı, vakit listesi, konum seçici, ayarlar
```

## Play Store'a giden yol
1. Google Play Console geliştirici hesabı (tek seferlik 25 $).
2. `applicationId`'yi kendi paket adınızla değiştirin, uygulama simgesini tamamlayın.
3. *Build → Generate Signed App Bundle* ile .aab oluşturup önce "Dahili test" kanalına yükleyin.
4. Kişisel hesaplarda üretime çıkmadan önce bir süre kapalı test (en az 12 test kullanıcısı, 14 gün) şartı var.
