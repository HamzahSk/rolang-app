plugins {
    id("com.diffplug.spotless") version "7.0.2"
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

spotless {
    // Cover all modules from the root, including shared and Android test sources.
    val excluded =
        arrayOf("**/build/**", "**/generated/**", "**/.gradle/**", "**/.git/**", "rolang-server/**")
    kotlin {
        target("**/src/**/*.kt")
        targetExclude(*excluded)
        ktfmt("0.54").kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude(*excluded)
        ktfmt("0.54").kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("misc") {
        target(
            "*.md",
            ".gitignore",
            "*.properties",
            "gradle/**/*.properties",
            ".github/workflows/*.yml",
        )
        targetExclude(*excluded)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
