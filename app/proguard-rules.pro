# kotlinx.serialization models (library ships its own rules; these keep our DTOs and routes intact).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.vknxworks.navictrl.**$$serializer { *; }
-keepclassmembers class com.vknxworks.navictrl.** {
    *** Companion;
}
-keepclasseswithmembers class com.vknxworks.navictrl.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
