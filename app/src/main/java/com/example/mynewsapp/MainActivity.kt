package com.example.mynewsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mynewsapp.ui.articles.ArticlesScreen
import com.example.mynewsapp.ui.detail.DetailScreen
import com.example.mynewsapp.ui.theme.MyNewsAppTheme
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNewsAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "articles") {
        composable("articles") {
            ArticlesScreen(onItemClick = { url ->
                navController.navigate("detail/${Uri.encode(url)}")
            })
        }
        composable(
            "detail/{articleUrl}",
            arguments = listOf(navArgument("articleUrl") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("articleUrl") ?: ""
            DetailScreen(url = url)
        }
    }
}
