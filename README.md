# 🎴 NeoCard - Digital Business Card Platform

<div align="center">

[![Android CI](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/neo/actions/workflows/android-ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Firebase](https://img.shields.io/badge/Firebase-Latest-orange.svg)](https://firebase.google.com)

**Modern digital business card platform for Android**

[Features](#-features) • [Architecture](#-architecture) • [Setup](#-setup) • [Tech Stack](#-tech-stack) • [Contributing](#-contributing)

</div>

---

## 📱 About

NeoCard is a modern Android application that allows users to create, manage, and share digital business cards. Built with the latest Android technologies including Jetpack Compose, Hilt, and Firebase, it provides a seamless experience for professional networking in the digital age.

### ✨ Key Highlights

- 🎨 **Beautiful UI** - Modern Material 3 design with Jetpack Compose
- 🔐 **Secure** - Firebase Authentication & Firestore Security Rules
- 🚀 **Fast** - Paging3 for efficient data loading
- 🏗️ **Clean Architecture** - MVVM + Repository + Use Cases
- 💉 **Dependency Injection** - Hilt for scalable architecture
- ✅ **Tested** - Unit tests with MockK and Turbine
- 🔄 **CI/CD** - Automated builds with GitHub Actions

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
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Compose    │  │  ViewModels  │  │   States     │      │
│  │   Screens    │◄─┤  (@HiltVM)   │◄─┤  (Resource)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Use Cases (Business Logic)              │   │
│  │  • GetUserCardsUseCase                              │   │
│  │  • SaveCardUseCase                                   │   │
│  │  • GetExploreCardsUseCase                           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │  Repository  │         │ PagingSource │                  │
│  │  Interfaces  │         │              │                  │
│  └──────────────┘         └──────────────┘                  │
│         │                        │                           │
│         ▼                        ▼                           │
│  ┌──────────────────────────────────────────┐              │
│  │     Firebase Implementation               │              │
│  │  • FirebaseCardRepository                │              │
│  │  • FirebaseAuthRepository                │              │
│  │  • CardPagingSource                      │              │
│  └──────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Firebase   │
                    │  • Firestore  │
                    │  • Auth       │
                    │  • Storage    │
                    │  • Analytics  │
                    └───────────────┘
```

### Architecture Components

#### 🎨 **UI Layer**
- **Jetpack Compose** - Modern declarative UI
- **ViewModels** - UI state management with `@HiltViewModel`
- **Resource Pattern** - Sealed class for Loading/Success/Error states
- **Navigation** - Type-safe navigation with Compose Navigation

#### 💼 **Domain Layer**
- **Use Cases** - Single responsibility business logic
- **Models** - Domain entities
- **Repository Interfaces** - Contract for data operations

#### 📦 **Data Layer**
- **Repositories** - Implementation of repository interfaces
- **Paging3** - Efficient data loading and pagination
- **Firebase SDK** - Backend integration
- **Hilt Modules** - Dependency injection configuration

### Key Design Patterns

✅ **MVVM** - Model-View-ViewModel pattern  
✅ **Repository Pattern** - Data source abstraction  
✅ **Use Case Pattern** - Single responsibility principle  
✅ **Dependency Injection** - Hilt for loose coupling  
✅ **Observer Pattern** - StateFlow for reactive UI  
✅ **Factory Pattern** - ViewModel and Repository creation  

---

## 🛠️ Tech Stack

### Core
- **Language:** Kotlin 1.9.23
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.2 with Kotlin DSL

### UI
- **Jetpack Compose** 1.6.0 - Modern declarative UI
- **Material 3** - Material Design components
- **Compose Navigation** - Navigation component
- **Coil** - Image loading library

### Architecture & DI
- **Hilt** 2.51.1 - Dependency injection
- **ViewModel** - Lifecycle-aware UI state management
- **Lifecycle** - Lifecycle-aware components
- **SavedStateHandle** - Process death handling

### Backend & Storage
- **Firebase Authentication** - User authentication
- **Cloud Firestore** - NoSQL database
- **Firebase Storage** - File storage
- **Firebase Analytics** - Usage analytics

### Asynchronous
- **Kotlin Coroutines** 1.9.0 - Async operations
- **StateFlow/SharedFlow** - Reactive state management

### Pagination
- **Paging 3** 3.3.0 - Efficient data loading
- **Paging Compose** - Compose integration

### Testing
- **JUnit** - Unit testing framework
- **MockK** 1.13.12 - Mocking library
- **Turbine** 1.1.0 - Flow testing
- **Arch Core Testing** 2.2.0 - Architecture components testing
- **Coroutines Test** 1.9.0 - Coroutine testing utilities

### Code Quality
- **Detekt** - Static code analysis
- **Lint** - Android lint checks

### CI/CD
- **GitHub Actions** - Automated builds and tests
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

### Run Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test
./gradlew test --tests "SaveCardUseCaseTest"

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Run Lint Checks

```bash
# Run lint
./gradlew lint

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

## 🗺️ Roadmap

### Version 1.1 (Q1 2025)
- [ ] Card analytics dashboard
- [ ] Advanced search and filters
- [ ] Dark mode improvements
- [ ] Offline support

### Version 1.2 (Q2 2025)
- [ ] Card templates
- [ ] Bulk card operations
- [ ] Export to PDF/VCF
- [ ] Advanced customization options

### Version 2.0 (Q3 2025)
- [ ] NFC card integration
- [ ] Widget support
- [ ] Wear OS companion app
- [ ] Multi-language support

---

## 📊 Project Status

[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![Coverage](https://img.shields.io/badge/Coverage-75%25-yellow.svg)]()
[![Code Quality](https://img.shields.io/badge/Code%20Quality-A-success.svg)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)]()

---

<div align="center">

**Made with ❤️ and Kotlin**

[⬆ Back to Top](#-neocard---digital-business-card-platform)

</div>
