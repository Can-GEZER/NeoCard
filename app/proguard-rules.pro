# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ===========================================
# Firebase & Google Services
# ===========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.cloud.firestore.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }

# ===========================================
# Google Play Billing
# ===========================================
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }

# ===========================================
# ZXing QR Code Library
# ===========================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ===========================================
# Kotlin
# ===========================================
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ===========================================
# Jetpack Compose
# ===========================================
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# ===========================================
# Data Classes & Models
# ===========================================
-keep class com.cangzr.neocard.data.** { *; }
-keep class com.cangzr.neocard.ui.screens.UserCard { *; }
-keep class com.cangzr.neocard.ui.screens.TextStyleDTO { *; }
-keep class com.cangzr.neocard.ui.screens.ExploreUserCard { *; }
-keep class com.cangzr.neocard.data.model.** { *; }

# ===========================================
# AdMob
# ===========================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ===========================================
# Coil Image Loading
# ===========================================
-keep class coil.** { *; }
-keep class coil3.** { *; }
-dontwarn coil.**

# ===========================================
# WorkManager
# ===========================================
-keep class androidx.work.** { *; }

# ===========================================
# Navigation Compose
# ===========================================
-keep class androidx.navigation.** { *; }

# ===========================================
# Lifecycle & ViewModel
# ===========================================
-keep class androidx.lifecycle.** { *; }

# ===========================================
# General Android
# ===========================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R class
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# ===========================================
# Debugging
# ===========================================
# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile