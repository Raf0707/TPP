package raf.console.tg_postman_premium.data.local.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<String> = value.split(",").filter { it.isNotBlank() }
}
