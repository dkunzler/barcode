package de.devland.scanner

import android.app.Application
import com.squareup.otto.Bus

/**
 * Created by deekay on 18.01.2018.
 */
class App : Application() {
    companion object {
        lateinit var instance : App
        val mainBus = Bus()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}