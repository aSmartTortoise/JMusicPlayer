# Plan：PlaybackService 跨进程 AIDL 改造

## 目标

当前 PlaybackService 仅支持同进程绑定（LocalBinder），无法为其他进程的客户端提供服务。本次改造通过 AIDL 为 PlaybackService 增加跨进程访问能力，同时保留现有的同进程本地绑定方式。

## 现状分析

### 当前架构

```
[同进程 Client]
  MusicPlayerViewModel
    └─ bindService() → LocalBinder（代理方法）→ PlaybackService → Player
```

### 跨进程的障碍

1. **LocalBinder 不支持跨进程**：`Binder` 子类直接调用方法的方式只在同进程有效
2. **数据类不可序列化**：`Song`、`PlayList` 未实现 `Parcelable`，无法跨进程传输
3. **回调机制不支持跨进程**：`IPlayback.Callback` 是普通接口，跨进程回调需要 AIDL 接口 + `RemoteCallbackList`

## 改造步骤

### Step 1：数据类实现 Parcelable

跨进程传输的对象必须实现 `Parcelable`。

**文件：** `Song.kt`
- 为 `Song` data class 添加 `@Parcelize` 注解，实现 `Parcelable`
- 需要在模块的 `build.gradle` 中启用 `kotlin-parcelize` 插件

**文件：** `PlayList.kt`
- 为 `PlayList` 实现 `Parcelable`（因为不是 data class，需要手动实现或改造为 data class 后用 `@Parcelize`）
- `PlayList` 持有 `MutableList<Song>` 和 `PlayMode`，这些也需要可序列化

**文件：** `PlayMode.kt`
- 枚举类型在 AIDL 中通过 `int` 传输，不需要实现 Parcelable
- AIDL 接口中用 `int` 表示 PlayMode，客户端/服务端通过 `ordinal` 和 `values()` 转换

**文件：** `build.gradle`（app 模块）
- 添加 `id 'kotlin-parcelize'` 插件

### Step 2：定义 AIDL 接口

在 `app/src/main/aidl/com/wyj/voice/player/` 目录下创建 AIDL 文件。

**文件：** `Song.aidl`（声明 Parcelable 类型）
```aidl
package com.wyj.voice.model;
parcelable Song;
```

**文件：** `PlayList.aidl`（声明 Parcelable 类型）
```aidl
package com.wyj.voice.player;
parcelable PlayList;
```

**文件：** `IPlaybackCallback.aidl`（跨进程回调接口）
```aidl
package com.wyj.voice.player;
import com.wyj.voice.model.Song;

interface IPlaybackCallback {
    void onSwitchLast(in Song last);
    void onSwitchNext(in Song next);
    void onComplete(in Song next);
    void onPlayStatusChanged(boolean isPlaying);
}
```

**文件：** `IPlaybackService.aidl`（跨进程服务接口）
```aidl
package com.wyj.voice.player;
import com.wyj.voice.model.Song;
import com.wyj.voice.player.PlayList;
import com.wyj.voice.player.IPlaybackCallback;

interface IPlaybackService {
    boolean play();
    boolean playWithList(in PlayList list, int startIndex);
    boolean pause();
    boolean playLast();
    boolean playNext();
    boolean isPlaying();
    int getProgress();
    boolean seekTo(int progress);
    Song getPlayingSong();
    PlayList getPlayList();
    void setPlayMode(int playMode);
    void registerCallback(IPlaybackCallback callback);
    void unregisterCallback(IPlaybackCallback callback);
    void registerPlaybackCallback();
}
```

**注意事项：**
- AIDL 不支持方法重载（同名不同参数），所以 `play()` 和 `play(list, index)` 需要用不同方法名
- `PlayMode` 用 `int` 传输，避免 AIDL 对枚举的限制
- Parcelable 参数必须标记 `in`/`out`/`inout` 方向标签

### Step 3：PlaybackService 实现 AIDL Stub

**文件：** `PlaybackService.kt`

新增 AIDL Stub 实现，使用 `RemoteCallbackList` 管理跨进程回调：

