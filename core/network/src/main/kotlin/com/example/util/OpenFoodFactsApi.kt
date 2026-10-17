package com.example.util

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object OpenFoodFactsApi {

    private const val BASE_URL = "https://world.openfoodfacts.org/"

    // Response models
    data class SearchResponse(
        val products: List<OffProduct>?
    )

    data class BarcodeResponse(
        val product: OffProduct?,
        val status: Int?
    )

    data class OffProduct(
        val code: String?,
        @param:Json(name = "product_name") val productName: String?,
        val categories: String?,
        @param:Json(name = "image_url") val imageUrl: String?,
        val brands: String?,
        @param:Json(name = "generic_name") val genericName: String?
    )

    interface Service {
        @GET("cgi/search.pl")
        suspend fun searchProducts(
            @Query("search_terms") terms: String,
            @Query("search_simple") simple: Int = 1,
            @Query("action") action: String = "process",
            @Query("json") json: Int = 1,
            @Query("fields") fields: String = "code,product_name,categories,image_url,brands,generic_name",
            @Query("tagtype_0") tagtype0: String? = null,
            @Query("tag_contains_0") tagContains0: String? = null,
            @Query("tag_0") tag0: String? = null
        ): SearchResponse

        @GET("api/v2/product/{barcode}.json")
        suspend fun getProductByBarcode(
            @Path("barcode") barcode: String,
            @Query("fields") fields: String = "code,product_name,categories,image_url,brands,generic_name"
        ): BarcodeResponse
    }

    fun formatQueryForOff(query: String): String {
        var result = query.trim()
        
        // Split camelCase / PascalCase into words (e.g. "CocaCola" or "CoCaCola" -> "Coca Cola")
        val camelRegex = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])".toRegex()
        result = result.replace(camelRegex, " ")
        
        // Replace common glued case-insensitive brand names with spaced names
        val lower = result.lowercase()
        if (lower == "cocacola") {
            result = "coca cola"
        } else if (lower.contains("cocacola")) {
            result = result.replace("(?i)cocacola".toRegex(), "coca cola")
        }
        
        return result
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "VarotraApp - Android - Version 1.0 - shellinopierre@gmail.com")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: Service by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Service::class.java)
    }
}
