package com.hoxbox.terminal.base.network

import com.hoxbox.terminal.base.extension.onSafeError
import com.hoxbox.terminal.base.extension.onSafeSuccess
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single

class HotBoxResponseConverter {

    fun <T> convert(hotBoxResponse: HotBoxResponse<T>?): Single<T> {
        return convertToSingle(hotBoxResponse)
    }

    fun <T> convertToSingle(hotBoxResponse: HotBoxResponse<T>?): Single<T> {
        return Single.create { emitter ->
            when {
                hotBoxResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !hotBoxResponse.success -> {
                    emitter.onSafeError(Exception(hotBoxResponse.message))
                }
                hotBoxResponse.success -> {
                    emitter.onSafeSuccess(hotBoxResponse.data)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponse(hotBoxResponse: HotBoxResponse<T>?): Single<HotBoxResponse<T>> {
        return Single.create { emitter ->
            when {
                hotBoxResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !hotBoxResponse.success -> {
                        emitter.onSafeError(Exception(hotBoxResponse.message))
                }
                hotBoxResponse.success -> {
                    emitter.onSafeSuccess(hotBoxResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponse(hotBoxCommonResponse: HotBoxCommonResponse?): Single<HotBoxCommonResponse> {
        return Single.create { emitter ->
            when {
                hotBoxCommonResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !hotBoxCommonResponse.success -> {
                    emitter.onSafeError(Exception(hotBoxCommonResponse.message))
                }
                hotBoxCommonResponse.success -> {
                    emitter.onSafeSuccess(hotBoxCommonResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }
}