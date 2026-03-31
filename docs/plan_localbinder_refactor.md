# Plan：LocalBinder 代理方法重构

## 目标

将 `LocalBinder` 从直接暴露 `PlaybackService` 实例改为自身定义代理方法，遵循最小暴露原则，客户端（ViewModel）无法接触到 Service 的生命周期方法。

## 最小暴露原则

**最小暴露原则**（Principle of Least Exposure / Least Privilege）：每个模块只暴露外部完成工作所必需的最少接口，隐藏一切内部实现细节。

以本次重构为例：

- **重构前**：`LocalBinder` 直接返回 `PlaybackService` 实例，ViewModel 能访问 `onCreate`、`onStartCommand`、`stopService`、`releasePlayer` 等所有 public 方法，远超实际需要的范围。
- **重构后**：`LocalBinder` 仅定义 14 个代理方法，ViewModel 只能调用明确的播放控制 API，无法触及 Service 的生命周期和内部实现。

**好处：**

1. **防误用**：调用方不可能意外调用不该调用的方法
2. **降耦合**：Service 内部重构（改方法名、改实现）不影响外部，只要代理方法签名不变
3. **明确契约**：看 LocalBinder 的方法列表就知道对外提供了哪些能力，不需要去猜

类比：酒店给你一张房卡只能开你的房间，而不是给你一把万能钥匙。能用的权限越少，出问题的可能性就越小。

## 现状分析

### 当前调用链

```
MusicPlayerViewModel
  └─ player: PlaybackService?    ← 直接持有 Service 引用
       ├─ play() / play(list, index)
       ├─ pause()
       ├─ playLast() / playNext()
       ├─ isPlaying()
       ├─ getProgress()
       ├─ seekTo(progress)
       ├─ getPlayingSong()
       ├─ getPlayList()
       ├─ setPlayMode(playMode)
       ├─ registerCallback(callback)
       ├─ unregisterCallback(callback)
       └─ registerPlaybackCallback()    ← PlaybackService 独有，不在 IPlayback 接口中
```

### 问题

1. ViewModel 持有 `PlaybackService` 具体类型，能访问 `onCreate`、`onStartCommand`、`stopService` 等不应被外部调用的方法
2. `registerPlaybackCallback()` 是 PlaybackService 的特有方法，不属于 IPlayback 接口，需要在 LocalBinder 中一并代理

## 重构步骤

### Step 1：改造 LocalBinder

**文件：** `PlaybackService.kt`

将 `LocalBinder` 从暴露 Service 实例改为定义代理方法，仅暴露 ViewModel 实际需要的 API：

```kotlin
inner class LocalBinder : Binder() {
    fun play(): Boolean = this@PlaybackService.play()
    fun play(list: PlayList, startIndex: Int): Boolean = this@PlaybackService.play(list, startIndex)
    fun pause(): Boolean = this@PlaybackService.pause()
    fun playLast(): Boolean = this@PlaybackService.playLast()
    fun playNext(): Boolean = this@PlaybackService.playNext()
    fun isPlaying(): Boolean = this@PlaybackService.isPlaying()
    fun getProgress(): Int = this@PlaybackService.getProgress()
    fun seekTo(progress: Int): Boolean = this@PlaybackService.seekTo(progress)
    fun getPlayingSong(): Song? = this@PlaybackService.getPlayingSong()
    fun getPlayList(): PlayList? = this@PlaybackService.getPlayList()
    fun setPlayMode(playMode: PlayMode) = this@PlaybackService.setPlayMode(playMode)
    fun registerCallback(callback: IPlayback.Callback) = this@PlaybackService.registerCallback(callback)
    fun unregisterCallback(callback: IPlayback.Callback) = this@PlaybackService.unregisterCallback(callback)
    fun registerPlaybackCallback() = this@PlaybackService.registerPlaybackCallback()
}
```

**要点：**
- 不暴露 `releasePlayer()`、`removeCallbacks()`、`setPlayList()` 等仅 Service 内部使用的方法
- `registerPlaybackCallback()` 是 ViewModel 需要的，必须代理

### Step 2：修改 MusicPlayerViewModel

**文件：** `MusicPlayerViewModel.kt`

将 `player` 的类型从 `PlaybackService?` 改为 `PlaybackService.LocalBinder?`：

```kotlin
// Before
private var player: PlaybackService? = null

// After
private var player: PlaybackService.LocalBinder? = null
```

同步修改 `ServiceConnection` 中的赋值：

```kotlin
// Before
player = (service as PlaybackService.LocalBinder).service

// After
player = service as PlaybackService.LocalBinder
```

ViewModel 中其他所有对 `player?.xxx()` 的调用无需改动，因为 LocalBinder 已定义了同名代理方法。

### Step 3：移除 PlaybackService 中原有的 service 属性

`LocalBinder` 中删除 `val service: PlaybackService` 属性，彻底切断外部获取 Service 实例的路径。

## 涉及文件

| 文件 | 改动 |
|------|------|
| `PlaybackService.kt` | LocalBinder 添加代理方法，删除 service 属性 |
| `MusicPlayerViewModel.kt` | player 类型改为 LocalBinder，ServiceConnection 赋值调整 |

## 风险评估

- **改动范围小**：仅 2 个文件，ViewModel 中对 player 的方法调用签名不变
- **编译即可验证**：如果遗漏了某个代理方法，编译期就会报错
- **不影响 IPlayback 接口**：接口本身不需要任何改动
