# Mundus ProGuard rules.
# kotlinx.serialization keeps generated serializers via annotations; keep them.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mundus.**$$serializer { *; }
-keepclassmembers class com.mundus.** {
    *** Companion;
}
