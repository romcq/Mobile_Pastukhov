package com.example.mynewsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsapp.data.repository.NewsRepository
import com.example.mynewsapp.data.model.Article
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    private val repository = NewsRepository()

    private val _articles = MutableLiveData<List<Article>>(emptyList())
    val articles: LiveData<List<Article>> = _articles

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _selectedCategory = MutableLiveData("general")
    val selectedCategory: LiveData<String> = _selectedCategory

    private var currentPage = 1
    private var lastPageReached = false
    private val pageSize = 20

    init {
        loadTopHeadlines()
    }

    fun retry() {
        loadTopHeadlines()
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadTopHeadlines()
        }
    }

    fun loadTopHeadlines() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                currentPage = 1
                lastPageReached = false
                val response = repository.fetchTopHeadlines(
                    category = _selectedCategory.value ?: "general",
                    page = currentPage,
                    pageSize = pageSize
                )
                _articles.value = response.articles
                // Если новостей меньше pageSize — страницы закончились
                lastPageReached = response.articles.size < pageSize
            } catch (t: Throwable) {
                _error.value = t.message ?: "Ошибка загрузки"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value == true || lastPageReached) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                currentPage++
                val response = repository.fetchTopHeadlines(
                    category = _selectedCategory.value ?: "general",
                    page = currentPage,
                    pageSize = pageSize
                )
                if (response.articles.isEmpty() || response.articles.size < pageSize) {
                    lastPageReached = true
                }
                _articles.value = _articles.value.orEmpty() + response.articles
            } catch (t: Throwable) {
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}
