package com.rolang.app

import android.app.Application
import com.rolang.app.diagnostics.DiagnosticLogger

class RoLangApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.initialize(this)
    }
}
