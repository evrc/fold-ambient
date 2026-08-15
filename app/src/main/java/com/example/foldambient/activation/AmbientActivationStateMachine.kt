package com.example.foldambient.activation

const val AMBIENT_ACTIVATION_ENTRY_DELAY_MILLIS = 750L
const val AMBIENT_ACTIVATION_EXIT_DELAY_MILLIS = 600L
const val AMBIENT_ACTIVATION_MANUAL_PAUSE_RESET_DELAY_MILLIS = 750L

class AmbientActivationStateMachine(
  private val timings: AmbientActivationTimings = AmbientActivationTimings(),
  private val logger: (String) -> Unit = {},
) {
  private var rawEligible = false
  var snapshot: AmbientActivationSnapshot = AmbientActivationSnapshot()
    private set

  fun onRawEligibilityChanged(
    eligible: Boolean,
    nowMillis: Long,
  ): AmbientActivationTransition {
    if (rawEligible != eligible) {
      logger("raw eligibility -> $eligible")
    }
    rawEligible = eligible

    return updateSnapshot(
      state =
        when (val state = snapshot.state) {
          AmbientActivationState.Inactive ->
            if (eligible) {
              logger("entry pending")
              AmbientActivationState.PendingEntry(nowMillis + timings.entryDelayMillis)
            } else {
              state
            }
          is AmbientActivationState.PendingEntry ->
            if (eligible) {
              state
            } else {
              logger("entry cancelled")
              AmbientActivationState.Inactive
            }
          is AmbientActivationState.Active ->
            when {
              state.source == AmbientActivationSource.Manual -> state
              eligible -> state
              else -> {
                logger("exit pending")
                AmbientActivationState.PendingExit(nowMillis + timings.exitDelayMillis)
              }
            }
          is AmbientActivationState.PendingExit ->
            if (eligible) {
              logger("exit cancelled")
              AmbientActivationState.Active(AmbientActivationSource.Automatic)
            } else {
              state
            }
          is AmbientActivationState.ManuallyPaused ->
            when {
              eligible -> {
                if (state.resetDeadlineMillis != null) {
                  logger("manual pause reset cancelled")
                }
                AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null)
              }
              state.resetDeadlineMillis == null -> {
                logger("manual pause reset pending")
                AmbientActivationState.ManuallyPaused(
                  resetDeadlineMillis = nowMillis + timings.manualPauseResetDelayMillis,
                )
              }
              else -> state
            }
        },
    )
  }

  fun onTimer(nowMillis: Long): AmbientActivationTransition =
    when (val state = snapshot.state) {
      is AmbientActivationState.PendingEntry ->
        if (nowMillis >= state.deadlineMillis) {
          if (rawEligible) {
            logger("ambient activated")
            updateSnapshot(
              state = AmbientActivationState.Active(AmbientActivationSource.Automatic),
              command = AmbientActivationCommand.ActivateAmbient,
            )
          } else {
            logger("entry cancelled")
            updateSnapshot(AmbientActivationState.Inactive)
          }
        } else {
          AmbientActivationTransition(snapshot = snapshot)
        }
      is AmbientActivationState.PendingExit ->
        if (nowMillis >= state.deadlineMillis) {
          if (rawEligible) {
            logger("exit cancelled")
            updateSnapshot(AmbientActivationState.Active(AmbientActivationSource.Automatic))
          } else {
            logger("ambient deactivated")
            updateSnapshot(
              state = AmbientActivationState.Inactive,
              command = AmbientActivationCommand.DeactivateAmbient,
            )
          }
        } else {
          AmbientActivationTransition(snapshot = snapshot)
        }
      is AmbientActivationState.ManuallyPaused ->
        if (state.resetDeadlineMillis != null && nowMillis >= state.resetDeadlineMillis) {
          if (rawEligible) {
            logger("manual pause reset cancelled")
            updateSnapshot(AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null))
          } else {
            logger("manual pause reset after stable ineligible posture")
            updateSnapshot(AmbientActivationState.Inactive)
          }
        } else {
          AmbientActivationTransition(snapshot = snapshot)
        }
      AmbientActivationState.Inactive,
      is AmbientActivationState.Active,
      -> AmbientActivationTransition(snapshot = snapshot)
    }

  fun onManualEntered(): AmbientActivationTransition {
    if (snapshot.state is AmbientActivationState.PendingEntry) {
      logger("entry cancelled")
    }
    logger("manual ambient entered")
    return updateSnapshot(AmbientActivationState.Active(AmbientActivationSource.Manual))
  }

  fun onManualExited(nowMillis: Long): AmbientActivationTransition {
    if (snapshot.state is AmbientActivationState.PendingEntry) {
      logger("entry cancelled")
    }
    if (snapshot.state is AmbientActivationState.PendingExit) {
      logger("exit cancelled")
    }
    logger("manual pause")
    return updateSnapshot(
      AmbientActivationState.ManuallyPaused(
        resetDeadlineMillis =
          if (rawEligible) {
            null
          } else {
            nowMillis + timings.manualPauseResetDelayMillis
          },
      ),
    )
  }

  fun onLifecycleStopped(): AmbientActivationTransition {
    val nextState =
      when (snapshot.state) {
        AmbientActivationState.Inactive,
        is AmbientActivationState.PendingEntry,
        -> AmbientActivationState.Inactive
        is AmbientActivationState.Active -> snapshot.state
        is AmbientActivationState.PendingExit ->
          AmbientActivationState.Active(AmbientActivationSource.Automatic)
        is AmbientActivationState.ManuallyPaused ->
          AmbientActivationState.ManuallyPaused(resetDeadlineMillis = null)
      }
    rawEligible = false
    if (snapshot.timerDeadlineMillis != null) {
      logger("pending activation timer cancelled for lifecycle stop")
    }
    return updateSnapshot(nextState)
  }

  private fun updateSnapshot(
    state: AmbientActivationState,
    command: AmbientActivationCommand? = null,
  ): AmbientActivationTransition {
    snapshot =
      AmbientActivationSnapshot(
        state = state,
        isRawEligible = rawEligible,
        timerDeadlineMillis = state.timerDeadlineMillis,
      )
    return AmbientActivationTransition(
      snapshot = snapshot,
      command = command,
    )
  }
}

