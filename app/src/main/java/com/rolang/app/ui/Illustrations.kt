package com.rolang.app.ui

import androidx.annotation.DrawableRes
import com.rolang.app.R

@DrawableRes
fun illustrationResource(name: String): Int = when (name) {
    "sounds" -> R.drawable.illustration_sounds
    "greetings" -> R.drawable.illustration_greetings
    "family" -> R.drawable.illustration_family
    "time" -> R.drawable.illustration_time
    "classroom" -> R.drawable.illustration_classroom
    "food" -> R.drawable.illustration_food
    "travel" -> R.drawable.illustration_travel
    else -> R.drawable.illustration_friends
}
