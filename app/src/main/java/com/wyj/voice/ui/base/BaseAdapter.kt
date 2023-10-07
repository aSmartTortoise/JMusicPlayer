package com.jie.databinding.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.jie.databinding.extention.getViewBinding
import java.util.*

abstract class BaseAdapter<T, VB : ViewDataBinding> :
    RecyclerView.Adapter<BaseAdapter.BindViewHolder<VB>>() {
    private var data: List<T> = mutableListOf()
    fun setData(data: List<T>?) {
        data?.let {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return this@BaseAdapter.data.size
                }

                override fun getNewListSize(): Int {
                    return it.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldData: T = this@BaseAdapter.data[oldItemPosition]
                    val newData: T = data[newItemPosition]
                    return this@BaseAdapter.areItemsTheSame(oldData, newData)
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldData: T = this@BaseAdapter.data[oldItemPosition]
                    val newData: T = data[newItemPosition]
                    return this@BaseAdapter.areItemContentsTheSame(
                        oldData,
                        newData,
                        oldItemPosition,
                        newItemPosition
                    )
                }
            })
            this.data = data
            result.dispatchUpdatesTo(this)
        } ?: let {// Kotlin 三元表达式写法
            this.data = mutableListOf()
            notifyItemRangeChanged(0, this.data.size)
        }
    }

    fun addData(data: List<T>?, position: Int? = null) {
        if (!data.isNullOrEmpty()) {
            with(LinkedList(this.data)) {
                position?.let {
                    val startPosition = when {
                        it < 0 -> 0
                        it > size -> size
                        else -> it
                    }
                    addAll(startPosition, data)
                } ?: addAll(data)//Kotlin三元表达式写法
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

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindViewHolder<VB> {
        return with(getViewBinding<VB>(LayoutInflater.from(parent.context), parent, position = 1)) {
            setListener()
            BindViewHolder(this)
        }
    }

    open fun VB.setListener() {}

    override fun onBindViewHolder(holder: BindViewHolder<VB>, position: Int) {
        with(holder.dataBinding) {
            onBindViewHolder(getItem(position), position)
            executePendingBindings()
        }
    }

    abstract fun VB.onBindViewHolder(bean: T, position: Int)

    class BindViewHolder<M : ViewDataBinding>(var dataBinding: M) :
        RecyclerView.ViewHolder(dataBinding.root)
}