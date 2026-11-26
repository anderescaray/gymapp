package com.kova.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kova.app.KovaApplication
import com.kova.app.MainActivity
import com.kova.app.R
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.ui.state.RestTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the rest timer running accurately in the background (even when switching apps or locking
 * the screen), shows the floating countdown pill over other apps, posts a silent ongoing countdown
 * notification, and ensures finish vibration is never dropped by Android background restrictions.
 */
class RestTimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var overlay: FloatingRestTimerOverlay? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockExpiresAtRealtime: Long = 0L
    private var lastNotificationSignature: String = ""

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        val app = application as KovaApplication
        overlay = FloatingRestTimerOverlay(
            context = this,
            onAdjust = { delta -> app.restTimerManager.adjust(delta) },
            onDismiss = { app.restTimerManager.dismiss() },
            onOpenApp = { openMainActivity() }
        )

        observerJob = serviceScope.launch {
            combine(
                app.restTimerManager.state,
                app.isAppInForeground
            ) { timerState, inForeground ->
                timerState to inForeground
            }.collect { (state, inForeground) ->
                if (!state.isVisible) {
                    overlay?.hide()
                    releaseWakeLock()
                    stopForegroundAndSelf()
                    return@collect
                }

                // Hold a bounded partial wake lock while ticking so background/locked-screen vibration fires on time
                if (state.isRunning) {
                    acquireWakeLock(state.remainingSeconds)
                } else {
                    releaseWakeLock()
                }

                // Show system floating overlay ONLY when user switched to another app
                if (inForeground) {
                    overlay?.hide()
                } else {
                    overlay?.showOrUpdate(state)
                }

                // Update silent ongoing notification (throttled to second/state changes)
                val sig = "${state.remainingSeconds}|${state.isRunning}|${state.isFinished}|${state.exerciseName}"
                if (sig != lastNotificationSignature) {
                    lastNotificationSignature = sig
                    runCatching {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as KovaApplication
        when (intent?.action) {
            ACTION_ADJUST -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_SECONDS, 0)
                if (delta != 0) app.restTimerManager.adjust(delta)
            }
            ACTION_DISMISS -> {
                app.restTimerManager.dismiss()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
            else -> {
                val current = app.restTimerManager.state.value
                val notification = buildNotification(current)
                startForegroundCompat(notification)
                if (!current.isVisible) {
                    stopForegroundAndSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        overlay?.hide()
        overlay = null
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundAndSelf() {
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun acquireWakeLock(remainingSeconds: Int) {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val wl = wakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kova:rest_timer").also {
                it.setReferenceCounted(false)
                wakeLock = it
            }
            val now = SystemClock.elapsedRealtime()
            val neededUntil = now + (remainingSeconds.coerceIn(5, 600) + 8) * 1000L
            if (!wl.isHeld || neededUntil > wakeLockExpiresAtRealtime + 2_000L) {
                wl.acquire(neededUntil - now)
                wakeLockExpiresAtRealtime = neededUntil
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLockExpiresAtRealtime = 0L
        }
    }

    private fun openMainActivity() {
        val launch = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        runCatching { startActivity(launch) }
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descanso activo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Cuenta atrás del descanso entre series"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(state: RestTimerState): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            state.isFinished -> "¡Descanso listo!"
            state.exerciseName.isNotBlank() -> "Descanso · ${state.exerciseName}"
            else -> "Descanso"
        }
        val text = when {
            state.isFinished -> "Toca para volver a tu entreno"
            !state.isRunning -> "${WeightFormatter.formatDuration(state.remainingSeconds.toLong())} · En pausa"
            else -> WeightFormatter.formatDuration(state.remainingSeconds.toLong())
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_kova)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(!state.isFinished)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)

        if (!state.isFinished) {
            builder.setProgress(state.totalSeconds.coerceAtLeast(1), state.remainingSeconds.coerceAtLeast(0), false)
            builder.addAction(0, "-15s", actionPendingIntent(ACTION_ADJUST, -15, 1))
            builder.addAction(0, "+15s", actionPendingIntent(ACTION_ADJUST, 15, 2))
            builder.addAction(0, "Saltar", actionPendingIntent(ACTION_DISMISS, 0, 3))
        }

        return builder.build()
    }

    private fun actionPendingIntent(action: String, delta: Int, requestCode: Int): PendingIntent {
        val intent = Intent(this, RestTimerForegroundService::class.java).apply {
            this.action = action
            if (delta != 0) putExtra(EXTRA_DELTA_SECONDS, delta)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "kova_rest_timer_v1"
        private const val NOTIFICATION_ID = 4101
        private const val ACTION_START = "com.kova.app.action.REST_START"
        private const val ACTION_ADJUST = "com.kova.app.action.REST_ADJUST"
        private const val ACTION_DISMISS = "com.kova.app.action.REST_DISMISS"
        private const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"

        fun start(context: Context) {
            val intent = Intent(context, RestTimerForegroundService::class.java).apply {
                action = ACTION_START
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
