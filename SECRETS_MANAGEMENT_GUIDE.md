# Secrets & API Keys Management Guide

## 🔐 Genel Bakış

Bu proje, API key'lerini ve gizli bilgileri güvenli şekilde yönetmek için `local.properties` dosyası ve `BuildConfig` kullanır. Bu sayede hardcoded key'ler koddan kaldırılmış ve version control'e commit edilmemesi sağlanmıştır.

---

## ✅ Yapılan Değişiklikler

### 1. local.properties Configuration
**Dosya:** `local.properties`

**Eklenen API Keys:**
```properties
# AdMob Configuration
ADMOB_APPLICATION_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
ADMOB_BANNER_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
ADMOB_INTERSTITIAL_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX

# Google Play Billing Configuration
BILLING_PUBLIC_KEY=base64_encoded_key_here
```

**⚠️ ÖNEMLİ:**
- `local.properties` dosyası **ASLA** version control'e commit edilmemelidir
- `.gitignore` dosyası zaten `local.properties`'i exclude ediyor ✅
- Her geliştirici kendi `local.properties` dosyasını oluşturmalıdır

---

### 2. build.gradle.kts Updates
**Dosya:** `app/build.gradle.kts`

**Yeni Özellikler:**

#### BuildConfig Aktif Edildi
```kotlin
buildFeatures {
    compose = true
    buildConfig = true  // ✅ Eklendi
}
```

#### local.properties Okuma
```kotlin
// Load secrets from local.properties
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
```

#### BuildConfig Fields Oluşturma
```kotlin
defaultConfig {
    // Load AdMob keys from local.properties
    val admobAppId = localProperties.getProperty("ADMOB_APPLICATION_ID", "")
    val admobBannerId = localProperties.getProperty("ADMOB_BANNER_AD_UNIT_ID", "")
    val admobInterstitialId = localProperties.getProperty("ADMOB_INTERSTITIAL_AD_UNIT_ID", "")
    val billingPublicKey = localProperties.getProperty("BILLING_PUBLIC_KEY", "")
    
    // BuildConfig fields
    buildConfigField("String", "ADMOB_APPLICATION_ID", "\"$admobAppId\"")
    buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialId\"")
    buildConfigField("String", "BILLING_PUBLIC_KEY", "\"$billingPublicKey\"")
    
    // Manifest placeholders
    manifestPlaceholders["admobApplicationId"] = admobAppId
}
```

---

### 3. AndroidManifest.xml Updates
**Dosya:** `app/src/main/AndroidManifest.xml`

**Değişiklik:**
```xml
<!-- Before (HARDCODED) ❌ -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>

<!-- After (SECURED) ✅ -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobApplicationId}"/>
```

**Nasıl Çalışır:**
- `manifestPlaceholders` build.gradle.kts'te tanımlı
- Build sırasında `${admobApplicationId}` yerine gerçek değer konulur
- Value `local.properties`'den okunur

---

### 4. AdManager.kt Updates
**Dosya:** `app/src/main/java/com/cangzr/neocard/ads/AdManager.kt`

**Değişiklik:**
```kotlin
// Before (HARDCODED) ❌
companion object {
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
}

// After (SECURED) ✅
import com.cangzr.neocard.BuildConfig

companion object {
    private val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    private val INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
}
```

---

### 5. .gitignore Verification
**Dosya:** `.gitignore`

**Kontrol Sonuçları:**
- ✅ `local.properties` exclude edilmiş (satır 88)
- ✅ `google-services.json` exclude edilmiş (satır 41-42)
- ✅ `.local.properties` exclude edilmiş (satır 4)

**Sonuç:** Tüm secret dosyaları version control'den korunuyor ✅

---

## 📋 Setup Instructions

### Yeni Geliştiriciler İçin

#### 1. local.properties Oluştur
Proje root dizininde `local.properties` dosyası oluştur:

```properties
# Android SDK Location
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk

# AdMob Configuration
ADMOB_APPLICATION_ID=your-admob-application-id-here
ADMOB_BANNER_AD_UNIT_ID=your-banner-ad-unit-id-here
ADMOB_INTERSTITIAL_AD_UNIT_ID=your-interstitial-ad-unit-id-here

# Google Play Billing Configuration
BILLING_PUBLIC_KEY=your-base64-billing-key-here
```

#### 2. Gerçek API Keys'i Ekle

