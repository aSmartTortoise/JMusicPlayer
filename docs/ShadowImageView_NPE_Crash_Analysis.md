# ShadowImageView NPE Crash 分析报告

## 1. 崩溃现象

### 复现步骤
1. 在播放详情界面（MusicPlayerActivity）操作播放控制栏，切歌若干次
2. 点返回退到主界面（MainActivity）
3. 点击下方播放控制栏重新拉起播放详情界面
4. 点击下一曲，触发 crash

### 崩溃堆栈
```
NullPointerException: Attempt to invoke virtual method 'void android.animation.ObjectAnimator.start()'
on a null object reference
    at com.wyj.voice.ui.view.ShadowImageView.resumeRotateAnimation(ShadowImageView.java:120)
```

`ShadowImageView.resumeRotateAnimation()` 调用时 `mRotateAnimator` 为 null。

## 2. 分析过程

### 第一阶段：排除直接原因

ShadowImageView 的代码在本次重构前从未改动，且之前不存在此崩溃。`mRotateAnimator` 被置 null 的唯一路径是 `onDetachedFromWindow()`，说明问题在于 **旧的已销毁的 Activity 实例上的回调被错误触发**。

### 第二阶段：添加诊断日志定位

在 `Player.kt` 的 `registerCallback`/`unregisterCallback` 和 `MusicPlayerViewModel.kt` 的对应方法中添加诊断日志，记录 callback 对象的 identity hash 和 player 是否为 null。

### 第三阶段：日志分析，锁定根因

关键日志序列：

```
// 步骤1：退出 MusicPlayerActivity 时，ViewModel 的 onCleared 先执行
unbindPlaybackService: wyj isServiceBind:true              ← onCleared() 执行，unbind 并置 player=null

// 步骤2：随后 onDestroy 执行，尝试反注册回调
unregisterCallback: player=false, callback=MusicPlayerActivity@9a71c2   ← player 已为 null，反注册无效！

// 步骤3：重新打开 MusicPlayerActivity，注册新回调
registerCallback: MusicPlayerActivity@c722640, callbacks size=4         ← 旧回调 @9a71c2 仍残留在列表中！
```

**问题链条清晰浮现：** 旧 Activity 的回调没有被成功移除，Player 单例在通知所有回调时，触发了已销毁 Activity 上的 `onPlayStatusChanged` → `resumeRotateAnimation`，而该 Activity 的 ShadowImageView 已经执行过 `onDetachedFromWindow`（`mRotateAnimator = null`），最终 NPE。

### 第四阶段：确认 singleTask 的特殊生命周期

MusicPlayerActivity 在 AndroidManifest 中声明为 `launchMode="singleTask"`。对于 singleTask 的 Activity：

- 当 Activity 被系统销毁重建时，**`ViewModel.onCleared()` 在 `Activity.onDestroy()` 之前执行**
- 正常情况下（非 singleTask），`onDestroy()` 先执行，Activity 有机会在其中调用 `unregisterCallback`
- singleTask 模式打破了这个顺序，导致 ViewModel 中的 `player` 在 Activity 还未来得及反注册时就已经被置 null

### 时序对比

**正常 Activity 生命周期：**
```
onDestroy() → unregisterCallback(player 有效，成功移除) → onCleared() → player=null
```

**singleTask Activity 生命周期：**
```
onCleared() → player=null → onDestroy() → unregisterCallback(player=null，无效！回调残留)
```

## 3. 修复方案

### 核心思路

将 callback 的反注册责任从 Activity 的 `onDestroy` 转移到 ViewModel 的 `onCleared`，确保在 player 被置 null 之前完成反注册。

### 具体改动

**文件：** `MusicPlayerViewModel.kt`

1. 新增 `registeredCallback` 字段，跟踪当前已注册的回调引用：

```kotlin
private var registeredCallback: IPlayback.Callback? = null
```

2. `registerCallback` / `unregisterCallback` 同步更新该字段：

```kotlin
fun registerCallback(callback: IPlayback.Callback) {
    registeredCallback = callback
    player?.registerCallback(callback)
}

fun unregisterCallback(callback: IPlayback.Callback) {
    registeredCallback = null
    player?.unregisterCallback(callback)
}
```

3. `onCleared()` 中在 unbind 和置 null **之前**主动反注册：

```kotlin
override fun onCleared() {
    super.onCleared()
    registeredCallback?.let {
        player?.unregisterCallback(it)
        registeredCallback = null
    }
    unbindPlaybackService()
    player = null
}
```

### 修复后时序

```
onCleared() → unregisterCallback(player 有效，成功移除) → unbind → player=null → onDestroy()（无残留回调）
```

## 4. 经验总结

1. **singleTask 的 ViewModel 生命周期陷阱：** `onCleared()` 可能在 `onDestroy()` 之前执行，不能假设 Activity 的清理代码（onDestroy）一定先于 ViewModel 的清理代码（onCleared）
2. **单例 + 回调列表模式的风险：** Player 作为单例持有所有 Activity 的回调引用，如果某个 Activity 未能正确反注册，其回调会一直残留，导致对已销毁对象的方法调用
3. **资源清理的归属原则：** 谁注册的资源，应由同一层级负责清理。回调通过 ViewModel 注册，就应由 ViewModel 负责在自身清理时反注册，而不是依赖 Activity 的生命周期
