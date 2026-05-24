package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SoldItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SoldItem::class.java)
    private val adapter = moshi.adapter<List<SoldItem>>(listType)

    @TypeConverter
    fun fromSoldItemList(value: List<SoldItem>?): String? {
        return value?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toSoldItemList(value: String?): List<SoldItem>? {
        return value?.let { adapter.fromJson(it) }
    }
}
