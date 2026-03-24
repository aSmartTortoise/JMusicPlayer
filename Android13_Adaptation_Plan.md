# JMusicPlayer Android 13 适配方案

## 当前状态

| 项目 | 当前值 |
|------|--------|
| compileSdk / targetSdk | 32 |
| AGP / Kotlin / Gradle | 7.3.1 / 1.7.20 / 7.4 |
| 存储权限 | `READ/WRITE_EXTERNAL_STORAGE` |
| 通知权限 | 无 |
| 前台服务类型 | 未声明 |

---

## Step 1: 升级构建工具链

### 涉及文件

- `build.gradle` (project)
- `app/build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`

### 1.1 升级 compileSdk / targetSdk

| 项目 | 当前版本 | 目标版本 |
|------|---------|---------|
| compileSdk | 32 | 33 |
| targetSdk | 32 | 33 |

> 根据[官方文档](https://developer.android.com/build/releases/about-agp)，compileSdk 33 最低要求 AGP 7.2，当前 AGP 7.3.1 已满足，仅适配 Android 13 不需要升级 AGP。

### 1.2 升级 Kotlin 至 1.8.20（用户要求）

将 Kotlin 从 1.7.20 升级至 1.8.20 会触发以下连锁升级：

根据[官方文档](https://developer.android.com/build/kotlin-support)，**Kotlin 1.8 最低要求 AGP 7.4**，而 AGP 7.4 最低要求 **Gradle 7.5**。因此需要同步升级：

| 项目 | 当前版本 | 目标版本 | 升级原因 |
|------|---------|---------|---------|
| Kotlin | 1.7.20 | 1.8.20 | 用户要求 |
| AGP | 7.3.1 | 7.4.2 | Kotlin 1.8 最低要求 AGP 7.4 |
| Gradle | 7.4 | 7.5.1 | AGP 7.4 要求 Gradle 7.5 |

### 1.3 Kotlin 相关依赖兼容性分析

项目中与 Kotlin 相关的依赖如下：

| 依赖 | 当前版本 | 是否需要升级 | 说明 |
|------|---------|------------|------|
| `kotlin-kapt` 插件 | 跟随 Kotlin 版本 | 自动跟随 | 随 Kotlin 1.8.20 自动升级，与 AGP 7.4 兼容 |
| `core-ktx` | 1.7.0 | 不需要 | 与 Kotlin 1.8.20 兼容 |
| `lifecycle-runtime-ktx` | 2.5.1 | 不需要 | 与 Kotlin 1.8.20 兼容 |
| `activity-ktx` | 1.5.1 | 不需要 | 与 Kotlin 1.8.20 兼容 |
| Glide (`kapt` 注解处理) | 4.15.0 | 不需要 | kapt 注解处理器与 Kotlin 版本无关 |
| RxJava / RxAndroid | 2.2.2 / 2.1.0 | 不需要 | Java 库，不受 Kotlin 版本影响 |

> **结论：** 升级 Kotlin 1.8.20 仅需连锁升级 AGP 和 Gradle，其余依赖均无需变动。

### 改动汇总

**`build.gradle` (project):**
```groovy
plugins {
    id 'com.android.application' version '7.4.2' apply false
    id 'com.android.library' version '7.4.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.8.20' apply false
}
```

**`app/build.gradle`:**
```groovy
compileSdk 33
targetSdk 33
```

**`gradle/wrapper/gradle-wrapper.properties`:**
```
distributionUrl=https\://services.gradle.org/distributions/gradle-7.5.1-bin.zip
```

---

## Step 2: 更新 AndroidManifest.xml 权限与服务声明

### 涉及文件

- `app/src/main/AndroidManifest.xml`

### 2.1 新增权限

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2.2 限制旧存储权限作用范围

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

> API 33+ 上 `READ_EXTERNAL_STORAGE` 已被忽略，必须使用 `READ_MEDIA_AUDIO` 替代。

### 2.3 声明前台服务类型

```xml
<service
    android:name=".player.PlaybackService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" >
```

- 添加 `android:foregroundServiceType="mediaPlayback"`
- `exported` 改为 `false`（该服务仅通过内部 PendingIntent 和 bindService 调用，无需对外暴露）

### 2.4 更新 targetApi

`tools:targetApi` 从 `31` 更新为 `33`。

---

## Step 3: 更新 MainActivity 存储权限请求逻辑

### 涉及文件

- `app/src/main/java/com/wyj/voice/ui/MainActivity.kt`

### 改动内容

`onClick()` 方法中当前仅请求 `WRITE_EXTERNAL_STORAGE`，需按 API 级别区分：

```kotlin
private fun getRequiredStoragePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}
```

在 `onClick()` 和 `onRequestPermissionsResult()` 中使用该方法统一处理权限判断。

---

## Step 4: 添加通知权限运行时请求

### 涉及文件

- `app/src/main/java/com/wyj/voice/ui/MainActivity.kt`
- `app/src/main/java/com/wyj/voice/ui/music/MusicPlayerActivity.kt`

### 改动内容

在 `subscribeService()` 流程中，首次播放前请求 `POST_NOTIFICATIONS`：

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_NOTIFICATION_CODE)
    }
}
```

> 即使用户拒绝通知权限，`startForeground()` 仍可正常运行，服务不受影响，只是通知不可见。

---

## Step 5: 修复已废弃的 stopForeground(boolean)

### 涉及文件

- `app/src/main/java/com/wyj/voice/player/PlaybackService.kt`

### 改动内容

`PlaybackService` 中两处 `stopForeground(true)` 替换为：

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    stopForeground(STOP_FOREGROUND_REMOVE)
} else {
    @Suppress("DEPRECATION")
    stopForeground(true)
}
```

