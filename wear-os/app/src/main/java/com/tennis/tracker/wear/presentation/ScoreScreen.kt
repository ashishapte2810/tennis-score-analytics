package com.tennis.tracker.wear.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.tennis.tracker.wear.data.MatchState
import com.tennis.tracker.wear.data.ServeType
import kotlinx.coroutines.delay

// Brand colors — matches phone PWA exactly
private val ColorGreen       = Color(0xFF3DD68C)
private val ColorGreenDim    = Color(0x1A3DD68C)   // rgba(61,214,140,0.10)
private val ColorGreenBorder = Color(0x4D3DD68C)   // rgba(61,214,140,0.30)
private val ColorOrange      = Color(0xFFF97316)
private val ColorOrangeDim   = Color(0x26F97316)   // rgba(249,115,22,0.15)
private val ColorOrangeBorder= Color(0x59F97316)   // rgba(249,115,22,0.35)
private val ColorAmber       = Color(0xFFFBBF24)
private val ColorAmberDim    = Color(0x33FBBF24)   // rgba(251,191,36,0.20)
private val ColorBg          = Color(0xFF080A0D)
private val ColorCard        = Color(0xFF111820)
private val ColorBall        = Color(0xFFC8E63A)
private val ColorText        = Color(0xFFFFFFFF)   // P1 score — white, matches phone
private val ColorSub         = Color(0xFF8B9AB0)   // player names
private val ColorMuted       = Color(0xFF484F58)
private val ColorScoreP2     = Color(0xFFFFFFFF)   // P2 score — white, same as P1

@Composable
fun ScoreScreen(
    state: MatchState,
    isSaving: Boolean,
    isAmbient: Boolean,
    matchId: String? = null,
    canUndo: Boolean = false,
    deciderPending: Boolean = false,
    uePending: Boolean = false,
    uePendingPlayer: Int = 0,
    frozenDisplayPts: List<String>? = null,
    onPoint: (player: Int, serveType: ServeType) -> Unit,
    onUndo: () -> Unit = {},
    onHome: () -> Unit = {},
    onConfirmDecider: (dec: String) -> Unit = {},
    onTagUE: () -> Unit = {},
    onDismissUE: () -> Unit = {}
) {
    if (isAmbient) {
        AmbientScoreScreen(state)
        return
    }

    if (state.done) {
        MatchOverScreen(state, onHome = onHome)
        return
    }

    var pendingPlayer by remember { mutableStateOf<Int?>(null) }

    // Auto-summary between games / sets.
    // showSummary is DERIVED — it becomes true the instant uePending clears,
    // with no intermediate composition frame, so there's no "0-0" flash.
    var pendingGameSummary by remember { mutableStateOf(false) }
    var lastGameWinner     by remember { mutableStateOf(0) }
    val initialized        = remember { mutableStateOf(false) }
    val trackedSetCount    = remember { mutableStateOf(0) }
    val trackedGames       = remember { mutableStateOf(listOf(0, 0)) }
    val showSummary        = pendingGameSummary && !uePending && !deciderPending

    LaunchedEffect(state.pointNum) {
        if (!initialized.value) {
            initialized.value = true
            trackedSetCount.value = state.completedSets.size
            trackedGames.value = state.games
            return@LaunchedEffect
        }
        // Reset on every point change (dismisses any current summary and handles undo).
        pendingGameSummary = false
        if (state.pts[0] == 0 && state.pts[1] == 0 &&
            state.tbPts[0] == 0 && state.tbPts[1] == 0 && !state.done) {
            val setJustEnded = state.completedSets.size > trackedSetCount.value
            lastGameWinner = if (setJustEnded) {
                val s = state.completedSets.last()
                if (s[0] > s[1]) 0 else 1
            } else {
                if (state.games[0] > trackedGames.value[0]) 0 else 1
            }
            pendingGameSummary = true
        }
        trackedSetCount.value = state.completedSets.size
        trackedGames.value = state.games
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))

            // Sets + games status line
            Text(
                text = state.statusLine(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ColorMuted,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center
            )

            // Match ID — visible so user can re-enter on phone if link is lost
            if (matchId != null) {
                Text(
                    text = matchId,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6E7681),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Saving dot
            Spacer(Modifier.height(3.dp))
            Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
                if (isSaving) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(ColorOrange))
                }
            }
            Spacer(Modifier.height(3.dp))

            // Two scoring halves
            val ue0 = uePending && uePendingPlayer == 0 && !showSummary && !deciderPending
            val ue1 = uePending && uePendingPlayer == 1 && !showSummary && !deciderPending

            LaunchedEffect(uePending) {
                if (uePending) { delay(2000L); onDismissUE() }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PlayerHalf(
                    modifier = Modifier.weight(1f),
                    player = 0,
                    state = state,
                    dimColor = ColorGreenDim,
                    isUePending = ue0,
                    overridePts = frozenDisplayPts?.get(0),
                    onTap = {
                        if (ue0) onTagUE()
                        else { if (uePending) onDismissUE(); onPoint(0, ServeType.FIRST) }
                    },
                    onLongPress = { pendingPlayer = 0 }
                )
                PlayerHalf(
                    modifier = Modifier.weight(1f),
                    player = 1,
                    state = state,
                    dimColor = ColorOrangeDim,
                    isUePending = ue1,
                    overridePts = frozenDisplayPts?.get(1),
                    onTap = {
                        if (ue1) onTagUE()
                        else { if (uePending) onDismissUE(); onPoint(1, ServeType.FIRST) }
                    },
                    onLongPress = { pendingPlayer = 1 }
                )
            }

            // Undo strip — bottom of screen, greyed when nothing to undo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(ColorBg)
                    .then(
                        if (canUndo) Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onUndo() })
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFF1C2536))
                        .align(Alignment.TopCenter)
                )
                Text(
                    text = "↩  UNDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canUndo) ColorMuted else Color(0xFF2E3A4A),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Serve type overlay (Option C — long press)
        AnimatedVisibility(
            visible = pendingPlayer != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            ServeTypeOverlay(
                player = pendingPlayer ?: 0,
                state = state,
                onSelect = { player, serveType ->
                    onPoint(player, serveType)
                    pendingPlayer = null
                },
                onDismiss = { pendingPlayer = null }
            )
        }

        // Sets & games summary — auto-shown after each game/set ends
        AnimatedVisibility(
            visible = showSummary,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SetsGamesScreen(
                state = state,
                gameWinner = lastGameWinner,
                onDismiss = { pendingGameSummary = false }
            )
        }

        // Decider check-in — shown after summary dismisses when sets are level
        AnimatedVisibility(
            visible = !showSummary && deciderPending,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DeciderCheckInScreen(
                currentDec = state.dec,
                onConfirm = onConfirmDecider
            )
        }
    }
}

