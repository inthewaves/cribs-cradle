-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