---

## Step 6: 其他改进（可选）

### 6.1 通知 Channel 重要性调整

`PlaybackService.showNotification()` 中通知 Channel 使用 `IMPORTANCE_HIGH`，会导致每次切歌弹出 heads-up 通知。建议改为 `IMPORTANCE_LOW` 或 `IMPORTANCE_DEFAULT`。

### 6.2 MediaStore 访问方式（未来优化）

当前通过 `DATA` 列获取文件路径后直接使用 `File(path)` 访问，在 API 33 下仍可正常工作。但未来版本可能进一步限制，建议逐步迁移为基于 Content URI 的访问方式：

```kotlin
val contentUri = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
```

---

## 实施顺序

```
Step 1 (构建工具链)
  ↓
Step 2 (Manifest 权限与服务声明)
  ↓
Step 3 (存储权限运行时请求)
  ↓
Step 4 (通知权限运行时请求)
  ↓
Step 5 (废弃 API 修复)
  ↓
Step 6 (可选改进)
```

每个 Step 完成后需执行完整验证流程，全部通过后再进入下一个 Step：

1. **编译通过** — `./gradlew assembleDebug` 无 error
2. **安装到测试机** — 在 Android 13 (API 33) 真机或模拟器上安装 APK
3. **运行验证** — 核心功能正常运行，无崩溃、无权限异常

---

## 关键文件清单

| 文件 | 改动内容 |
|------|----------|
| `build.gradle` (project) | AGP 7.4.2、Kotlin 1.8.20 |
| `app/build.gradle` | compileSdk、targetSdk 升级到 33 |
| `gradle-wrapper.properties` | Gradle 7.5.1 |
| `AndroidManifest.xml` | 新增权限、服务类型声明、targetApi |
| `MainActivity.kt` | 存储权限 + 通知权限运行时请求 |
| `MusicPlayerActivity.kt` | 通知权限运行时请求 |
| `PlaybackService.kt` | stopForeground 废弃 API 修复 |

---

## 注意事项

- Kotlin 1.8.20 → AGP 7.4.2 → Gradle 7.5.1 为连锁升级，缺一不可
- 其余 AndroidX / 第三方依赖与 Kotlin 1.8.20 均兼容，无需变动
- 权限变更行为只能在 API 33+ 真机/模拟器上完整验证
- 用户拒绝通知权限后，建议展示引导弹窗说明通知的用途