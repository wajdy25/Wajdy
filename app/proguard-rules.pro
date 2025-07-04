# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all classes in our package
-keep class com.animecharacter.** { *; }

# Keep OpenAI API related classes
-keep class com.android.volley.** { *; }
-keep class com.google.gson.** { *; }

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Speech Recognition classes
-keep class android.speech.** { *; }

# Keep Text-to-Speech classes
-keep class android.speech.tts.** { *; }

# Keep Material Design classes
-keep class com.google.android.material.** { *; }

# Keep GIF library classes
-keep class pl.droidsonroids.gif.** { *; }

# Keep CircleImageView classes
-keep class de.hdodenhof.circleimageview.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

