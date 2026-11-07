# TimLyrics (Fabric 1.21.11)

TimSesBot'ta çalan şarkının sözlerini oyunda havada, **herkes için aynı koordinatta** gösterir.
Yalnızca istemci modudur: normal sunucuda, Essential'da ya da tek kişilik dünyada çalışır, sunucuya mod gerekmez.

## Jar'ı derleme (GitHub)
1. Bu klasörü yeni bir GitHub deposuna yükle (`git push`).
2. Depoda **Actions → Jar derle** çalışır (ya da "Run workflow").
3. Bitince çalıştırmanın altındaki **timlyrics-jar** dosyasını indir, zip'ten çıkan `timlyrics-*.jar` dosyasını `mods` klasörüne at.
   Fabric Loader ve Fabric API gerekir (LyricsFlow'da zaten kullanıyorsun).

Yerelde derlemek için: `gradle build` (Java 21) → `build/libs/`.
"Could not resolve" hatası olursa `gradle.properties` içindeki sürümleri https://fabricmc.net/develop adresindeki güncel değerlerle değiştir.

## Kullanım
1. Botun arkadaş paneli adresini al (ücretsiz tünel adresi, `runtime/arkadas-paneli-*.txt` içinde).
2. Oyunda **J** tuşu → menü. Adresi yapıştır, **Kaydet**, sonra **Discord ile eşleştir**.
3. Panoya kopyalanan `!panel KOD` komutunu, ses kanalındayken Discord'da botun görebildiği kanala yaz.
4. **Sözleri buraya as** → bakış yönünün 4 blok önüne asılır. Aynı botla eşleşen herkes aynı noktada görür.
5. Boyut, satır sayısı ve zaman farkı (±100 ms) menüden ayarlanır ve herkese yansır.

Her arkadaş kendi modunu kurup aynı bot adresiyle eşleşmeli. Bir kişi asınca (ya da ayar değiştirince) hepsinde güncellenir.

## Notlar
- Sözler LRCLIB'den botun çektiği zaman damgalı sözlerdir; bulunamazsa şarkı adı gösterilir.
- Yazı blokların arkasında da görünür (duvardan geçer). Menüde "Sunucu adını kontrol et" şimdilik config dosyasındadır (`checkWorld`).
- Tuş ataması Ayarlar → Kontroller → TimLyrics bölümünden değişir.
