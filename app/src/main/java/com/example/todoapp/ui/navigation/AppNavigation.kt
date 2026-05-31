package com.example.todoapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todoapp.ui.screens.deadlines.DeadlinesScreen
import com.example.todoapp.ui.screens.taskdetail.TaskDetailScreen
import com.example.todoapp.ui.screens.tasklist.TaskListScreen

// ── Маршруты ────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    data object TaskList   : Screen("task_list")
    data object Deadlines  : Screen("deadlines")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
        // Специальный маркер для создания новой задачи
        const val NEW_TASK_ID = "new"
    }
}

// Вкладки нижней навигации
private data class BottomTab(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Screen.TaskList,  "Задачи",    Icons.AutoMirrored.Filled.List),
    BottomTab(Screen.Deadlines, "Дедлайны",  Icons.Default.DateRange)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.TaskList.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.TaskList.route) {
                TaskListScreen(
                    onTaskClick    = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    },
                    onAddTaskClick = {
                        navController.navigate(
                            Screen.TaskDetail.createRoute(Screen.TaskDetail.NEW_TASK_ID)
                        )
                    }
                )
            }

            composable(Screen.Deadlines.route) {
                DeadlinesScreen(
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }

            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                TaskDetailScreen(
                    taskId    = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Скрываем нижнюю панель на экране деталей задачи
    val showBottomBar = bottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
    }
    if (!showBottomBar) return

    NavigationBar {
        bottomTabs.forEach { tab ->
            val selected = currentDestination?.hierarchy
                ?.any { it.route == tab.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick  = {
                    navController.navigate(tab.screen.route) {
                        // Очищаем back stack до стартового экрана —
                        // предотвращает накопление вкладок в стеке
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
