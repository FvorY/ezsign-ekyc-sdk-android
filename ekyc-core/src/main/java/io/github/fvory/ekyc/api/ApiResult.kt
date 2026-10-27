package io.github.fvory.ekyc.api

import kotlinx.coroutines.CancellationException

internal inline fun <T> apiResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
