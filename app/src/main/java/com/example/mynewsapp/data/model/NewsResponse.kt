package com.example.mynewsapp.data.model

import com.squareup.moshi.Json

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val source: Source,
    val author: String?,
    val title: String,
    val description: String?,
    @Json(name = "url") val url: String,
    @Json(name = "urlToImage") val imageUrl: String?,
    @Json(name = "publishedAt") val publishedAt: String,
    val content: String?
)

data class Source(
    val id: String?,
    val name: String
)
