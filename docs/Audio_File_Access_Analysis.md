# 项目中读取本地音频文件的代码分析

## 1. MediaStore 查询（核心数据源）

### LocalMusicViewModel.kt — 通过 CursorLoader 查询 MediaStore
- 第 27 行：`MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`
- 第 28-29 行：过滤条件 `IS_MUSIC=1 AND SIZE>0`
- 第 53-61 行：`onCreateLoader()` 创建 CursorLoader
- 第 64-76 行：`onLoadFinished()` 将 Cursor 交给 SongRepository 处理

### SongRepository.kt — 将 Cursor 转为 Song 对象
- 第 20-54 行：`getLocalSongs()` — RxJava 方式
- 第 56-90 行：`getLocalSongsFlow()` — Flow 方式
- 第 92-117 行：`cursorToMusic()` — 读取 `DATA`、`TITLE`、`ARTIST`、`ALBUM`、`DURATION`、`SIZE` 等字段

## 2. 文件路径直接访问

### Player.kt — 播放音频
- 第 66 行：`setDataSource(song?.path)` — 通过文件路径加载音频

### PlayMusicActivity.kt — 独立播放器页面
- 第 222 行：`setDataSource(musicViewModel?.songs?.value!![playingIndex].path)`

## 3. 文件元数据读取

### FileUtils.kt — `fileToMusic()` 方法（第 12-46 行）
- 使用 `MediaMetadataRetriever.setDataSource(file.absolutePath)` 提取时长、标题、歌手、专辑

### AlbumUtils.kt — `parseAlbum()` 方法（第 12-27 行）
- 使用 `MediaMetadataRetriever` 提取内嵌专辑封面

## 4. 权限请求入口

### MainActivity.kt
- 第 70-77 行：请求 `WRITE_EXTERNAL_STORAGE`

### PlayMusicActivity.kt
- 第 133-139 行：请求 `WRITE_EXTERNAL_STORAGE`

## 总结

音频文件访问的完整链路：

```
权限请求 (MainActivity / PlayMusicActivity)
  → MediaStore 查询 (LocalMusicViewModel → SongRepository)
  → Song.path 存储文件路径
  → MediaPlayer.setDataSource(path) 播放 (Player / PlayMusicActivity)
  → MediaMetadataRetriever 提取元数据/封面 (FileUtils / AlbumUtils)
```

所有这些读取操作在 Android 13 上都依赖 `READ_MEDIA_AUDIO` 权限（替代原来的 `READ_EXTERNAL_STORAGE`）。