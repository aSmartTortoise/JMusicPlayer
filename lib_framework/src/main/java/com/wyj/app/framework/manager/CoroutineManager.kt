package com.wyj.app.framework.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 *  author : jie wang
 *  date : 2024/11/12 9:32
 *  description :
 */
class CoroutineManager private constructor() {

    companion object {
        fun getInstance(): CoroutineManager = Holder.instance
    }

    private object Holder {
        val instance = CoroutineManager()
    }

    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)


    fun normalScope(): CoroutineScope {
        return coroutineScope
    }
}