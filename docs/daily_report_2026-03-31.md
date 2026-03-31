# 日报 2026-03-31

## 任务列表（按重要性排序）

### 1. PlaybackService 跨进程 AIDL 改造
Song/PlayList 实现 Parcelable，定义 AIDL 接口，PlaybackService 新增 Stub 实现和 RemoteCallbackList，onBind 区分本地/远程绑定，Manifest 配置自定义权限。

**问题：** `@Parcelize` 生成的 CREATOR 不可被其他 Kotlin 类直接引用，改为手动实现 Parcelable。**已修复。**

### 2. LocalBinder 代理方法重构
LocalBinder 改为定义代理方法，不再暴露 PlaybackService 实例，遵循最小暴露原则。

### 3. 代码规范优化
REQ_PER_CODE 重命名为 REQ_STORAGE_PERMISSION_CODE；清理 PlayList 中未使用的 COLUMN_FAVORITE 和 fromFolder。
