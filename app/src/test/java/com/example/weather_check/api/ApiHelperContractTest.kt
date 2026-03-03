package com.example.weather_check.api

import com.example.weather_check.ApiConfig
import com.example.weather_check.models.AddFavoriteRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ApiHelperContractTest {
    private val token = "test-token"

    @Test
    fun getHistoryRequest_matchesContractMethodAndPath() {
        val request = ApiHelper.buildGetRequest(ApiConfig.HISTORY_ENDPOINT, token)

        assertEquals("GET", request.method)
        assertEquals("${ApiConfig.BASE_URL}${ApiConfig.HISTORY_ENDPOINT}", request.url.toString())
        assertEquals("Bearer $token", request.header("Authorization"))
    }

    @Test
    fun deleteHistoryRequest_matchesContractMethodAndPath() {
        val request = ApiHelper.buildDeleteRequest(ApiConfig.HISTORY_ENDPOINT, token)

        assertEquals("DELETE", request.method)
        assertEquals("${ApiConfig.BASE_URL}${ApiConfig.HISTORY_ENDPOINT}", request.url.toString())
        assertEquals("Bearer $token", request.header("Authorization"))
    }

    @Test
    fun getFavoritesRequest_matchesContractMethodAndPath() {
        val request = ApiHelper.buildGetRequest(ApiConfig.FAVORITES_ENDPOINT, token)

        assertEquals("GET", request.method)
        assertEquals("${ApiConfig.BASE_URL}${ApiConfig.FAVORITES_ENDPOINT}", request.url.toString())
        assertEquals("Bearer $token", request.header("Authorization"))
    }

    @Test
    fun postFavoritesRequest_matchesContractMethodPathAndJsonBody() {
        val request = ApiHelper.buildPostJsonRequest(
            ApiConfig.FAVORITES_ENDPOINT,
            AddFavoriteRequest(city = "London", country = "GB"),
            token
        )

        assertEquals("POST", request.method)
        assertEquals("${ApiConfig.BASE_URL}${ApiConfig.FAVORITES_ENDPOINT}", request.url.toString())
        assertEquals("Bearer $token", request.header("Authorization"))
        assertTrue(request.body != null)
    }

    @Test
    fun deleteFavoriteByCityRequest_usesEncodedCityPath() {
        val city = "Tel Aviv"
        val encoded = URLEncoder.encode(city, StandardCharsets.UTF_8.name())
        val endpoint = ApiConfig.FAVORITE_BY_CITY_ENDPOINT_TEMPLATE.format(encoded)

        val request = ApiHelper.buildDeleteRequest(endpoint, token)

        assertEquals("DELETE", request.method)
        assertEquals("${ApiConfig.BASE_URL}/api/favorites/Tel+Aviv", request.url.toString())
        assertEquals("Bearer $token", request.header("Authorization"))
    }

    @Test
    fun parseError_extractsContractErrorField() {
        val parsed = ApiHelper.parseError("{\"error\":\"City not found\"}")
        assertEquals("City not found", parsed)
    }
}

