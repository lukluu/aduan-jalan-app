package com.example.aduanjalan.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun TopBar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // tinggi TopBar
            .background(MaterialTheme.colorScheme.background)
            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), shape = RectangleShape) // border tipis bawah
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Aduan Jalan",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomStart) // posisi teks di bawah kiri
                .padding(bottom = 8.dp) // jarak dari border bawah
        )
    }
}
