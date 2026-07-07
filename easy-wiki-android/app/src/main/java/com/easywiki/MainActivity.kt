package com.easywiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.easywiki.ui.navigation.EasyWikiNavGraph
import com.easywiki.ui.theme.EasyWikiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as EasyWikiApplication

        setContent {
            EasyWikiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EasyWikiNavGraph(
                        settingsDataStore = app.settingsDataStore,
                        authRepository = app.authRepository
                    )
                }
            }
        }
    }
}
