package kittoku.osc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Astra VPN addition: process-wide tunnel status published by the vendored SSTP core. */
sealed class AstraStatus {
    object Idle : AstraStatus()
    object Connecting : AstraStatus()
    object Connected : AstraStatus()
    data class Failed(val reason: String) : AstraStatus()
}

object AstraStatusHolder {
    private val _state = MutableStateFlow<AstraStatus>(AstraStatus.Idle)
    val state: StateFlow<AstraStatus> = _state

    internal fun set(value: AstraStatus) {
        _state.value = value
    }
}
