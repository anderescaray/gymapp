package com.kova.app.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CloudSyncStatus {
    data object ReadyToConfigure : CloudSyncStatus
    data class Connected(val accountEmail: String, val lastSyncedAtMillis: Long?) : CloudSyncStatus
    data object Syncing : CloudSyncStatus
    data class Error(val message: String) : CloudSyncStatus
}

/**
 * Abstraction layer for future cloud backup synchronization (e.g., Google Drive appDataFolder).
 * Uses the exact same KovaBackupSnapshot format as local JSON export/import so enabling
 * Google Drive sync only requires plugging in Credential Manager + Drive REST API v3 credentials.
 */
interface CloudSyncProvider {
    val providerName: String
    val backupFileName: String
    val status: StateFlow<CloudSyncStatus>

    suspend fun uploadSnapshot(snapshot: KovaBackupSnapshot): Result<Unit>
    suspend fun downloadLatestSnapshot(): Result<KovaBackupSnapshot?>
}

class GoogleDriveSyncManager : CloudSyncProvider {
    override val providerName: String = "Google Drive"
    override val backupFileName: String = "kova-backup-v1.json"

    private val _status = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.ReadyToConfigure)
    override val status: StateFlow<CloudSyncStatus> = _status.asStateFlow()

    override suspend fun uploadSnapshot(snapshot: KovaBackupSnapshot): Result<Unit> {
        // Validate snapshot serialization immediately so the pipeline stays healthy end-to-end
        KovaBackupCodec.encode(snapshot)
        return Result.failure(
            IllegalStateException(
                "Para activar la sincronización con Google Drive añade tu OAuth Client ID de Google Cloud (scope drive.appdata)."
            )
        )
    }

    override suspend fun downloadLatestSnapshot(): Result<KovaBackupSnapshot?> {
        return Result.failure(
            IllegalStateException(
                "Sincronización con Google Drive pendiente de configurar OAuth Client ID."
            )
        )
    }
}
