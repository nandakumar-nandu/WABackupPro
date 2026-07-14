# Proguard rules for WABackupPro
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\AdminStar\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and share first.

# Keep Google Play services / Google API Client classes
-keep class com.google.api.** { *; }
-keep class com.google.cloud.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
