# Firestore Security Rules Documentation

Bu dokümant NeoCard uygulamasının Firestore güvenlik kurallarını açıklar.

## 📋 İçindekiler

- [Temel Prensipler](#temel-prensipler)
- [Koleksiyon Kuralları](#koleksiyon-kuralları)
- [Data Validasyonu](#data-validasyonu)
- [Test Etme](#test-etme)
- [Deployment](#deployment)

## 🔐 Temel Prensipler

### Helper Fonksiyonlar

#### `isAuthenticated()`
Kullanıcının giriş yapıp yapmadığını kontrol eder.

```javascript
function isAuthenticated() {
  return request.auth != null;
}
```

#### `isOwner(uid)`
Kullanıcının kaynak sahibi olup olmadığını kontrol eder.

```javascript
function isOwner(uid) {
  return isAuthenticated() && request.auth.uid == uid;
}
```

#### `isValidEmail(email)`
Email formatının geçerli olup olmadığını kontrol eder.

```javascript
function isValidEmail(email) {
  return email.matches('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$');
}
```

#### `isServer()`
İsteğin Cloud Functions'dan gelip gelmediğini kontrol eder.

```javascript
function isServer() {
  return request.auth.token.admin == true;
}
```

## 📚 Koleksiyon Kuralları

### 1. `/users/{uid}`

**Read:** Sadece kayıt sahibi kullanıcı  
**Write:** Sadece kayıt sahibi kullanıcı  
**Validasyon:** 
- Email geçerli format olmalı
- DisplayName 100 karakterden az olmalı

```javascript
match /users/{uid} {
  allow read: if isOwner(uid);
  allow create, update: if isOwner(uid) && validateUserData(request.resource.data);
  allow delete: if isOwner(uid);
}
```

**Örnek Geçerli Data:**
```json
{
  "id": "user123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "premium": false,
  "connectRequests": [],
  "connected": []
}
```

### 2. `/users/{uid}/cards/{cardId}`

**Read:** Sadece kart sahibi kullanıcı  
**Write:** Sadece kart sahibi kullanıcı  
**Validasyon:**
- Name < 100 chars
- Surname < 100 chars
- Email geçerli format
- Company < 200 chars
- Title < 200 chars
- Phone < 20 chars
- Bio < 500 chars

```javascript
match /users/{uid}/cards/{cardId} {
  allow read: if isOwner(uid);
  allow create, update: if isOwner(uid) && validateCardData(request.resource.data);
  allow delete: if isOwner(uid);
}
```

**Örnek Geçerli Data:**
```json
{
  "id": "card123",
  "name": "John",
  "surname": "Doe",
  "email": "john@company.com",
  "phone": "+1234567890",
  "company": "Tech Corp",
  "title": "Software Engineer",
  "cardType": "Business",
  "isPublic": true
}
```

### 3. `/public_cards/{cardId}`

**Read:** Herkes (`isPublic == true` ise)  
**Write:** Sadece kart sahibi veya server

```javascript
match /public_cards/{cardId} {
  allow read: if resource.data.isPublic == true;
  allow create, update: if isAuthenticated() 
    && (request.auth.uid == request.resource.data.userId || isServer());
  allow delete: if isAuthenticated() && request.auth.uid == resource.data.userId;
}
```

**Örnek Geçerli Data:**
```json
{
  "id": "card123",
  "userId": "user123",
  "isPublic": true
}
```

### 4. `/card_statistics/{cardId}`

**Read:** Sadece kart sahibi  
**Write:** Sadece server (Cloud Functions)

```javascript
match /card_statistics/{cardId} {
  allow read: if isAuthenticated() && request.auth.uid == resource.data.cardOwnerId;
  allow create, update: if isServer();
  allow delete: if isServer() || (isAuthenticated() && request.auth.uid == resource.data.cardOwnerId);
}
```

**Örnek Data:**
```json
{
  "cardId": "card123",
  "cardOwnerId": "user123",
  "viewCount": 150,
  "linkClicks": 45,
  "qrScans": 20
}
```

### 5. `/card_analytics/{eventId}`

**Read:** Sadece kart sahibi  
**Create:** Herkes (tracking için)  
**Update/Delete:** Sadece server

```javascript
match /card_analytics/{eventId} {
  allow read: if isAuthenticated() && request.auth.uid == resource.data.cardOwnerId;
  allow create: if request.resource.data.keys().hasAll(['cardId', 'cardOwnerId', 'eventType', 'timestamp']);
  allow update, delete: if isServer();
}
```

### 6. `/connection_requests/{requestId}`

**Read:** Gönderen veya alıcı  
**Create:** Sadece gönderen  
**Update:** Sadece alıcı (accept/reject için)  
**Delete:** Gönderen veya alıcı

```javascript
match /connection_requests/{requestId} {
  allow read: if isAuthenticated() 
    && (request.auth.uid == resource.data.fromUserId || request.auth.uid == resource.data.toUserId);
  allow create: if isAuthenticated() && request.auth.uid == request.resource.data.fromUserId;
  allow update: if isAuthenticated() && request.auth.uid == resource.data.toUserId;
  allow delete: if isAuthenticated() 
    && (request.auth.uid == resource.data.fromUserId || request.auth.uid == resource.data.toUserId);
}
```

## ✅ Data Validasyonu

### User Data
- ✓ Email: Geçerli format
- ✓ DisplayName: Max 100 karakter

### Card Data
- ✓ Name: Max 100 karakter (required)
- ✓ Surname: Max 100 karakter (required)
- ✓ Email: Geçerli format veya boş
- ✓ Company: Max 200 karakter
- ✓ Title: Max 200 karakter
- ✓ Phone: Max 20 karakter
- ✓ Website: Max 500 karakter
- ✓ Social media handles: Max 100 karakter
- ✓ Bio: Max 500 karakter

### Validation Helper Functions

```javascript
function isValidLength(text, maxLength) {
  return text.size() <= maxLength;
}

function isValidEmail(email) {
  return email.matches('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$');
}
```

## 🧪 Test Etme

### Firebase Emulator ile Test

1. Firebase emulator'ı başlat:
```bash
firebase emulators:start
```

2. Test kuralları:
```bash
firebase emulators:exec --only firestore "npm test"
```

### Manuel Test Senaryoları

#### ✅ Başarılı Senaryolar
1. Kullanıcı kendi profilini okuyabilir
2. Kullanıcı kendi kartını oluşturabilir
3. Herkes public kartları okuyabilir
4. Server statistics yazabilir

#### ❌ Başarısız Olması Gereken Senaryolar
1. Kullanıcı başkasının profilini okuyamaz
2. Kullanıcı başkasının kartını silemez
3. Geçersiz email ile kart oluşturulamaz
4. 100 karakterden uzun isim kabul edilmez

### Örnek Test Kodu

```kotlin
@Test
fun testUserCanReadOwnProfile() = runTest {
    // Given
    val userId = "user123"
    
    // When
    val result = firestore.collection("users")
        .document(userId)
        .get()
        .await()
    
    // Then
    assertTrue(result.exists())
}

@Test
fun testUserCannotReadOtherProfile() = runTest {
    // Given
    val otherUserId = "user456"
    
    // When & Then
    assertThrows<PermissionDeniedException> {
        firestore.collection("users")
            .document(otherUserId)
            .get()
            .await()
    }
}
```

## 🚀 Deployment

### Firebase Console Üzerinden

1. Firebase Console'a git
2. Firestore Database > Rules sekmesine tıkla
3. `firestore.rules` dosyasının içeriğini kopyala
4. "Publish" butonuna tıkla

### Firebase CLI ile

```bash
# Deploy rules
firebase deploy --only firestore:rules

# Deploy rules ve indexes
firebase deploy --only firestore
```

### CI/CD Pipeline

```yaml
# GitHub Actions örneği
- name: Deploy Firestore Rules
  run: |
    npm install -g firebase-tools
    firebase deploy --only firestore:rules --token ${{ secrets.FIREBASE_TOKEN }}
```

## 🔒 Güvenlik Best Practices

1. **Her zaman kimlik doğrulama yap**: Hassas verilere erişimde `isAuthenticated()` kontrol et
2. **Ownership doğrula**: Kullanıcının sadece kendi verilerine erişebildiğinden emin ol
3. **Input validasyonu**: Tüm girdi verilerini validate et
4. **Rate limiting**: Cloud Functions'da rate limiting kullan
5. **Audit logs**: Önemli işlemleri logla
6. **Test coverage**: Tüm kuralları test et
7. **Regular reviews**: Güvenlik kurallarını düzenli olarak gözden geçir

## 📊 Monitoring

Firebase Console'da güvenlik kuralı ihlallerini izle:

1. Firestore > Usage sekmesi
2. Security rules violations grafiği
3. Alerts ayarla

## ⚠️ Önemli Notlar

- Server-side işlemler için `admin` claim'i gereklidir
- Cloud Functions'dan yazma yaparken admin SDK kullanın
- Production'da mutlaka güvenlik kurallarını test edin
- Kurallar deploy edildikten sonra 1-2 dakika içinde aktif olur

## 🆘 Sorun Giderme

### "Permission Denied" Hatası

1. Kullanıcının giriş yaptığından emin olun
2. UID'nin doğru olduğunu kontrol edin
3. Firestore Console'da kuralları kontrol edin
4. Emulator ile test edin

### "Invalid Data" Hatası

1. Validation kurallarını kontrol edin
2. Gönderilen data formatını doğrulayın
3. Required alanların eksik olmadığından emin olun

## 📝 Changelog

### v1.0.0 (Current)
- Initial security rules
- User, cards, public_cards, statistics rules
- Data validation
- Connection requests rules
- Analytics tracking rules

## 🔗 İlgili Dökümanlar

- [Firebase Security Rules Docs](https://firebase.google.com/docs/firestore/security/get-started)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Security Rules Testing](https://firebase.google.com/docs/rules/unit-tests)

