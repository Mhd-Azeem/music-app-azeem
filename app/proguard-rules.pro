# Add project specific ProGuard rules here.

# JSON models (Moshi-parsed) and Room entities - kept by name/fields as a safety net on top of
# Moshi/Room's own bundled consumer rules, since every model here is @JsonClass(generateAdapter =
# true)/@Entity codegen-backed rather than reflection-based, so this is mostly defensive.
-keep class com.wavelength.music.data.remote.jiosaavn.** { *; }
-keep class com.wavelength.music.data.remote.lrclib.** { *; }
-keep class com.wavelength.music.data.backup.** { *; }
-keep class com.wavelength.music.data.local.** { *; }

# --- Moshi (see https://github.com/square/moshi#proguard-r8) ---
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class *
-keep class <1>$JsonAdapter {
    <init>(...);
    <fields>;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# --- Retrofit (see https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro) ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep interface com.wavelength.music.data.remote.jiosaavn.JioSaavnApiService { *; }
-keep interface com.wavelength.music.data.remote.lrclib.LrcLibApiService { *; }

# --- OkHttp (see https://square.github.io/okhttp/features/r8_proguard/) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
