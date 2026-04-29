# MapLibre Native
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# MapLibre Compose + Spatial K (GeoJSON serialization)
-keep class org.maplibre.compose.** { *; }
-keep class org.maplibre.spatialk.** { *; }
-keepclassmembers class org.maplibre.spatialk.geojson.** {
    <init>(...);
    *** Companion;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class org.maplibre.spatialk.**$$serializer { *; }
-keepclassmembers class org.maplibre.spatialk.** {
    *** Companion;
}
-keepclasseswithmembers class org.maplibre.spatialk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App serializable classes (API DTOs)
-keep,includedescriptorclasses class is.rosaparks.data.api.**$$serializer { *; }
-keepclassmembers class is.rosaparks.data.api.** {
    *** Companion;
}
-keepclasseswithmembers class is.rosaparks.data.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}
