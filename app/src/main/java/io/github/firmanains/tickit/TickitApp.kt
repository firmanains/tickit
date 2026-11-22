package io.github.firmanains.tickit

import android.app.Application
import android.content.Context
import io.github.firmanains.tickit.di.AppContainer

class TickitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannel()
    }
}

val Context.container: AppContainer get() = (applicationContext as TickitApp).container
