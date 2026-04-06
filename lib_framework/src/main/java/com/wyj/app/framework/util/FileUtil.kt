package com.wyj.app.framework.util

import android.content.Context
import com.blankj.utilcode.util.LogUtils
import java.io.File
import java.io.FileOutputStream

/**
 *  author : jie wang
 *  date : 2024/8/8 14:47
 *  description :
 */
object FileUtil {

    fun writeToFile(context: Context, fileName: String, content: String) {
        // 获取应用的私有文件目录
        val fileDir = context.filesDir

        // 创建文件对象
        val file = File(fileDir, fileName)
        if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                LogUtils.w("delete error:$e")
            }
        }
        try {
            // 使用FileOutputStream写入文件
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }

            LogUtils.d("writeToFile, ok，${file.absolutePath}")
        } catch (e: Exception) {
            LogUtils.w("writeToFile, fail，${e.message}")
        }
    }
}