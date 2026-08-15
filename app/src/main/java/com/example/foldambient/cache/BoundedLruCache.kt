package com.example.foldambient.cache

import java.util.LinkedHashMap

internal class BoundedLruCache<K, V>(
  private val capacity: Int,
) {
  private val values =
    object : LinkedHashMap<K, V>(capacity, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > capacity
    }

  init {
    require(capacity > 0) { "capacity must be positive" }
  }

  @Synchronized
  operator fun get(key: K): V? = values[key]

  @Synchronized
  operator fun set(key: K, value: V) {
    values[key] = value
  }

  @Synchronized
  fun keys(): List<K> = values.keys.toList()
}
