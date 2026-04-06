package com.wyj.app.framework.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 *  author : jie wang
 *  date : 2024/4/12 14:30
 *  description :
 */

fun normalScope(): CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)