// ── Sets & Games summary — auto-shown between games/sets ─────────────────────

@Composable
private fun SetsGamesScreen(
    state: MatchState,
    gameWinner: Int = 0,
    onDismiss: () -> Unit
) {
    val totalSeconds = 5
    var secondsLeft by remember { mutableStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        onDismiss()
    }

    val setJustEnded  = state.completedSets.isNotEmpty() &&
                        state.games[0] == 0 && state.games[1] == 0
    val label         = if (setJustEnded) "SET" else "GAME"
    val score0        = if (setJustEnded) state.completedSets.last()[0] else state.games[0]
    val score1        = if (setJustEnded) state.completedSets.last()[1] else state.games[1]
    // Change ends after every odd total of games in the set (1, 3, 5… and always after a TB set)
    val changeEnds    = (score0 + score1) % 2 == 1
    val winnerName    = state.names.getOrElse(gameWinner) { "P${gameWinner + 1}" }.take(3).uppercase()
    val winnerColor   = if (gameWinner == 0) ColorGreen else ColorOrange
    val tied          = score0 == score1
    // Who is actually ahead in the score (not necessarily who just won this game)
    val leaderIdx     = if (score0 > score1) 0 else 1
    val leaderName    = state.names.getOrElse(leaderIdx) { "P${leaderIdx + 1}" }.take(3).uppercase()
    val leaderColor   = if (leaderIdx == 0) ColorGreen else ColorOrange
    val subLabel      = when {
        tied          -> "scores level"
        setJustEnded  -> "$winnerName wins set"
        else          -> "$leaderName leads"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        // Gray countdown ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = 6.dp.toPx()
            val r  = size.minDimension / 2f - sw / 2f
            val cx = size.width  / 2f
            val cy = size.height / 2f
            drawArc(
                color = Color(0xFF2E3A4A),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = sw, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2)
            )
            val progress = secondsLeft.toFloat() / totalSeconds.toFloat()
            if (progress > 0f) {
                drawArc(
                    color = Color(0xFF8B9AB0),
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                    topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2)
                )
            }
        }

        // Content centered on screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "GAME" or "SET" label
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorMuted,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(4.dp))

            // Large white score
            Text(
                text = "$score0 – $score1",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorText,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Leader/tied label
            Text(
                text = subLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    tied         -> ColorSub
                    setJustEnded -> winnerColor
                    else         -> leaderColor
                }
            )

            Spacer(Modifier.height(10.dp))

            // Countdown hint
            Text(
                text = "back in ${secondsLeft}s…",
                fontSize = 10.sp,
                color = Color(0xFF2E3A4A)
            )

            if (changeEnds) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "↔  CHANGE ENDS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorAmber,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerHalf(
    modifier: Modifier,
    player: Int,
    state: MatchState,
    dimColor: Color,
    isUePending: Boolean = false,
    overridePts: String? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val isServing = state.serving == player
    val pts  = overridePts ?: state.displayPts(player)
    val name = state.names.getOrElse(player) { "P${player + 1}" }.take(7)
    val scoreColor = if (player == 0) ColorText else ColorScoreP2

    val latestOnTap by rememberUpdatedState(newValue = onTap)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isUePending) ColorAmberDim else dimColor)
            .then(
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { latestOnTap() },
                        onLongPress = { onLongPress() }
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Serving ball or UE bolt
            Box(Modifier.height(14.dp), contentAlignment = Alignment.Center) {
                when {
                    isUePending -> Text("⚡", fontSize = 11.sp)
                    isServing   -> Box(Modifier.size(8.dp).clip(CircleShape).background(ColorBall))
                }
            }

            // Game score
            Text(
                text = pts,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isUePending) ColorAmber.copy(alpha = 0.9f) else scoreColor,
                textAlign = TextAlign.Center,
                letterSpacing = (-2).sp
            )

            Spacer(Modifier.height(2.dp))

            // Player name
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ColorSub,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun ServeTypeOverlay(
    player: Int,
    state: MatchState,
    onSelect: (Int, ServeType) -> Unit,
    onDismiss: () -> Unit
) {
    val name = state.names.getOrElse(player) { "P${player + 1}" }.take(8).uppercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC080A0D))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF1C2536))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ColorMuted,
                letterSpacing = 0.8.sp
            )

            Button(
                onClick = { onSelect(player, ServeType.SECOND) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1F3A2A))
            ) {
                Text("2nd Serve", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorGreen)
            }

            Button(
                onClick = { onSelect(player, ServeType.DOUBLE_FAULT) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2D1C1C))
            ) {
                Text("Double Fault", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF85149))
            }
        }
    }
}

