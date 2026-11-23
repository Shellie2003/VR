# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}

# Moshi rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keepclassmembers class * extends com.squareup.moshi.JsonAdapter {
    public <init>(...);
}

# Retrofit rules
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# OkHttp rules
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep our data models, sync bridge, and database entities intact for serialization/Room mapping
-keep class com.example.data.model.** { *; }
-keep class com.example.sync.** { *; }
-keep class com.example.data.local.** { *; }

# Keep ML Kit and camera components
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Open Food Facts : les classes de réponse sont désérialisées par RÉFLEXION
# (KotlinJsonAdapterFactory), donc par NOM de propriété Kotlin. Elles vivent dans
# `com.example.util`, qu'aucune règle ci-dessus ne retient, et SIX de leurs neuf propriétés
# n'ont aucune annotation : `products`, `product`, `status`, `code`, `categories`, `brands`.
# La règle Moshi `@com.squareup.moshi.Json <fields>` ne les couvre pas non plus — les trois
# annotations du fichier sont `@param:Json`, des annotations de PARAMÈTRE, pas de champ.
#
# R8 renomme donc ces propriétés et la réflexion cherche des clés JSON « a », « b »… Comme
# `products` et `product` sont les CONTENEURS de la réponse, ils reviennent nuls et toute
# l'intégration Open Food Facts ne rend plus rien — en silence, et UNIQUEMENT dans l'APK signé :
# le debug n'est pas minifié, donc le défaut est invisible en développement et en CI.
-keep class com.example.util.OpenFoodFactsApi { *; }
-keep class com.example.util.OpenFoodFactsApi$** { *; }
