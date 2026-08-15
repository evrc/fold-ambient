package com.example.foldambient.activation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientActivationStateMachineTest {
  private val timings =
    AmbientActivationTimings(
      entryDelayMillis = 750L,
      exitDelayMillis = 600L,
      manualPauseResetDelayMillis = 750L,
    )

  @Test
  fun eligibleDoesNotActivateImmediately() {
    val machine = machine()

    val transition = machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    assertNull(transition.command)
    assertEquals(AmbientActivationState.PendingEntry(1_750L), transition.snapshot.state)
  }

  @Test
  fun stableEligibleActivatesAfterEntryDelay() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    val transition = machine.onTimer(nowMillis = 1_750L)

    assertEquals(AmbientActivationCommand.ActivateAmbient, transition.command)
    assertEquals(
      AmbientActivationState.Active(AmbientActivationSource.Automatic),
      transition.snapshot.state,
    )
  }

  @Test
  fun eligibilityDisappearsBeforeEntryDelayCancelsActivation() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    val cancelled = machine.onRawEligibilityChanged(eligible = false, nowMillis = 1_200L)
    val staleTimer = machine.onTimer(nowMillis = 1_750L)

    assertNull(cancelled.command)
    assertEquals(AmbientActivationState.Inactive, cancelled.snapshot.state)
    assertNull(staleTimer.command)
    assertEquals(AmbientActivationState.Inactive, staleTimer.snapshot.state)
  }

  @Test
  fun eligibilityReturningStartsANewEntryDelay() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 1_200L)

    val restarted = machine.onRawEligibilityChanged(eligible = true, nowMillis = 2_000L)

    assertEquals(AmbientActivationState.PendingEntry(2_750L), restarted.snapshot.state)
  }

  @Test
  fun transientIneligibleDoesNotExitImmediately() {
    val machine = activeMachine()

    val transition = machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_000L)

    assertNull(transition.command)
    assertEquals(AmbientActivationState.PendingExit(2_600L), transition.snapshot.state)
  }

  @Test
  fun stableIneligibleExitsAfterExitDelay() {
    val machine = activeMachine()
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_000L)

    val transition = machine.onTimer(nowMillis = 2_600L)

    assertEquals(AmbientActivationCommand.DeactivateAmbient, transition.command)
    assertEquals(AmbientActivationState.Inactive, transition.snapshot.state)
  }

  @Test
  fun eligibilityReturningBeforeExitDelayCancelsExit() {
    val machine = activeMachine()
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_000L)

    val cancelled = machine.onRawEligibilityChanged(eligible = true, nowMillis = 2_300L)
    val staleTimer = machine.onTimer(nowMillis = 2_600L)

    assertNull(cancelled.command)
    assertEquals(
      AmbientActivationState.Active(AmbientActivationSource.Automatic),
      cancelled.snapshot.state,
    )
    assertNull(staleTimer.command)
    assertEquals(
      AmbientActivationState.Active(AmbientActivationSource.Automatic),
      staleTimer.snapshot.state,
    )
  }

  @Test
  fun manualExitFromActiveEntersPauseAndDoesNotReenterWhileStillEligible() {
    val machine = activeMachine()

    val paused = machine.onManualExited(nowMillis = 2_000L)
    val stillEligible = machine.onRawEligibilityChanged(eligible = true, nowMillis = 3_000L)
    val timer = machine.onTimer(nowMillis = 10_000L)

    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), paused.snapshot.state)
    assertNull(stillEligible.command)
    assertNull(timer.command)
    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), timer.snapshot.state)
  }

  @Test
  fun transientIneligibleDoesNotClearManualPause() {
    val machine = activeMachine()
    machine.onManualExited(nowMillis = 2_000L)

    val pendingReset = machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_100L)
    val resetCancelled = machine.onRawEligibilityChanged(eligible = true, nowMillis = 2_300L)
    val staleTimer = machine.onTimer(nowMillis = 2_850L)

    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = 2_850L), pendingReset.snapshot.state)
    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), resetCancelled.snapshot.state)
    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), staleTimer.snapshot.state)
  }

  @Test
  fun stableIneligibleClearsManualPauseAndAllowsFutureActivation() {
    val machine = activeMachine()
    machine.onManualExited(nowMillis = 2_000L)
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_100L)

    val reset = machine.onTimer(nowMillis = 2_850L)
    val newEntry = machine.onRawEligibilityChanged(eligible = true, nowMillis = 3_000L)
    val activated = machine.onTimer(nowMillis = 3_750L)

    assertEquals(AmbientActivationState.Inactive, reset.snapshot.state)
    assertEquals(AmbientActivationState.PendingEntry(3_750L), newEntry.snapshot.state)
    assertEquals(AmbientActivationCommand.ActivateAmbient, activated.command)
  }

  @Test
  fun disablingAutomaticActivationCancelsPendingEntry() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    val transition = machine.onRawEligibilityChanged(eligible = false, nowMillis = 1_100L)

    assertEquals(AmbientActivationState.Inactive, transition.snapshot.state)
    assertNull(transition.command)
  }

  @Test
  fun enablingAutomaticActivationReevaluatesFromCurrentRawState() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 1_000L)

    val transition = machine.onRawEligibilityChanged(eligible = true, nowMillis = 2_000L)

    assertEquals(AmbientActivationState.PendingEntry(2_750L), transition.snapshot.state)
  }

  @Test
  fun settingChangeThatInvalidatesActivePostureUsesExitDelay() {
    val machine = activeMachine()

    val transition = machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_000L)

    assertEquals(AmbientActivationState.PendingExit(2_600L), transition.snapshot.state)
    assertNull(transition.command)
  }

  @Test
  fun manualExitCancelsPendingEntryTimer() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    val paused = machine.onManualExited(nowMillis = 1_200L)
    val staleTimer = machine.onTimer(nowMillis = 1_750L)

    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), paused.snapshot.state)
    assertNull(staleTimer.command)
    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null), staleTimer.snapshot.state)
  }

  @Test
  fun manualExitCancelsPendingExitTimer() {
    val machine = activeMachine()
    machine.onRawEligibilityChanged(eligible = false, nowMillis = 2_000L)

    val paused = machine.onManualExited(nowMillis = 2_100L)
    val staleExitTimer = machine.onTimer(nowMillis = 2_600L)

    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = 2_850L), paused.snapshot.state)
    assertNull(staleExitTimer.command)
    assertEquals(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = 2_850L), staleExitTimer.snapshot.state)
  }

  @Test
  fun lifecycleStopCancelsPendingEntryAndRequiresFreshEvaluation() {
    val machine = machine()
    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)

    val stopped = machine.onLifecycleStopped()
    val staleTimer = machine.onTimer(nowMillis = 1_750L)
    val freshEntry = machine.onRawEligibilityChanged(eligible = true, nowMillis = 2_000L)

    assertEquals(AmbientActivationState.Inactive, stopped.snapshot.state)
    assertNull(staleTimer.command)
    assertEquals(AmbientActivationState.PendingEntry(2_750L), freshEntry.snapshot.state)
  }

  @Test
  fun transitionLoggingIsConciseAndStateBased() {
    val logs = mutableListOf<String>()
    val machine = machine(logs::add)

    machine.onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)
    machine.onTimer(nowMillis = 1_750L)
    machine.onManualExited(nowMillis = 2_000L)

    assertTrue(logs.contains("raw eligibility -> true"))
    assertTrue(logs.contains("entry pending"))
    assertTrue(logs.contains("ambient activated"))
    assertTrue(logs.contains("manual pause"))
  }

  private fun machine(logger: (String) -> Unit = {}): AmbientActivationStateMachine =
    AmbientActivationStateMachine(timings = timings, logger = logger)

  private fun activeMachine(): AmbientActivationStateMachine =
    machine().apply {
      onRawEligibilityChanged(eligible = true, nowMillis = 1_000L)
      onTimer(nowMillis = 1_750L)
    }
}
