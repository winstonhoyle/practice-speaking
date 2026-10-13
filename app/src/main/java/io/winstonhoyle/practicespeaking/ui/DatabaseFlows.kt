package io.winstonhoyle.practicespeaking.ui

import io.winstonhoyle.practicespeaking.error.StorageError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Turns a failing database flow into [fallback] plus a [StorageError.DatabaseFailed] passed to
 * [onError], so a read-only screen shows an error instead of crashing its ViewModel.
 */
fun <T> Flow<T>.catchDatabase(fallback: T, onError: (StorageError.DatabaseFailed) -> Unit): Flow<T> =
    catch { e ->
        onError(StorageError.DatabaseFailed(e.message.orEmpty()))
        emit(fallback)
    }
