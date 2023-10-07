package com.jie.databinding.extention

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

inline fun <VB : ViewBinding> Any.getViewBinding(inflater: LayoutInflater, position: Int = 0): VB {
    val vbClass =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()
    val inflate = vbClass[position].getDeclaredMethod("inflate", LayoutInflater::class.java)
    return inflate.invoke(null, inflater) as VB
}

inline fun <VB : ViewDataBinding> Any.getViewBinding(
    inflater: LayoutInflater, container: ViewGroup?,
    attachToRoot: Boolean = false, position: Int = 0
): VB {
    val vbClass =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()
    val inflate = vbClass[position].getDeclaredMethod(
        "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
    )
    return inflate.invoke(null, inflater, container, attachToRoot) as VB
}