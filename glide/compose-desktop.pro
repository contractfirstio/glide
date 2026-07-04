# Glide release packaging — extends Compose Desktop default rules.

# kotlinx.serialization: keep model classes referenced by generated $$serializer types.
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers class ** {
    *** Companion;
}

# PDFBox optional encryption (BouncyCastle) — not bundled, not used for standard invoices.
-dontwarn org.bouncycastle.**
-dontwarn org.apache.pdfbox.pdmodel.encryption.**
-dontwarn org.apache.pdfbox.io.IOUtils

# Ktor serializable enums used by HTTP client models.
-keep enum io.ktor.** { *; }

# Apache Commons Logging optional backends.
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.avalon.**
-dontwarn javax.servlet.**

# Ktor / networking optional references.
-dontwarn io.ktor.utils.io.jvm.javaio.**
