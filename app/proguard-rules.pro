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

-keep class com.highcapable.** { *; }

-keep class dev.ujhhgtg.pandorasbox.BuildConfig { *; }
-keep class dev.ujhhgtg.pandorasbox.BrowserHistory { *; }
-keep class dev.ujhhgtg.pandorasbox.BrowserHistoryOrBuilder { *; }
-keep class dev.ujhhgtg.pandorasbox.History { *; }
-keep class dev.ujhhgtg.pandorasbox.HistoryEntry { *; }
-keep class dev.ujhhgtg.pandorasbox.HistoryEntryOrBuilder { *; }
-keep class dev.ujhhgtg.pandorasbox.xposed.** { *; }

-keepnames class dev.ujhhgtg.pandorasbox.services.* { *; }

-classobfuscationdictionary proguard-dict.txt
-obfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt
