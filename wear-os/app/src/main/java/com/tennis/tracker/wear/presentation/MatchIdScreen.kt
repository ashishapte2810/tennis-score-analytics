package com.tennis.tracker.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*

private val ColorBg    = Color(0xFF080A0D)
private val ColorGreen = Color(0xFF3DD68C)
private val ColorText  = Color(0xFFE6EDF3)
private val ColorMuted = Color(0xFF484F58)
private val ColorSub   = Color(0xFF8B9AB0)

@Composable
fun MatchIdScreen(
    isConnecting: Boolean = false,
    onConnect: (String) -> Unit
) {
    if (isConnecting) {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    indicatorColor = ColorGreen,
                    trackColor = Color(0xFF1C2536)
                )
                Text(
                    text = "Finding match…",
                    fontSize = 12.sp,
                    color = ColorSub,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    // Digit state for 6-char ID entry using +/- buttons
    val digits = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    var selected by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize().background(ColorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text("🎾", fontSize = 20.sp)

            Text(
                text = "Match ID",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )

            // Display the 6 digits
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                digits.forEachIndexed { i, d ->
                    val isSelected = i == selected
                    Text(
                        text = chars[d].toString(),
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) ColorGreen else ColorMuted
                    )
                }
            }

            // Prev / Next char for selected digit
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactButton(
                    onClick = { digits[selected] = (digits[selected] - 1 + chars.length) % chars.length },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1C2536))
                ) { Text("−", color = ColorText, fontSize = 14.sp) }

                CompactButton(
                    onClick = { digits[selected] = (digits[selected] + 1) % chars.length },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1C2536))
                ) { Text("+", color = ColorText, fontSize = 14.sp) }
            }

            // Move to next digit / confirm
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selected > 0) {
                    CompactButton(
                        onClick = { selected-- },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1C2536))
                    ) { Text("◀", color = ColorMuted, fontSize = 11.sp) }
                }
                if (selected < 5) {
                    CompactButton(
                        onClick = { selected++ },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1C2536))
                    ) { Text("▶", color = ColorText, fontSize = 11.sp) }
                } else {
                    CompactButton(
                        onClick = {
                            val id = digits.map { chars[it] }.joinToString("")
                            onConnect(id)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = ColorGreen)
                    ) { Text("✓", color = Color(0xFF080A0D), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Text(
                text = "Start a match on\nyour phone first",
                fontSize = 9.sp,
                color = ColorSub,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
