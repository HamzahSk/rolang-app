package com.rolang.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.rolang.app.LearningViewModel
import com.rolang.app.R
import com.rolang.app.data.Story

@DrawableRes
fun illustrationResource(name: String): Int =
    when (name) {
        "sounds" -> R.drawable.illustration_sounds
        "greetings" -> R.drawable.illustration_greetings
        "family" -> R.drawable.illustration_family
        "time" -> R.drawable.illustration_time
        "classroom" -> R.drawable.illustration_classroom
        "food" -> R.drawable.illustration_food
        "travel" -> R.drawable.illustration_travel
        else -> R.drawable.illustration_friends
    }

@Composable
fun StoryIllustration(
    story: Story,
    viewModel: LearningViewModel?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val remote = viewModel != null && story.contentPath.isNotBlank() && story.image in story.assets
    if (!remote) {
        Image(
            painterResource(illustrationResource(story.image)),
            null,
            modifier,
            contentScale = contentScale,
        )
        return
    }
    SubcomposeAsyncImage(
        model = viewModel!!.assetModel(story, story.image),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        error = {
            Image(
                painterResource(illustrationResource(story.family)),
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        },
        success = { SubcomposeAsyncImageContent() },
    )
}