data class AmbientActivationTimings(
  val entryDelayMillis: Long = AMBIENT_ACTIVATION_ENTRY_DELAY_MILLIS,
  val exitDelayMillis: Long = AMBIENT_ACTIVATION_EXIT_DELAY_MILLIS,
  val manualPauseResetDelayMillis: Long = AMBIENT_ACTIVATION_MANUAL_PAUSE_RESET_DELAY_MILLIS,
)

data class AmbientActivationSnapshot(
  val state: AmbientActivationState = AmbientActivationState.Inactive,
  val isRawEligible: Boolean = false,
  val timerDeadlineMillis: Long? = null,
)

data class AmbientActivationTransition(
  val snapshot: AmbientActivationSnapshot,
  val command: AmbientActivationCommand? = null,
)

sealed interface AmbientActivationState {
  data object Inactive : AmbientActivationState

  data class PendingEntry(
    val deadlineMillis: Long,
  ) : AmbientActivationState

  data class Active(
    val source: AmbientActivationSource,
  ) : AmbientActivationState

  data class PendingExit(
    val deadlineMillis: Long,
  ) : AmbientActivationState

  data class ManuallyPaused(
    val resetDeadlineMillis: Long?,
  ) : AmbientActivationState
}

enum class AmbientActivationSource {
  Automatic,
  Manual,
}

enum class AmbientActivationCommand {
  ActivateAmbient,
  DeactivateAmbient,
}

private val AmbientActivationState.timerDeadlineMillis: Long?
  get() =
    when (this) {
      is AmbientActivationState.PendingEntry -> deadlineMillis
      is AmbientActivationState.PendingExit -> deadlineMillis
      is AmbientActivationState.ManuallyPaused -> resetDeadlineMillis
      AmbientActivationState.Inactive,
      is AmbientActivationState.Active,
      -> null
    }
