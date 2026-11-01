# kotlinx.serialization: сохраняем сгенерированные сериализаторы моделей.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.example.pulseworkout.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.pulseworkout.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.pulseworkout.data.**$$serializer { *; }

# Модели ответа GitHub для проверки обновлений.
-keepclassmembers class com.example.pulseworkout.update.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.pulseworkout.update.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.pulseworkout.update.**$$serializer { *; }
