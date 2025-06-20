package com.example.mynewsapp.ui.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mynewsapp.data.model.Article
import com.example.mynewsapp.viewmodel.NewsViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.example.mynewsapp.utils.ReadStatusManager
import com.example.mynewsapp.utils.FavoriteManager

// Импортируй свой WebViewScreen:
import com.example.mynewsapp.ui.articles.ArticleWebViewScreen

private val categories = listOf(
    "general" to "Главные",
    "business" to "Бизнес",
    "entertainment" to "Развлечения",
    "health" to "Здоровье",
    "science" to "Наука",
    "sports" to "Спорт",
    "technology" to "Технологии"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(onItemClick: (String) -> Unit) {
    val vm: NewsViewModel = viewModel()
    val articles by vm.articles.observeAsState(emptyList())
    val isLoading by vm.isLoading.observeAsState(false)
    val error by vm.error.observeAsState()
    val isLoadingMore by vm.isLoadingMore.observeAsState(false)
    val context = LocalContext.current

    val selectedCategory by vm.selectedCategory.observeAsState("general")
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var favoritesVersion by remember { mutableStateOf(0) }

    // Добавляем переменную состояния для WebView
    var selectedUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мои новости") },
                actions = {
                    IconButton(onClick = { showOnlyFavorites = !showOnlyFavorites }) {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (showOnlyFavorites) "Показать все" else "Показать только избранное",
                            tint = if (showOnlyFavorites) Color(0xFFFFD600) else Color.Gray
                        )
                    }
                    IconButton(onClick = { vm.retry() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val swipeState = rememberSwipeRefreshState(isRefreshing = isLoading)
            SwipeRefresh(
                state = swipeState,
                onRefresh = { vm.retry() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        items(categories) { (catKey, catTitle) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = {
                                    vm.selectCategory(catKey)
                                },
                                label = { Text(catTitle) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    val displayedArticles = remember(articles, showOnlyFavorites, favoritesVersion) {
                        if (showOnlyFavorites) {
                            val favorites = FavoriteManager.getFavorites(context)
                            articles.filter { favorites.contains(it.url) }
                        } else {
                            articles
                        }
                    }

                    when {
                        error != null -> {
                            ErrorView(error = error!!, onRetry = { vm.retry() })
                        }
                        isLoading && articles.isEmpty() -> {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                        else -> {
                            ArticlesList(
                                articles = displayedArticles,
                                onItemClick = { url ->
                                    ReadStatusManager.markAsRead(context, url)
                                    selectedUrl = url     // <--- открытие WebView
                                },
                                onToggleFavorite = { favoritesVersion++ },
                                favoritesVersion = favoritesVersion,
                                onLoadMore = { vm.loadMore() },
                                isLoadingMore = isLoadingMore
                            )
                        }
                    }
                }
            }

            // Модальное окно WebView
            if (selectedUrl != null) {
                AlertDialog(
                    onDismissRequest = { selectedUrl = null },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { selectedUrl = null }) {
                            Text("Закрыть")
                        }
                    },
                    text = {
                        Box(
                            Modifier
                                .height(500.dp)
                                .fillMaxWidth()
                        ) {
                            ArticleWebViewScreen(url = selectedUrl!!)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun ArticlesList(
    articles: List<Article>,
    onItemClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    favoritesVersion: Int,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Автоматическая подгрузка при скролле вниз
    LaunchedEffect(listState.firstVisibleItemIndex, articles.size) {
        val lastVisibleItem = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size
        if (lastVisibleItem >= articles.size - 5 && articles.isNotEmpty()) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = articles,
            key = { it.url }
        ) { article ->
            val isRead = ReadStatusManager.isRead(context, article.url)
            val isFavorite = remember(favoritesVersion) {
                FavoriteManager.isFavorite(context, article.url)
            }
            ArticleItem(
                article = article,
                isRead = isRead,
                isFavorite = isFavorite,
                onClick = { onItemClick(article.url) },
                onToggleFavorite = {
                    FavoriteManager.toggleFavorite(context, article.url)
                    onToggleFavorite()
                }
            )
        }

        // Индикатор загрузки в конце списка
        if (isLoadingMore) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ArticleItem(
    article: Article,
    isRead: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            article.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }

            // --------- Автор и источник ---------
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!article.author.isNullOrBlank()) {
                    Text(
                        text = article.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (!article.source?.name.isNullOrBlank()) {
                    if (!article.author.isNullOrBlank()) Spacer(Modifier.width(12.dp))
                    Text(
                        text = article.source?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // -------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRead) Color.Gray else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    onToggleFavorite()
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                        tint = if (isFavorite) Color(0xFFFFD600) else Color.Gray
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // --- Формат даты: MM/dd/yyyy ---
            val formattedDate = remember(article.publishedAt) {
                try {
                    ZonedDateTime.parse(article.publishedAt)
                        .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                } catch (e: Exception) {
                    ""
                }
            }
            if (formattedDate.isNotEmpty()) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
            }

            article.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = if (isRead) Color.Gray else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
