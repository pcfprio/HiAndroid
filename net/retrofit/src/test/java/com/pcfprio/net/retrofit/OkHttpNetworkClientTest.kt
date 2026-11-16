package com.pcfprio.net.retrofit

import com.pcfprio.base.result.AppResult
import com.pcfprio.net.Endpoint
import com.pcfprio.net.HttpMethod
import com.pcfprio.net.NetworkLogEvent
import com.pcfprio.net.NetworkLogSink
import com.pcfprio.net.NetworkRequest
import com.pcfprio.net.ServerError
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpNetworkClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpNetworkClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val config = RetrofitNetworkConfig(
            baseUrl = server.url("/").toString(),
        )
        client = OkHttpNetworkClient(
            okHttpClient = OkHttpClient(),
            requestFactory = OkHttpRequestFactory(config),
            errorMapper = com.pcfprio.net.DefaultNetworkErrorMapper,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun clientReturnsBodyBytesWhenRequestSucceeds() = runBlocking {
        server.enqueue(
            MockResponse(
                code = 200,
                body = "hello",
            ),
        )

        val result = client.execute(
            NetworkRequest(
                method = HttpMethod.GET,
                endpoint = Endpoint.RelativeUrl("repos"),
            ),
        )

        assertTrue(result is AppResult.Success)
        assertArrayEquals("hello".toByteArray(), (result as AppResult.Success).value)
        assertEquals("/repos", server.takeRequest().url.encodedPath)
    }

    @Test
    fun clientMapsHttpFailureStatusCode() = runBlocking {
        server.enqueue(
            MockResponse(
                code = 404,
                body = "not found",
            ),
        )

        val result = client.execute(
            NetworkRequest(
                method = HttpMethod.GET,
                endpoint = Endpoint.RelativeUrl("missing"),
            ),
        )

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is ServerError.HttpFailure)
        assertEquals(404, (error as ServerError.HttpFailure).statusCode)
    }

    @Test
    fun clientLogsNetworkEvents() = runBlocking {
        val events = mutableListOf<NetworkLogEvent>()
        val config = RetrofitNetworkConfig(
            baseUrl = server.url("/").toString(),
        )
        client = OkHttpNetworkClient(
            okHttpClient = OkHttpClient(),
            requestFactory = OkHttpRequestFactory(config),
            errorMapper = com.pcfprio.net.DefaultNetworkErrorMapper,
            logSink = NetworkLogSink { event -> events += event },
        )
        server.enqueue(
            MockResponse(
                code = 200,
                body = "hello",
            ),
        )

        client.execute(
            NetworkRequest(
                method = HttpMethod.GET,
                endpoint = Endpoint.RelativeUrl("repos"),
            ),
        )

        assertEquals(2, events.size)
        assertTrue(events[0] is NetworkLogEvent.RequestStarted)
        assertTrue(events[1] is NetworkLogEvent.ResponseReceived)
    }
}