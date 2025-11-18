package com.example.elementalmadness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*   // Box, Column, Row, size, padding, fillMaxSize, etc.
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.elementalmadness.ui.theme.ElementalMadnessTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
                ElementalMadnessTheme {
                    GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background board
        Image(
            painter = painterResource(id = R.drawable.bkgndboard),
            contentDescription = "Game board",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 3x3 grid of holes on top
        HolesGrid()
    }
}

@Composable
fun HolesGrid() {
    val holeSize = 120.dp  // adjust if they look too big/small

    Column(
        modifier = Modifier
            .fillMaxSize()
            // adjust padding to sit nicely inside the wooden frame
            .padding(horizontal = 160.dp, vertical = 100.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Image(
                        painter = painterResource(id = R.drawable.runehole),
                        contentDescription = "Hole",
                        modifier = Modifier.size(holeSize)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ElementalMadnessTheme {
        Greeting("Android")
    }
}