package com.tennis.tracker.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text

private val ColorBg    = Color(0xFF080A0D)
private val ColorGreen = Color(0xFF3DD68C)
private val ColorText  = Color(0xFFFFFFFF)
private val ColorSub   = Color(0xFF8B9AB0)

@Composable
fun SignInScreen(
    isLoading: Boolean,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(ColorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "🎾",
                fontSize = 28.sp
            )
            Text(
                text = "Tennis Score",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                textAlign = TextAlign.Center
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    indicatorColor = ColorGreen,
                    trackColor = Color(0xFF1C2536)
                )
                Text(
                    text = "Signing in…",
                    fontSize = 11.sp,
                    color = ColorSub,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Sign in with your\nGoogle account",
                    fontSize = 11.sp,
                    color = ColorSub,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ColorGreen)
                ) {
                    Text(
                        text = "Sign in",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF080A0D)
                    )
                }
            }
        }
    }
}
