package com.pcfprio.net.retrofit

import com.pcfprio.base.result.AppResult
import com.pcfprio.net.LoggingNetworkClient
import com.pcfprio.net.NetworkClient
import com.pcfprio.net.NetworkErrorMapper
import com.pcfprio.net.NetworkLogSink
import com.pcfprio.net.NetworkRequest
import com.pcfprio.net.NetworkResult
import com.pcfprio.net.NoOpNetworkLogSink
import com.pcfprio.net.ServerError
import com.pcfprio.net.runNetworkCatching
import okhttp3.OkHttpClient

/**
 * 基于 OkHttp 的 NetworkClient 实现。
 * 该实现只返回原始 ByteArray，JSON 解析仍由上层 mapper 或 Retrofit service 负责。
 */
class OkHttpNetworkClient(
    okHttpClient: OkHttpClient,
    requestFactory: OkHttpRequestFactory,
    errorMapper: NetworkErrorMapper,
    logSink: NetworkLogSink = NoOpNetworkLogSink,
) : NetworkClient {
    private val loggingClient = LoggingNetworkClient(
        delegate = RawOkHttpNetworkClient(
            okHttpClient = okHttpClient,
            requestFactory = requestFactory,
            errorMapper = errorMapper,
        ),
        logSink = logSink,
    )

    constructor(
        config: RetrofitNetworkConfig,
        okHttpClient: OkHttpClient = RetrofitNetworkFactory.createOkHttpClient(config),
        errorMapper: NetworkErrorMapper = com.pcfprio.net.DefaultNetworkErrorMapper,
        logSink: NetworkLogSink = NoOpNetworkLogSink,
    ) : this(
        okHttpClient = okHttpClient,
        requestFactory = OkHttpRequestFactory(config),
        errorMapper = errorMapper,
        logSink = logSink,
    )

    override suspend fun execute(request: NetworkRequest): NetworkResult<ByteArray> {
        return loggingClient.execute(request)
    }
}

private class RawOkHttpNetworkClient(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: OkHttpRequestFactory,
    private val errorMapper: NetworkErrorMapper,
) : NetworkClient {
    override suspend fun execute(request: NetworkRequest): NetworkResult<ByteArray> {
        return runNetworkCatching(errorMapper = errorMapper) {
            okHttpClient.newCall(requestFactory.create(request)).execute().use { response ->
                val bytes = response.body.bytes()
                if (response.isSuccessful) {
                    bytes
                } else {
                    throw HttpStatusException(
                        statusCode = response.code,
                        message = response.message,
                        apiMessage = bytes.decodeToString().let(::parseApiErrorMessage),
                    )
                }
            }
        }.let { result ->
            when (result) {
                is AppResult.Success -> result
                is AppResult.Failure -> {
                    val httpStatusException = result.error.cause as? HttpStatusException
                    if (httpStatusException != null) {
                        AppResult.Failure(
                            ServerError.HttpFailure(
                                statusCode = httpStatusException.statusCode,
                                message = httpStatusException.apiMessage,
                                debugMessage = httpStatusException.message,
                                cause = httpStatusException,
                            ),
                        )
                    } else {
                        result
                    }
                }
            }
        }
    }
}

/**
 * 仅用于在内部把非 2xx 状态码从执行块带出来。
 */
private class HttpStatusException(
    val statusCode: Int,
    override val message: String?,
    val apiMessage: String?,
) : RuntimeException(message)
