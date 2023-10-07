package com.jie.databinding.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class BaseMultiTypeAdapter<T> :
    RecyclerView.Adapter<BaseMultiTypeAdapter.MultiTypeViewHolder>() {
    private var data: List<T> = mutableListOf()

    fun setData(data: List<T>?) {
        data?.let {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return this@BaseMultiTypeAdapter.data.size
                }

                override fun getNewListSize(): Int {
                    return it.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = this@BaseMultiTypeAdapter.data.get(oldItemPosition)
                    val newItem = it.get(newItemPosition)
                    return this@BaseMultiTypeAdapter.areItemsTheSame(oldItem, newItem)
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = this@BaseMultiTypeAdapter.data.get(oldItemPosition)
                    val newItem = it.get(newItemPosition)
                    return this@BaseMultiTypeAdapter.areItemContentsTheSame(
                        oldItem,
                        newItem,
                        oldItemPosition,
                        newItemPosition
                    )
                }

            })
            this.data = data
            result.dispatchUpdatesTo(this)
        } ?: let {
            this.data = mutableListOf()
            notifyItemRangeChanged(0, this.data.size)
        }
    }

    fun addData(data: List<T>, position: Int? = null) {
        if (!data.isNullOrEmpty()) {
            with(LinkedList(this.data)) {
                position?.let {
                    val startPosition = when {
                        it < 0 -> 0
                        it > size -> size
                        else -> it
                    }
                    addAll(startPosition, data)
                } ?: addAll(data)
                setData(this)
            }
        }
    }

    protected open fun areItemContentsTheSame(
        oldItem: T,
        newItem: T,
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return oldItem == newItem
    }

    protected open fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }

    fun getData(): List<T> {
        return data
    }

    fun getItem(position: Int): T {
        return data[position]
    }

    fun getActualPosition(data: T): Int {
        return this.data.indexOf(data)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiTypeViewHolder {
        return MultiTypeViewHolder(onCreateMultiViewHolder(parent, viewType))
    }

    override fun onBindViewHolder(holder: MultiTypeViewHolder, position: Int) {
        holder.onBindViewHolder(holder, getItem(position), position)
        holder.dataBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    abstract fun onCreateMultiViewHolder(parent: ViewGroup, viewType: Int): ViewDataBinding

    abstract fun MultiTypeViewHolder.onBindViewHolder(
        holder: MultiTypeViewHolder,
        item: T,
        position: Int
    )

    protected fun <VB : ViewDataBinding> loadLayout(vbClass: Class<VB>, parent: ViewGroup): VB {
        val inflate = vbClass.getDeclaredMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return inflate.invoke(null, LayoutInflater.from(parent.context), parent, false) as VB
    }

    class MultiTypeViewHolder(var dataBinding: ViewDataBinding) :
        RecyclerView.ViewHolder(dataBinding.root) {

    }
}