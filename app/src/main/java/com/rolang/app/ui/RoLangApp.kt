package com.rolang.app.ui

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rolang.app.LearningViewModel
import com.rolang.app.speech.PronunciationAvailability

private data class Destination(val route: String, val title: String, val icon: ImageVector)

private val destinations =
    listOf(
        Destination("home", "Beranda", Icons.Outlined.Home),
        Destination("lessons", "Pelajaran", Icons.Outlined.School),
        Destination("stories", "Cerita", Icons.AutoMirrored.Outlined.MenuBook),
        Destination("vocabulary", "Kosakata", Icons.Outlined.Bookmarks),
        Destination("review", "Tinjauan", Icons.Outlined.AutoAwesome),
        Destination("profile", "Profil", Icons.Outlined.PersonOutline),
    )

@Composable
fun RoLangApp(viewModel: LearningViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val quiz by viewModel.quiz.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pronunciation by viewModel.pronunciationState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !state.preferences.dark
                isAppearanceLightNavigationBars = !state.preferences.dark
            }
        }
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    fun navigate(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(pronunciation.availability) {
        if (
            pronunciation.availability in
                setOf(
                    PronunciationAvailability.MISSING_DATA,
                    PronunciationAvailability.UNSUPPORTED,
                    PronunciationAvailability.ERROR,
                )
        ) {
            snackbar.showSnackbar(pronunciation.message)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.pronunciation.refresh() }
    CompositionLocalProvider(
        LocalPronunciationState provides pronunciation,
        LocalPronunciationController provides viewModel.pronunciation,
    ) {
        RoLangTheme(state.preferences.dark) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!route.startsWith("reader") && !route.startsWith("lesson/"))
                        NavigationBar {
                            destinations.forEach { item ->
                                NavigationBarItem(
                                    selected = route == item.route,
                                    onClick = { navigate(item.route) },
                                    modifier = Modifier.testTag("nav-${item.route}"),
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.title) },
                                )
                            }
                        }
                },
            ) { padding ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (state.error != null) {
                        Box(Modifier.padding(24.dp)) {
                            InfoCard("Pustaka lokal tidak tersedia", state.error!!)
                        }
                    } else if (!state.ready) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Menyiapkan pustaka…")
                        }
                    } else
                        NavHost(
                            nav,
                            startDestination = "home",
                            modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                        ) {
                            composable("home") {
                                HomeScreen(
                                    state,
                                    now,
                                    { navigate("lessons") },
                                    { navigate("stories") },
                                    { navigate("review") },
                                    { nav.navigate("lesson/$it") },
                                    { nav.navigate("reader/$it") },
                                )
                            }
                            composable("lessons") {
                                LessonsScreen(state, viewModel) { nav.navigate("lesson/$it") }
                            }
                            composable("stories") {
                                StoriesScreen(state, viewModel) { nav.navigate("reader/$it") }
                            }
                            composable("vocabulary") {
                                VocabularyScreen(
                                    state,
                                    now,
                                    viewModel::mastery,
                                    viewModel::delete,
                                    viewModel::status,
                                ) {
                                    navigate("stories")
                                }
                            }
                            composable("review") {
                                ReviewScreen(
                                    state,
                                    now,
                                    quiz,
                                    busy,
                                    viewModel::startQuiz,
                                    viewModel::answer,
                                    viewModel::reveal,
                                    viewModel::rate,
                                    viewModel::exitQuiz,
                                ) {
                                    navigate("stories")
                                }
                            }
                            composable("profile") {
                                ProfileScreen(state, viewModel::font, viewModel::dark) {
                                    DiagnosticLogCard()
                                }
                            }
                            composable("reader/{id}") { backStack ->
                                StoryContentRoute(
                                    backStack.arguments?.getString("id").orEmpty(),
                                    viewModel,
                                    { nav.popBackStack() },
                                ) { loaded ->
                                    ReaderScreen(
                                        loaded,
                                        state,
                                        viewModel,
                                        onBack = { nav.popBackStack() },
                                    )
                                }
                            }
                            composable("lesson/{id}") { backStack ->
                                StoryContentRoute(
                                    backStack.arguments?.getString("id").orEmpty(),
                                    viewModel,
                                    { nav.popBackStack() },
                                ) { loaded ->
                                    val index = state.lessons.indexOfFirst { it.id == loaded.id }
                                    val previous = state.lessons.getOrNull(index - 1)
                                    val next = state.lessons.getOrNull(index + 1)
                                    LessonScreen(
                                        loaded,
                                        state,
                                        viewModel,
                                        index + 1,
                                        onBack = { nav.popBackStack() },
                                        onComplete = { score, total, answers ->
                                            viewModel.completeExercises(
                                                loaded,
                                                score,
                                                total,
                                                answers,
                                            )
                                        },
                                        onPrevious =
                                            previous?.let { previousLesson ->
                                                { nav.navigate("lesson/${previousLesson.id}") }
                                            },
                                        onNext =
                                            next?.let { nextLesson ->
                                                { nav.navigate("lesson/${nextLesson.id}") }
                                            },
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}
