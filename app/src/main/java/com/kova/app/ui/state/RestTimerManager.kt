package com.kova.app.ui.state

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kova.app.domain.model.AccentPalette
import com.kova.app.service.RestTimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestTimerState(
    val isVisible: Boolean = false,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isExpanded: Boolean = false,
    val remainingSeconds: Int = 90,
    val totalSeconds: Int = 90,
    val exerciseName: String = "",
    val accentColorInt: Int = AccentPalette.VOLT.previewHexDark.toInt()
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

class RestTimerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var dismissJob: Job? = null
    private var endRealtimeMillis: Long = 0L
    private var hapticsEnabled: Boolean = true
    private var currentAccentColorInt: Int = AccentPalette.VOLT.previewHexDark.toInt()

    fun setSoundAndHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    fun setAccentColor(colorInt: Int) {
        currentAccentColorInt = colorInt
        _state.update { it.copy(accentColorInt = colorInt) }
    }

    fun start(seconds: Int, exerciseName: String = "") {
        val safeSeconds = seconds.coerceIn(10, 600)
        dismissJob?.cancel()
        tickerJob?.cancel()

        endRealtimeMillis = SystemClock.elapsedRealtime() + safeSeconds * 1000L
        _state.value = RestTimerState(
            isVisible = true,
            isRunning = true,
            isFinished = false,
            isExpanded = false,
            remainingSeconds = safeSeconds,
            totalSeconds = safeSeconds,
            exerciseName = exerciseName,
            accentColorInt = currentAccentColorInt
        )
        RestTimerForegroundService.start(context)
        launchTicker()
    }

    fun adjust(deltaSeconds: Int) {
        val current = _state.value
        if (!current.isVisible) return

        dismissJob?.cancel()
        val nextRemaining = (current.remainingSeconds + deltaSeconds).coerceIn(5, 600)
        val nextTotal = maxOf(current.totalSeconds, nextRemaining)

        if (current.isRunning) {
            endRealtimeMillis = SystemClock.elapsedRealtime() + nextRemaining * 1000L
        }
        _state.update {
            it.copy(
                remainingSeconds = nextRemaining,
                totalSeconds = nextTotal,
                isFinished = false
            )
        }
        if (!current.isRunning && current.isFinished) {
            resume()
        }
    }

    fun selectPreset(seconds: Int) {
        start(seconds, _state.value.exerciseName)
    }

    fun togglePause() {
        val current = _state.value
        if (!current.isVisible || current.isFinished) return
        if (current.isRunning) {
            tickerJob?.cancel()
            _state.update { it.copy(isRunning = false) }
        } else {
            resume()
        }
    }

    private fun resume() {
        val remaining = _state.value.remainingSeconds
        if (remaining <= 0) return
        tickerJob?.cancel()
        endRealtimeMillis = SystemClock.elapsedRealtime() + remaining * 1000L
        _state.update { it.copy(isRunning = true, isFinished = false) }
        launchTicker()
    }

    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun dismiss() {
        tickerJob?.cancel()
        dismissJob?.cancel()
        _state.value = RestTimerState(accentColorInt = currentAccentColorInt)
    }

    private fun launchTicker() {
        tickerJob = scope.launch {
            while (true) {
                val leftMillis = endRealtimeMillis - SystemClock.elapsedRealtime()
                val leftSec = ((leftMillis + 999L) / 1000L).toInt().coerceAtLeast(0)
                if (leftSec <= 0) {
                    _state.update {
                        it.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            isFinished = true,
                            isExpanded = false
                        )
                    }
                    playFinishedVibration()
                    scheduleAutoDismiss()
                    break
                } else {
                    _state.update { it.copy(remainingSeconds = leftSec) }
                }
                delay(200L)
            }
        }
    }

    private fun scheduleAutoDismiss() {
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(4500L)
            if (_state.value.isFinished) {
                dismiss()
            }
        }
    }

    private fun playFinishedVibration() {
        if (!hapticsEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            val timings = longArrayOf(0, 180, 90, 180, 90, 260)
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, intArrayOf(0, 220, 0, 220, 0, 255), -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttrs)
            }
        }
    }
}
