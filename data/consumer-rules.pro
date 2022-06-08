-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

-keep class android.database.sqlite.** { *; }
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.** { *; }
