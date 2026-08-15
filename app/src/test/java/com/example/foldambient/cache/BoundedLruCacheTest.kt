package com.example.foldambient.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoundedLruCacheTest {
  @Test
  fun evictsLeastRecentlyUsedEntryWhenCapacityIsExceeded() {
    val cache = BoundedLruCache<String, Int>(capacity = 2)

    cache["one"] = 1
    cache["two"] = 2
    cache["three"] = 3

    assertNull(cache["one"])
    assertEquals(2, cache["two"])
    assertEquals(3, cache["three"])
    assertEquals(listOf("two", "three"), cache.keys())
  }

  @Test
  fun readingEntryRefreshesItsRecency() {
    val cache = BoundedLruCache<String, Int>(capacity = 2)

    cache["one"] = 1
    cache["two"] = 2
    assertEquals(1, cache["one"])
    cache["three"] = 3

    assertEquals(1, cache["one"])
    assertNull(cache["two"])
    assertEquals(3, cache["three"])
  }
}
