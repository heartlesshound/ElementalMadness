package com.example.elementalsmash

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar
        supportActionBar?.hide()

        // Go full-screen immersive
        enableFullscreen()

        // Create GameView
        gameView = GameView(this)

        // Hook up listener for Restart / Exit buttons
        gameView.listener = object : GameView.GameViewListener {
            override fun onExitRequested() {
                finish()
                System.exit(0)   // table expects apps to close cleanly
            }

            override fun onRestartRequested() {
                gameView.resetGameFromActivity()
            }
        }

        // Show the view
        setContentView(gameView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableFullscreen()
    }

    private fun enableFullscreen() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }
}
