package com.example.apptest.core.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ¿Por qué se necesitan TypeConverters?
 * Room solo puede almacenar tipos primitivos y String directamente en SQLite.
 * Tipos como List<String>, List<Int> o clases personalizadas no son soportados nativamente.
 * Los TypeConverters los transforman a/desde tipos que Room sí puede guardar.
 *
 * @TypeConverter -> anota las funciones de conversión.
 *      Room detecta automáticamente estas funciones cuando la clase está registrada
 *      en @Database con @TypeConverters(Converters::class).
 *
 * Patrón: Serialización JSON con Gson.
 *      - Lista -> String JSON al guardar en SQLite
 *      - String JSON -> Lista al leer de SQLite
 *
 * Ejemplo en base de datos:
 *      genres: List<String> = ["Action", "Sci-Fi"]
 *      se guarda como: genres TEXT = '["Action", "Sci-Fi"]'
 */

class Converters {
    private val gson = Gson()

    // List<String>
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // List<Int>
    // Usado para genre_ids en la entidad de películas populares

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return gson.toJson(list ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}