package com.example.mynewsapp.data.repository

import com.example.mynewsapp.data.model.NewsResponse
import com.example.mynewsapp.data.network.RetrofitInstance

class NewsRepository {
    suspend fun fetchTopHeadlines(
        country: String = "us",
        category: String = "general",
        page: Int = 1,
        pageSize: Int = 20
    ): NewsResponse {
        return RetrofitInstance.api.getTopHeadlines(
            country = country,
            category = category,
            page = page,
            pageSize = pageSize
        )
    }
}
