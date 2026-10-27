# MegLive / FaceID Pro rules
-keep class com.megvii.** { *; }
-keep class com.meglive.** { *; }
-keep class com.megvii.lv5.** { *; }

# ezSign eKYC SDK
-keep class io.github.fvory.ekyc.** { *; }
-keepclassmembers class io.github.fvory.ekyc.model.** { *; }
