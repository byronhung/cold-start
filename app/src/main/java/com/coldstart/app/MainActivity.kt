package com.coldstart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Chunk 00: prove the app builds and installs. Real colours and fonts arrive in chunk 01.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Placeholder() }
    }
}

@Composable
private fun Placeholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1017))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cold Start", color = Color(0xFFE6EAF2), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "chunk 00 · it runs",
            color = Color(0xFF3FCFB4),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Preview
@Composable
private fun PlaceholderPreview() = Placeholder()
