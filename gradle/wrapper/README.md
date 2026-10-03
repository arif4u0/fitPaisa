# Gradle wrapper

The wrapper JAR is intentionally not committed. After opening `android/` in
Android Studio, generate it once:

```
gradle wrapper --gradle-version 8.9
```

CI (`.github/workflows/android.yml`) uses an installed Gradle distribution instead,
so no wrapper is required there.
