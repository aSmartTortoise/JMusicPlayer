package com.wyj.app.framework.itemdecoration

import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 *  author : jie wang
 *  date : 2024/3/21 10:43
 *  description : 单itemType，线性布局对应的item decoration。
 */
class LinearItemDecoration(private var config: Config?) : RecyclerView.ItemDecoration() {

    companion object {
        const val TAG = "LinearItemDecoration"
    }
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (config == null) return
        val adapterPosition = parent.getChildAdapterPosition(view)
        val count = parent.adapter!!.itemCount

        Log.d(TAG, "getItemOffsets: adapterPosition:$adapterPosition, count:$count")
        outRect.let {

            it.left = config!!.itemPaddingLeft
            it.right = config!!.itemPaddingRight
            if (adapterPosition == 0) it.top = config!!.firstItemPaddingTop
            else it.top = config!!.itemPaddingTop
            if (adapterPosition != count - 1) it.bottom = config!!.itemPaddingBottom
            else it.bottom = config!!.lastItemPaddingBottom
        }
    }

    class Builder {
        private val config = Config()

        fun setItemPaddingLeft(paddingLeft: Int) = apply {
            config.itemPaddingLeft = paddingLeft
        }

        fun setItemPaddingTop(paddingTop: Int) = apply {
            config.itemPaddingTop = paddingTop
        }

        fun setItemPaddingRight(paddingRight: Int) = apply {
            config.itemPaddingRight = paddingRight
        }

        fun setItemPaddingBottom(paddingBottom: Int) = apply {
            config.itemPaddingBottom = paddingBottom
        }

        fun setFirstItemPaddingTop(firstItemPaddingTop: Int) = apply {
            config.firstItemPaddingTop = firstItemPaddingTop
        }

        fun setLastItemPaddingBottom(lastItemPaddingBottom: Int) = apply {
            config.lastItemPaddingBottom = lastItemPaddingBottom
        }

        fun create() = LinearItemDecoration(config)
    }

    data class Config(
        var itemPaddingLeft: Int = 0,
        var itemPaddingTop: Int = 0,
        var itemPaddingRight: Int = 0,
        var itemPaddingBottom: Int = 0,
        var firstItemPaddingTop: Int = 0,
        var lastItemPaddingBottom: Int = 0
    )

}