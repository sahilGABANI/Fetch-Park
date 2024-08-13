package com.hoxbox.terminal.api.clockinout

import com.google.gson.Gson
import com.hoxbox.terminal.api.clockinout.model.ClockInOutHistoryResponse
import com.hoxbox.terminal.api.clockinout.model.SubmitTimeRequest
import com.hoxbox.terminal.api.clockinout.model.TimeResponse
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import io.reactivex.Single
import timber.log.Timber

class ClockInOutRepository(
    private val clockInOutRetrofitAPI: ClockInOutRetrofitAPI
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun submitTime(submitTimeRequest: SubmitTimeRequest): Single<HotBoxCommonResponse> {
        Timber.tag("OkHttpClient").i("SubmitTimeRequest :${Gson().toJson(submitTimeRequest)}")
        return clockInOutRetrofitAPI.submitTime(submitTimeRequest)
            .flatMap { hotBoxResponseConverter.convertCommonResponse(it) }
    }

    fun getCurrentTime(userId: Int): Single<TimeResponse> {
        return clockInOutRetrofitAPI.getCurrentTime(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun clockInOutData(userId: Int, month: Int, year: Int): Single<ClockInOutHistoryResponse> {
        val monthYear = "${month}-${year}".toDate("MM-yyyy")?.formatTo("MM-yyyy").toString()
        return clockInOutRetrofitAPI.getClockInOutTime(userId, monthYear)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}