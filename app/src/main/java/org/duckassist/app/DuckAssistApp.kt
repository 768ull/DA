package org.duckassist.app

import android.app.Application
import org.duckassist.app.data.ChatDatabase
import org.duckassist.app.data.ChatRepository
import org.duckassist.app.data.SettingsStore

class DuckAssistApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var repository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsStore = SettingsStore(this)
        val database = ChatDatabase.build(this)
        repository = ChatRepository(database.dao(), settingsStore)
    }

    companion object {
        lateinit var instance: DuckAssistApp
            private set
    }
}
