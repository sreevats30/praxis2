# Keep MediaPipe Tasks classes (used via JNI) if minification is ever enabled.
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
