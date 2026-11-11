# R8 / ProGuard rules for SaveBox release builds.
#
# Most libraries (Room, Compose, Navigation, WorkManager, Glance, DataStore, kotlinx.serialization)
# ship their own rules automatically. The rules below are extra safety for OUR classes.

# Keep line numbers in crash stack traces (useful when a user reports a crash),
# while hiding the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: keep the generated serializers of our @Serializable classes
# (navigation routes and the backup file format).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.savebox.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.savebox.app.**$$serializer { *; }

# WorkManager creates ReminderWorker by its class name.
-keep class com.savebox.app.notifications.ReminderWorker { <init>(...); }