**AdMob Keys:**
1. [AdMob Console](https://apps.admob.com/)'a git
2. Your App > App Settings > App ID'yi kopyala
3. Ad Units > Banner/Interstitial ID'leri kopyala

**Billing Public Key:**
1. [Google Play Console](https://play.google.com/console/)'a git
2. Your App > Monetization > Monetization setup
3. License Testing bölümünden Public Key'i kopyala

#### 3. Build ve Test
```bash
./gradlew clean build
```

---

## 🔍 API Key Locations

### AdMob Application ID
**Kullanım Yerleri:**
- ✅ `AndroidManifest.xml` → `<meta-data>` tag
- ✅ `build.gradle.kts` → `manifestPlaceholders`

**Format:**
```
ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
```

### AdMob Ad Unit IDs
**Kullanım Yerleri:**
- ✅ `AdManager.kt` → `BuildConfig.ADMOB_BANNER_AD_UNIT_ID`
- ✅ `AdManager.kt` → `BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID`

**Format:**
```
ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```

### Billing Public Key
**Kullanım Yerleri:**
- ✅ `BuildConfig.BILLING_PUBLIC_KEY` → Hazır, kullanıma göre implement edilebilir

**Format:**
```
Base64 encoded string
```

---

## 🛡️ Güvenlik Best Practices

### ✅ DO's (Yapılması Gerekenler)

1. **local.properties kullan**
   - Tüm secret key'leri `local.properties`'e koy
   - Bu dosya `.gitignore`'da olmalı

2. **BuildConfig kullan**
   - Kod içinde `BuildConfig.XXX` ile eriş
   - Hardcoded value yok

3. **Environment Variables (Opsiyonel)**
   - CI/CD için environment variables kullan
   - GitHub Actions, Jenkins, vb.

4. **Test Keys kullan**
   - Development için Google'ın test keys'lerini kullan
   - Production key'leri sadece release build'de

### ❌ DON'Ts (Yapılmaması Gerekenler)

1. **Hardcoded Keys Yazma**
   ```kotlin
   // ❌ YAPMA!
   private const val AD_UNIT_ID = "ca-app-pub-..."
   ```

2. **local.properties Commit Etme**
   ```bash
   # ❌ YAPMA!
   git add local.properties
   ```

3. **Secret'ları String Resources'a Koyma**
   ```xml
   <!-- ❌ YAPMA! -->
   <string name="admob_app_id">ca-app-pub-...</string>
   ```

4. **Public Repository'e Secret'ları Push Etme**
   - Secret'ları GitHub'a push etme
   - Code review'lerde dikkatli ol

---

## 🔄 Migration Checklist

Mevcut projeler için migration:

- [x] ✅ `local.properties` template oluşturuldu
- [x] ✅ `build.gradle.kts` BuildConfig eklendi
- [x] ✅ `AndroidManifest.xml` hardcoded key kaldırıldı
- [x] ✅ `AdManager.kt` hardcoded key kaldırıldı
- [x] ✅ `.gitignore` kontrol edildi ve doğru
- [ ] ⏳ Diğer dosyalarda hardcoded key var mı kontrol et
- [ ] ⏳ Team'e secret management hakkında bilgi ver

---

## 🧪 Testing

### BuildConfig Doğrulama

Build'den sonra BuildConfig class'ını kontrol et:
```kotlin
// Generated at: app/build/generated/source/buildConfig/.../BuildConfig.java
public final class BuildConfig {
    public static final String ADMOB_APPLICATION_ID = "...";
    public static final String ADMOB_BANNER_AD_UNIT_ID = "...";
    public static final String ADMOB_INTERSTITIAL_AD_UNIT_ID = "...";
    public static final String BILLING_PUBLIC_KEY = "...";
}
```

### Runtime Test
```kotlin
// Debug'da key'lerin doğru yüklendiğini kontrol et
Log.d("Config", "AdMob App ID: ${BuildConfig.ADMOB_APPLICATION_ID}")
Log.d("Config", "Banner ID: ${BuildConfig.ADMOB_BANNER_AD_UNIT_ID}")
```

---

## 📁 Dosya Yapısı

```
project-root/
├── .gitignore              ✅ local.properties exclude
├── local.properties         ✅ Secrets (NOT in git)
├── app/
│   ├── build.gradle.kts    ✅ BuildConfig fields
│   ├── google-services.json ✅ Firebase config (NOT in git)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  ✅ Uses manifestPlaceholders
│           └── java/
│               └── .../ads/
│                   └── AdManager.kt ✅ Uses BuildConfig
```

---

## 🚨 Troubleshooting

### BuildConfig Fields Boş Görünüyor

**Sorun:** `BuildConfig.ADMOB_APPLICATION_ID` boş string

**Çözüm:**
1. `local.properties` dosyasının root'ta olduğunu kontrol et
2. Key'lerin doğru isimlendirildiğini kontrol et
3. Build'den sonra `app/build/generated/.../BuildConfig.java` dosyasını kontrol et

### Manifest Error: "admobApplicationId not found"

**Sorun:** Build sırasında manifest placeholder bulunamıyor

**Çözüm:**
1. `build.gradle.kts`'te `manifestPlaceholders["admobApplicationId"]` tanımlı mı kontrol et
2. `local.properties`'te `ADMOB_APPLICATION_ID` var mı kontrol et

### AdMob Ads Not Showing

**Sorun:** Ads gösterilmiyor

**Çözüm:**
1. BuildConfig'te key'lerin doğru yüklendiğini kontrol et
2. Test keys kullanıyorsan Production'a geç
3. AdMob Console'da ad unit'lerin aktif olduğunu kontrol et

---

## 📚 Kaynaklar

- [Google AdMob Setup](https://developers.google.com/admob/android/quick-start)
- [Google Play Billing Security](https://developer.android.com/google/play/billing/security)
- [Android BuildConfig](https://developer.android.com/reference/android/os/BuildConfig)
- [Gradle Properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)

---

## ✅ Tamamlanan İşler

- [x] ✅ `local.properties` template oluşturuldu
- [x] ✅ `build.gradle.kts` BuildConfig integration
- [x] ✅ `AndroidManifest.xml` manifestPlaceholders kullanımı
- [x] ✅ `AdManager.kt` BuildConfig kullanımı
- [x] ✅ `.gitignore` doğrulandı
- [x] ✅ Hardcoded keys kaldırıldı
- [x] ✅ Dokümantasyon oluşturuldu

---

*Secrets Management Guide - NeoCard Project*
*Date: 29 Ekim 2024*

