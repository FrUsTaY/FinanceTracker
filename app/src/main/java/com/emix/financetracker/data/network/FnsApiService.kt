package com.emix.financetracker.data.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface FnsApiService {
    @FormUrlEncoded
    @POST("v1/check/get")
    suspend fun checkReceipt(
        @Field("qrraw") qrRaw: String,
        @Field("token") token: String
    ): Response<ReceiptResponse>
}