```kotlin
private val remoteCallbackList = RemoteCallbackList<IPlaybackCallback>()

private val aidlBinder = object : IPlaybackService.Stub() {
    override fun play(): Boolean = this@PlaybackService.play()
    override fun playWithList(list: PlayList, startIndex: Int): Boolean =
        this@PlaybackService.play(list, startIndex)
    override fun pause(): Boolean = this@PlaybackService.pause()
    // ... 其他方法代理
    override fun registerCallback(callback: IPlaybackCallback) {
        remoteCallbackList.register(callback)
    }
    override fun unregisterCallback(callback: IPlaybackCallback) {
        remoteCallbackList.unregister(callback)
    }
}
```

**修改 `onBind`**：根据 Intent 区分本地绑定和远程绑定：

```kotlin
override fun onBind(intent: Intent): IBinder {
    return if (intent.action == ACTION_BIND_REMOTE) {
        aidlBinder
    } else {
        LocalBinder()
    }
}
```

**跨进程回调分发**：在 `IPlayback.Callback` 的回调方法中，同时通知 `RemoteCallbackList`：

```kotlin
override fun onPlayStatusChanged(isPlaying: Boolean) {
    showNotification()
    val count = remoteCallbackList.beginBroadcast()
    for (i in 0 until count) {
        remoteCallbackList.getBroadcastItem(i).onPlayStatusChanged(isPlaying)
    }
    remoteCallbackList.finishBroadcast()
}
```

### Step 4：AndroidManifest 配置

**文件：** `AndroidManifest.xml`

为 PlaybackService 添加 `intent-filter`，供远程客户端通过隐式 Intent 绑定：

```xml
<service android:name=".player.PlaybackService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.wyj.voice.action.BIND_PLAYBACK_REMOTE" />
    </intent-filter>
</service>
```

**注意：** `exported="true"` 后需评估安全性，考虑添加自定义权限控制访问。

### Step 5（可选）：添加自定义权限

防止任意第三方应用绑定：

```xml
<!-- 声明权限 -->
<permission
    android:name="com.wyj.voice.permission.BIND_PLAYBACK"
    android:protectionLevel="signature" />

<!-- Service 使用权限 -->
<service android:name=".player.PlaybackService"
    android:permission="com.wyj.voice.permission.BIND_PLAYBACK"
    android:exported="true">
    ...
</service>
```

`signature` 级别表示只有相同签名的应用才能绑定。

## 涉及文件

| 文件 | 改动 |
|------|------|
| `build.gradle` (app) | 添加 kotlin-parcelize 插件 |
| `Song.kt` | 添加 `@Parcelize` 和 `Parcelable` |
| `PlayList.kt` | 实现 `Parcelable` |
| `aidl/.../Song.aidl` | 新建，声明 Parcelable |
| `aidl/.../PlayList.aidl` | 新建，声明 Parcelable |
| `aidl/.../IPlaybackCallback.aidl` | 新建，跨进程回调接口 |
| `aidl/.../IPlaybackService.aidl` | 新建，跨进程服务接口 |
| `PlaybackService.kt` | 新增 AIDL Stub 实现、RemoteCallbackList、onBind 分发 |
| `AndroidManifest.xml` | Service 添加 exported 和 intent-filter |

## 架构对比

```
改造后：

[同进程 Client]
  MusicPlayerViewModel
    └─ bindService() → LocalBinder（代理方法）→ PlaybackService → Player

[跨进程 Client]
  RemoteApp
    └─ bindService(ACTION_BIND_REMOTE) → IPlaybackService.Stub（AIDL）→ PlaybackService → Player
```

## 风险评估

- **同进程无影响**：LocalBinder 保持不变，现有的同进程绑定逻辑无需修改
- **Parcelable 改造有侵入性**：Song、PlayList 需要实现 Parcelable，但不影响现有功能
- **跨进程回调的线程安全**：AIDL 回调在 Binder 线程池执行，如需更新 UI 需切换到主线程
- **安全性**：exported Service 需要通过自定义权限控制访问，防止恶意绑定
