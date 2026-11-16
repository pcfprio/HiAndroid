package com.pcfprio.net.retrofit

import com.pcfprio.net.DefaultNetworkErrorMapper
import com.pcfprio.net.NetworkError
import com.pcfprio.net.NetworkErrorMapper
import retrofit2.HttpException

/**
 * Retrofit 异常映射。
 *
 * 保留 HTTP 状态码，业务层可据此区分认证失效、服务端异常等情况。
 */
class RetrofitNetworkErrorMapper(
    private val fallback: NetworkErrorMapper = DefaultNetworkErrorMapper,
) : NetworkErrorMapper {
    override fun map(throwable: Throwable): NetworkError {
        if (throwable is HttpException) {
            return throwable.toHttpFailure()
        }

        return fallback.map(throwable)
    }
}
