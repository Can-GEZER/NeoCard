# 🎴 NeoCard - Digital Business Card Platform

<div align="center">

[![Build Status](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml)
[![Test Coverage](https://codecov.io/gh/YOUR_USERNAME/neo/branch/master/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/neo)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Hilt](https://img.shields.io/badge/Hilt-2.51.1-673AB7.svg?logo=android)](https://dagger.dev/hilt/)
[![Compose](https://img.shields.io/badge/Compose-1.6.0-4285F4.svg?logo=jetpack-compose)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Latest-FFCA28.svg?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Modern digital business card platform for Android**

[Features](#-features) • [Architecture](#-architecture) • [Testing](#-testing) • [Security](#-security--validation-v21) • [Error Handling](#-retry-logic--error-handling-v21) • [Setup](#-setup) • [Tech Stack](#-tech-stack) • [Contributing](#-contributing)

</div>

---

## 📱 About

NeoCard is a modern Android application that allows users to create, manage, and share digital business cards. Built with the latest Android technologies including Jetpack Compose, Hilt, and Firebase, it provides a seamless experience for professional networking in the digital age.

### ✨ Key Highlights

- 🎨 **Beautiful UI** - Modern Material 3 design with Jetpack Compose
- 🔐 **Secure** - Firebase Authentication & Firestore Security Rules with validation
- 🚀 **Fast** - Paging3 for efficient data loading
- 🏗️ **Clean Architecture** - MVVM + Repository + Use Cases
- 💉 **Dependency Injection** - Hilt for scalable architecture
- ✅ **Well Tested** - 164+ unit tests with 75%+ coverage
- 🔄 **CI/CD Ready** - Automated builds, tests, and coverage reports
- 🔁 **Retry Logic** - Automatic retry with exponential backoff for network failures
- 💬 **User-Friendly Errors** - Human-readable error messages in Turkish

---

## 🌟 Features

### 👤 User Management
- ✅ Email/Password authentication
- ✅ Google Sign-In integration
- ✅ User profile management
- ✅ Premium membership system

### 🎴 Card Management
- ✅ Create unlimited digital business cards (Premium)
- ✅ Multiple card types (General, Business, Developer, Designer, etc.)
- ✅ Custom backgrounds (Solid colors, Gradients, Images)
- ✅ Profile image upload with Firebase Storage
- ✅ Social media links (LinkedIn, Instagram, Twitter, GitHub, etc.)
- ✅ QR code generation for cards
- ✅ Public/Private card visibility

### 🔍 Discovery & Networking
- ✅ Explore public cards from other users
- ✅ Search and filter cards by type
- ✅ Connection requests system
- ✅ Share cards via link or QR code

### 📊 Analytics (Coming Soon)
- ⏳ Card view statistics
- ⏳ Connection analytics
- ⏳ Engagement tracking

### 🎨 Customization
- ✅ Custom text styles (Color, Size, Font Weight)
- ✅ Background customization
- ✅ Preview before saving
- ✅ Real-time editing

---

## 📸 Screenshots

<div align="center">

| Splash Screen | Authentication | Home Screen |
|:---:|:---:|:---:|
| ![Splash](screenshots/splash.png) | ![Auth](screenshots/auth.png) | ![Home](screenshots/home.png) |

| Create Card | Card Detail | Profile |
|:---:|:---:|:---:|
| ![Create](screenshots/create.png) | ![Detail](screenshots/detail.png) | ![Profile](screenshots/profile.png) |

</div>

> **Note:** Add your screenshots to `screenshots/` directory

---

## 🏗️ Architecture

NeoCard follows **Clean Architecture** principles with **MVVM** pattern for separation of concerns and testability.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          UI Layer                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐     │
│  │   Compose    │  │  ViewModels  │  │   Resource States     │     │
│  │   Screens    │◄─┤  (@HiltVM)   │◄─┤   + Error Display    │     │
│  │  + Testing   │  └──────────────┘  └──────────────────────┘     │
│  └──────────────┘                                                  │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Domain Layer                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Use Cases (Business Logic)                      │   │
│  │  • GetUserCardsUseCase (Tested)                             │   │
│  │  • SaveCardUseCase (Tested)                                  │   │
│  │  • GetExploreCardsUseCase (Tested)                          │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Data Layer                                     │
│  ┌──────────────┐         ┌──────────────┐                         │
│  │  Repository  │         │ PagingSource │                         │
│  │  Interfaces  │         │              │                         │
│  └──────────────┘         └──────────────┘                         │
│         │                        │                                  │
│         ▼                        ▼                                  │
│  ┌──────────────────────────────────────────────────────┐         │
│  │     Firebase Implementation                          │         │
│  │  • FirebaseCardRepository (Tested)                  │         │
│  │  • FirebaseAuthRepository (Tested)                   │         │
│  │  • CardPagingSource                                  │         │
│  │  • safeApiCall (Retry Logic + Error Mapping)         │         │
│  └──────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Common Utilities                                  │
│  • Resource<T> (Sealed Class)                                       │
│  • safeApiCall (Retry Logic + Exponential Backoff)                  │
│  • ErrorMapper (User-Friendly Error Messages)                       │
│  • ValidationUtils (Input Validation)                               │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Firebase   │
                    │  • Firestore  │
                    │    (Secure    │
                    │     Rules)   │
                    │  • Auth       │
                    │  • Storage    │
                    │  • Analytics  │
                    └───────────────┘
```

### Architecture Components

#### 🎨 **UI Layer**
- **Jetpack Compose** - Modern declarative UI with Material 3
- **ViewModels** - UI state management with `@HiltViewModel` (tested)
- **Resource Pattern** - Sealed class for Loading/Success/Error states
- **Error Display** - User-friendly error components (Snackbar, AlertDialog)
- **Navigation** - Type-safe navigation with Compose Navigation
- **UI Testing** - Compose UI tests with test tags

#### 💼 **Domain Layer**
- **Use Cases** - Single responsibility business logic (all tested)
- **Models** - Domain entities with KDoc documentation
- **Repository Interfaces** - Contract for data operations
- **Business Rules** - Enforced in use cases

#### 📦 **Data Layer**
- **Repositories** - Implementation of repository interfaces (Firebase)
- **Paging3** - Efficient data loading and pagination
- **safeApiCall** - Retry logic with exponential backoff
- **ErrorMapper** - Exception to user message translation
- **ValidationUtils** - Input validation utilities
- **Firebase SDK** - Backend integration with security rules
- **Hilt Modules** - Dependency injection configuration

#### 🔐 **Common Utilities**
- **Resource<T>** - Sealed class for API results
- **safeApiCall** - Automatic retry on transient failures
- **ErrorMapper** - 40+ exception types to Turkish messages
- **ValidationUtils** - Comprehensive input validation

### Key Design Patterns

✅ **MVVM** - Model-View-ViewModel pattern  
✅ **Repository Pattern** - Data source abstraction  
✅ **Use Case Pattern** - Single responsibility principle  
✅ **Dependency Injection** - Hilt for loose coupling  
✅ **Observer Pattern** - StateFlow for reactive UI  
✅ **Factory Pattern** - ViewModel and Repository creation  

---

## 🛠️ Tech Stack

### Core Technologies
- ![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin) **Kotlin 1.9.23** - Modern JVM language
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Build System:** Gradle 8.2 with Kotlin DSL

### UI Framework
- ![Compose](https://img.shields.io/badge/Compose-1.6.0-4285F4.svg) **Jetpack Compose 1.6.0** - Modern declarative UI
- **Material 3** - Material Design components
- **Compose Navigation** - Type-safe navigation
- **Coil 2.5.0** - Image loading library

### Architecture & Dependency Injection
- ![Hilt](https://img.shields.io/badge/Hilt-2.51.1-673AB7.svg) **Hilt 2.51.1** - Dependency injection framework
- **ViewModel** - Lifecycle-aware UI state management
- **Lifecycle** - Lifecycle-aware components
- **SavedStateHandle** - Process death handling

### Backend & Storage
- ![Firebase](https://img.shields.io/badge/Firebase-Latest-FFCA28.svg?logo=firebase) **Firebase Suite**
  - **Authentication** - User authentication (Email + Google Sign-In)
  - **Cloud Firestore** - NoSQL database with security rules
  - **Firebase Storage** - File storage for images
  - **Firebase Analytics** - Usage analytics
  - **Firebase Crashlytics** - Crash reporting
  - **Firebase Functions** - Serverless functions

### Asynchronous & Reactive
- **Kotlin Coroutines** 1.9.0 - Async operations
- **StateFlow/SharedFlow** - Reactive state management
- **Flow Testing** - Turbine for Flow testing

### Pagination
- **Paging 3** 3.3.0 - Efficient data loading
- **Paging Compose** - Compose integration

### Testing Framework
- **JUnit 4** - Unit testing framework
- **MockK** 1.13.12 - Mocking library for Kotlin
- **Turbine** 1.1.0 - Flow testing utility
- **Arch Core Testing** 2.2.0 - Architecture components testing
- **Coroutines Test** 1.9.0 - Coroutine testing utilities
- **JaCoCo** 0.8.11 - Code coverage tool

### Code Quality & Validation
- **Detekt** - Static code analysis
- **Android Lint** - Code quality checks
- **ValidationUtils** - Input validation utilities
- **KDoc** - Comprehensive API documentation

### CI/CD & Automation
- **GitHub Actions** - Automated CI/CD pipeline
  - Build job (APK generation)
  - Test job (unit tests + coverage)
  - Lint job (code quality)
  - Status job (PR comments)
- **JaCoCo** - Test coverage reports
- **Codecov** - Coverage tracking (optional)
- **Gradle Caching** - Faster builds

---

## 📦 Project Structure

```
neo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cangzr/neocard/
│   │   │   │   ├── common/              # Common utilities
│   │   │   │   │   └── Resource.kt      # Resource sealed class
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/           # Data models
│   │   │   │   │   ├── paging/          # Paging sources
│   │   │   │   │   └── repository/      # Repository interfaces & impl
│   │   │   │   │       ├── CardRepository.kt
│   │   │   │   │       ├── AuthRepository.kt
│   │   │   │   │       └── impl/
│   │   │   │   │           ├── FirebaseCardRepository.kt
│   │   │   │   │           └── FirebaseAuthRepository.kt
│   │   │   │   ├── di/                  # Hilt modules
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   └── RepositoryModule.kt
│   │   │   │   ├── domain/
│   │   │   │   │   └── usecase/         # Use cases
│   │   │   │   │       ├── GetUserCardsUseCase.kt
│   │   │   │   │       ├── SaveCardUseCase.kt
│   │   │   │   │       └── GetExploreCardsUseCase.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/         # Compose screens
│   │   │   │   │   │   ├── home/
│   │   │   │   │   │   │   ├── viewmodels/
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── createcard/
│   │   │   │   │   │   ├── profile/
│   │   │   │   │   │   └── carddetail/
│   │   │   │   │   └── theme/           # Material 3 theme
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── NeoCardApplication.kt
│   │   │   ├── res/                     # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                        # Unit tests
│   │       └── java/com/cangzr/neocard/
│   │           └── domain/usecase/
│   │               └── SaveCardUseCaseTest.kt
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml               # Version catalog
├── .github/
│   └── workflows/                       # CI/CD workflows
│       ├── android-ci.yml
│       ├── release.yml
│       └── code-quality.yml
├── firestore.rules                      # Firestore security rules
├── firestore.indexes.json               # Firestore indexes
├── firebase.json                        # Firebase config
└── README.md
```

---

## 🚀 Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Firebase account
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/neo.git
cd neo
```

### 2. Firebase Setup

#### A. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Follow the setup wizard

#### B. Add Android App

1. In Firebase Console, click "Add app" → Android
2. Register your app with package name: `com.cangzr.neocard`
3. Download `google-services.json`
4. Place it in `app/` directory

#### C. Enable Firebase Services

**Authentication:**
1. Go to Authentication → Sign-in method
2. Enable:
   - ✅ Email/Password
   - ✅ Google Sign-In

**Firestore Database:**
1. Go to Firestore Database → Create database
2. Start in **test mode** (we'll deploy rules later)
3. Choose a location (e.g., us-central)

**Storage:**
1. Go to Storage → Get started
2. Use default rules

**Analytics (Optional):**
1. Go to Analytics → Enable

#### D. Deploy Firestore Rules & Indexes

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase project
firebase init

# Select:
# - Firestore (rules and indexes)
# - Use existing project
# - Select your project

# Deploy rules and indexes
firebase deploy --only firestore
```

### 3. Configure Local Environment

#### A. Create `local.properties`

```properties
sdk.dir=/path/to/Android/sdk
```

#### B. (Optional) Create Keystore for Release

```bash
keytool -genkey -v -keystore keystore.jks -alias neocard \
  -keyalg RSA -keysize 2048 -validity 10000
```

Update `app/build.gradle.kts` with your keystore info.

### 4. Build & Run

```bash
# Sync Gradle
./gradlew build

# Run on device/emulator
./gradlew installDebug

# Or use Android Studio
# Click Run ▶️
```

### 5. Verify Setup

- [ ] App builds successfully
- [ ] Authentication works (Email & Google Sign-In)
- [ ] Can create a card
- [ ] Card appears in home screen
- [ ] Can view card details
- [ ] Images upload to Firebase Storage

---

## 🧪 Testing

### Test Coverage (v2.1)

NeoCard has comprehensive test coverage with **164+ unit tests** covering:

- ✅ **Use Cases** (3 use cases, 38+ tests)
  - SaveCardUseCase (14 tests)
  - GetUserCardsUseCase (12 tests)
  - GetExploreCardsUseCase (12+ tests)
  
- ✅ **ViewModels** (2 ViewModels, 50+ tests)
  - HomeViewModel (20 tests)
  - CreateCardViewModel (30 tests)
  
- ✅ **Repositories & Managers** (25+ tests)
  - BillingManager (25 tests)
  - Error handling (24+ tests)
  - Retry logic (13 tests)
  
- ✅ **UI Components** (2 screens, 26+ tests)
  - HomeScreen (11 tests)
  - CreateCardScreen (15 tests)

**Coverage Report:**
```bash
# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View HTML report
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Coverage metrics
# - Line Coverage: ~75%+
# - Branch Coverage: ~70%+
# - Method Coverage: ~80%+
```

### CI/CD Testing Pipeline

GitHub Actions automatically runs tests on every push/PR:

- **Build Job** - Compiles debug & release APKs
- **Test Job** - Runs all unit tests and generates coverage reports
- **Lint Job** - Performs code quality checks

**Coverage Artifacts:**
- HTML coverage report (interactive drill-down)
- XML coverage report (for CI tools like Codecov)
- Test results summary

### Run Unit Tests Locally

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "SaveCardUseCaseTest"

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report (HTML)
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Run Lint Checks

```bash
# Run lint
./gradlew lintDebug

# View lint report
open app/build/reports/lint-results-debug.html
```

### Run Detekt (Static Analysis)

```bash
# Run detekt
./gradlew detekt

# View detekt report
open app/build/reports/detekt/detekt.html
```

---

## 🔐 Security & Validation (v2.1)

### Firestore Security Rules

Enhanced security rules following best practices:

- ✅ **Authentication Required** - All `/users` and `/cards` paths require authentication
- ✅ **Public Cards** - Read-only when `isPublic == true`
- ✅ **Statistics Protection** - Client writes to `/card_statistics` completely disallowed
- ✅ **Data Validation** - Field length checks (name < 100, email format validation)

**Security Features:**
- Explicit authentication checks for all operations
- Owner-only access to user resources
- Server-only writes for statistics
- Comprehensive field validation

See [`firestore.rules`](firestore.rules) for full security configuration.

### Input Validation

**ValidationUtils** provides comprehensive input validation:

- ✅ **Name Validation** - Length (2-50 chars), alphabetic only
- ✅ **Email Validation** - Format check, length limit (100 chars), domain validation
- ✅ **Phone Validation** - Format check, length (7-20 digits)
- ✅ **URL Validation** - Website and social media links
- ✅ **Field Length Checks** - Company, title, bio, etc.

All validation methods return `ValidationResult` (Valid/Invalid with message).

### API Keys Security

All sensitive keys are stored in `local.properties` (excluded from version control):

- ✅ AdMob Application ID and Ad Unit IDs
- ✅ Google Play Billing Public Key
- ✅ BuildConfig integration for secure access
- ✅ No hardcoded keys in source code

See [`SECRETS_MANAGEMENT_GUIDE.md`](SECRETS_MANAGEMENT_GUIDE.md) for setup instructions.

---

## 🔁 Retry Logic & Error Handling (v2.1)

### Automatic Retry Mechanism

**safeApiCall** function implements intelligent retry logic:

- ✅ **Exponential Backoff** - 500ms → 1000ms → 2000ms delays
- ✅ **Smart Retry** - Only retries transient errors (`UNAVAILABLE`)
- ✅ **Maximum Limits** - Up to 3 retries (4 total attempts)
- ✅ **Non-Blocking** - Uses `Dispatchers.IO` to avoid blocking main thread

**Retry Strategy:**
```kotlin
// Automatic retry on network failures
val result = safeApiCall {
    firestore.collection("cards").get().await()
}
// Result is wrapped in Resource<T> with retry handling
```

### User-Friendly Error Messages

**ErrorMapper** translates 40+ exception types to Turkish messages:

- ✅ **Firebase Firestore** - 16 error codes mapped
- ✅ **Firebase Auth** - 10+ error codes mapped
- ✅ **Network Exceptions** - Connection timeout, unavailable, etc.
- ✅ **Generic Exceptions** - Validation errors, null pointers

**Features:**
- Context-aware error titles
- Retry eligibility detection
- Localized Turkish messages

**Example Error Messages:**
- "İnternet bağlantınızı kontrol edin"
- "Bu işlem için yetkiniz yok"
- "Bağlantı zaman aşımına uğradı. Lütfen tekrar deneyin"

**UI Integration:**
- `ErrorDisplay` component for full-screen errors
- `ErrorSnackbarHost` for inline notifications
- `ErrorAlertDialog` for critical errors
- Inline form field validation messages

See [`ERROR_HANDLING_GUIDE.md`](ERROR_HANDLING_GUIDE.md) for complete documentation.

---

## 🔧 Configuration

### Gradle Dependencies

Dependencies are managed in `gradle/libs.versions.toml`:

```toml
[versions]
hilt = "2.51.1"
compose = "1.6.0"
firebase = "32.7.0"
paging = "3.3.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
# ... more dependencies
```

### Hilt Setup

The app uses Hilt for dependency injection:

```kotlin
// Application class
@HiltAndroidApp
class NeoCardApplication : Application()

// Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity()

// ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserCardsUseCase: GetUserCardsUseCase
) : ViewModel()

// Module
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = 
        Firebase.firestore
}
```

### Resource Pattern

The app uses a `Resource` sealed class for managing UI states:

```kotlin
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val exception: Throwable, val message: String?) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

// Usage in ViewModel
private val _uiState = MutableStateFlow<Resource<UiState>>(Resource.Loading)
val uiState: StateFlow<Resource<UiState>> = _uiState

// Usage in Compose
when (val state = uiState.collectAsState().value) {
    is Resource.Loading -> CircularProgressIndicator()
    is Resource.Success -> ShowContent(state.data)
    is Resource.Error -> ShowError(state.message)
}
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

### 1. Fork & Clone

```bash
git clone https://github.com/YOUR_USERNAME/neo.git
cd neo
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Follow existing code style
- Write unit tests for new features
- Update documentation if needed

### 3. Test Your Changes

```bash
./gradlew test
./gradlew lint
./gradlew detekt
```

### 4. Commit & Push

```bash
git add .
git commit -m "feat: Add your feature description"
git push origin feature/your-feature-name
```

### 5. Create Pull Request

- Go to GitHub repository
- Click "New Pull Request"
- Select your branch
- Fill in PR template
- Wait for CI checks to pass

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Write KDoc for public APIs
- Keep functions small and focused

### Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: Add new feature
fix: Fix bug
docs: Update documentation
style: Code style changes
refactor: Code refactoring
test: Add tests
chore: Maintenance tasks
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 NeoCard

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 🙏 Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Firebase](https://firebase.google.com/) - Backend services
- [Hilt](https://dagger.dev/hilt/) - Dependency injection
- [Material Design](https://m3.material.io/) - Design system
- [Coil](https://coil-kt.github.io/coil/) - Image loading

---

## 📞 Contact & Support

- **Issues:** [GitHub Issues](https://github.com/YOUR_USERNAME/neo/issues)
- **Discussions:** [GitHub Discussions](https://github.com/YOUR_USERNAME/neo/discussions)
- **Email:** your.email@example.com

---

## 📝 Version History

### v2.1 (Current) - October 2024
- ✅ Comprehensive test coverage (164+ tests, 75%+ coverage)
- ✅ Enhanced CI/CD pipeline (separate jobs, coverage reports)
- ✅ Security improvements (Firestore rules, API key management)
- ✅ Retry logic with exponential backoff
- ✅ User-friendly error messages (40+ exception types)
- ✅ Input validation utilities
- ✅ KDoc documentation for all public APIs

### v2.0 - September 2024
- ✅ Clean Architecture implementation
- ✅ MVVM pattern with Hilt
- ✅ Jetpack Compose UI
- ✅ Firebase integration
- ✅ Paging3 for data loading

### v1.0 - Initial Release
- ✅ Basic card creation and management
- ✅ Authentication (Email + Google Sign-In)
- ✅ Profile management

---

## 🗺️ Roadmap

### Version 2.2 (Q1 2025)
- [ ] Card analytics dashboard
- [ ] Advanced search and filters
- [ ] Dark mode improvements
- [ ] Offline support with Room database

### Version 2.3 (Q2 2025)
- [ ] Card templates library
- [ ] Bulk card operations
- [ ] Export to PDF/VCF
- [ ] Advanced customization options
- [ ] Multi-language support (i18n)

### Version 3.0 (Q3 2025)
- [ ] NFC card integration
- [ ] Widget support
- [ ] Wear OS companion app
- [ ] AI-powered card suggestions

---

## 📊 Project Status

### v2.1 Highlights

🎯 **Comprehensive Testing**
- 164+ unit tests
- 75%+ code coverage
- ViewModel and UseCase tests
- UI component tests

🔐 **Enhanced Security**
- Firestore security rules best practices
- API keys secured via BuildConfig
- Input validation utilities
- Authentication requirements

🔄 **Reliability Improvements**
- Automatic retry with exponential backoff
- User-friendly error messages (Turkish)
- Error mapping for 40+ exception types
- Smart retry eligibility detection

🚀 **CI/CD Pipeline**
- Separate jobs for build, test, lint
- Automated coverage reports
- Artifact management (APKs + reports)
- PR status comments

### Status Badges

[![Build Status](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml/badge.svg?branch=master)](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml)
[![Test Coverage](https://codecov.io/gh/YOUR_USERNAME/neo/branch/master/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/neo)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Hilt](https://img.shields.io/badge/Hilt-2.51.1-673AB7.svg)](https://dagger.dev/hilt/)
[![Compose](https://img.shields.io/badge/Compose-1.6.0-4285F4.svg?logo=jetpack-compose)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Latest-FFCA28.svg?logo=firebase)](https://firebase.google.com)
[![Code Quality](https://img.shields.io/badge/Code%20Quality-A-success.svg)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)](https://github.com/YOUR_USERNAME/neo/pulls)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

### Project Metrics

- **Total Tests:** 164+
- **Test Coverage:** 75%+
- **CI/CD Jobs:** 4 (build, test, lint, status)
- **Security Rules:** Best practices implemented
- **Error Handling:** 40+ exception types supported

---

<div align="center">

**Made with ❤️ and Kotlin**

[⬆ Back to Top](#-neocard---digital-business-card-platform)

</div>
