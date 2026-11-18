package com.mangareader.local

import com.mangareader.local.model.*
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val stats = WordStats(this, lifecycleScope)
        val reader = Reader(lifecycleScope, CameraController(this, this), ServerClient(this), stats)
        val store = WordStore(this)

        val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) reader.startCamera() else reader.permissionDenied()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            reader.startCamera()
        } else {
            askCamera.launch(Manifest.permission.CAMERA)
        }

        setContent { MangaTheme { AppRoot(reader, store) } }
    }
}
