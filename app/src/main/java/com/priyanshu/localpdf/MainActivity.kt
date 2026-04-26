package com.priyanshu.localpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.priyanshu.localpdf.engine.PdfEngine
import com.priyanshu.localpdf.ui.screens.MainScreen
import com.priyanshu.localpdf.ui.screens.ToolConfigScreen
import com.priyanshu.localpdf.ui.theme.LocalPDFTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfEngine.init(this)
        com.priyanshu.localpdf.engine.StatsManager.init(this)

        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#1E1E2E"))

        setContent {
            LocalPDFTheme {
                LocalPdfApp()
            }
        }
    }
}

@Composable
fun LocalPdfApp() {
    val navController = rememberNavController()


    NavHost(
        navController = navController, 
        startDestination = "main",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) }
    ) {
        composable("main") {
            MainScreen(
                onToolClick = { toolId ->
                    navController.navigate("tool_config/$toolId")
                }
            )
        }
        composable(
            route = "tool_config/{toolId}",
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getString("toolId") ?: ""
            ToolConfigScreen(
                toolId = toolId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}