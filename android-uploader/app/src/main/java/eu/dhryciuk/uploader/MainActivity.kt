package eu.dhryciuk.uploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import eu.dhryciuk.uploader.ui.DHUploaderApp
import eu.dhryciuk.uploader.ui.DHUploaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DHUploaderTheme {
                DHUploaderApp()
            }
        }
    }
}
