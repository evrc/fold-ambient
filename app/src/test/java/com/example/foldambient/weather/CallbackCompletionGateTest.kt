package com.example.foldambient.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallbackCompletionGateTest {
  @Test
  fun completionOnlyWinsOnce() {
    val gate = CallbackCompletionGate()

    assertTrue(gate.tryComplete())
    assertFalse(gate.tryComplete())
  }

  @Test
  fun cancellationPreventsLaterCompletionAndRunsCancelWorkOnce() {
    var cancelCount = 0
    val gate = CallbackCompletionGate { cancelCount += 1 }

    assertTrue(gate.cancel())
    assertFalse(gate.tryComplete())
    assertFalse(gate.cancel())
    assertEquals(1, cancelCount)
  }

  @Test
  fun completionPreventsLaterCancellation() {
    var cancelCount = 0
    val gate = CallbackCompletionGate { cancelCount += 1 }

    assertTrue(gate.tryComplete())
    assertFalse(gate.cancel())
    assertEquals(0, cancelCount)
  }
}