// ── Decider check-in — shown when sets are level before deciding set ────────

@Composable
private fun DeciderCheckInScreen(
    currentDec: String,
    onConfirm: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentDec) }

    val options = listOf(
        Triple("stb", "Super Tiebreak", "First to 10"),
        Triple("tb",  "Tiebreak",       "At 6-6"),
        Triple("adv", "Advantage",      "No tiebreak")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "SETS LEVEL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ColorMuted,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "Deciding Set",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorText,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(2.dp))

            options.forEach { (key, label, sub) ->
                val isSelected = selected == key
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF1A2E1E) else Color(0xFF161B22))
                        .then(Modifier.pointerInput(key) {
                            detectTapGestures(onTap = { selected = key })
                        }),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ColorGreen else ColorText
                        )
                        Text(
                            text = sub,
                            fontSize = 9.sp,
                            color = if (isSelected) ColorGreen.copy(alpha = 0.5f) else ColorMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = { onConfirm(selected) },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = ColorGreen)
            ) {
                Text(
                    text = "Let's Play",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF080A0D)
                )
            }
        }
    }
}

// ── Ambient mode — minimal, low-power display ──────────────────────────────

@Composable
fun AmbientScoreScreen(state: MatchState) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${state.displayPts(0)}  –  ${state.displayPts(1)}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.statusLine(),
                fontSize = 9.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Match over screen ──────────────────────────────────────────────────────

@Composable
fun MatchOverScreen(state: MatchState, onHome: () -> Unit = {}) {
    val sets = state.setScores
    val winnerIdx = if (sets[0] > sets[1]) 0 else 1
    val winnerName = state.names.getOrElse(winnerIdx) { "P${winnerIdx + 1}" }
    val winnerColor = if (winnerIdx == 0) ColorGreen else ColorOrange

    val setLine = state.completedSets.joinToString("  ") { "${it[0]}-${it[1]}" }

    Box(
        modifier = Modifier.fillMaxSize().background(ColorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "MATCH OVER",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.sp,
                color = ColorMuted
            )

            Text(text = "🏆", fontSize = 28.sp)

            Text(
                text = winnerName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = winnerColor,
                textAlign = TextAlign.Center
            )

            Text(text = "wins", fontSize = 11.sp, color = ColorSub)

            Text(
                text = setLine,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(34.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1C2536))
            ) {
                Text(
                    text = "⌂  Home",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText
                )
            }
        }
    }
}
