package com.mangareader.local

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mangareader.local.data.camera.AndroidPhotoCropper
import com.mangareader.local.data.camera.CameraController
import com.mangareader.local.data.local.FavouritesFile
import com.mangareader.local.data.local.StatsFile
import com.mangareader.local.data.remote.ServerClient
import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.ReaderViewModel
import com.mangareader.local.logic.SettingsViewModel
import com.mangareader.local.logic.WordStatistics
import com.mangareader.local.ui.AppRoot
import com.mangareader.local.ui.MangaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Composition root: the only place that picks the data-layer implementations.
        val server = ServerClient(this)
        val stats = WordStatistics(StatsFile(filesDir), lifecycleScope)
        val reader = ReaderViewModel(lifecycleScope, CameraController(this, this), server, AndroidPhotoCropper, stats)
        val favourites = Favourites(FavouritesFile(filesDir))
        val settings = SettingsViewModel(server, onChanged = reader::checkServer)

        val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) reader.startCamera() else reader.permissionDenied()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            reader.startCamera()
        } else {
            askCamera.launch(Manifest.permission.CAMERA)
        }

        setContent { MangaTheme { AppRoot(reader, favourites, stats, settings) } }
    }
}
