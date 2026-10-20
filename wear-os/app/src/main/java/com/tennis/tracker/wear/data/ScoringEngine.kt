package com.tennis.tracker.wear.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure scoring logic — mirrors the JS engine in tennis_tracker.html.
 * pointLog entries match the phone's exact format so LOG + STATS tabs render correctly.
 */
object ScoringEngine {

    fun awardPoint(state: MatchState, winner: Int, serveType: ServeType): MatchState {
        if (state.done) return state

        // Double fault: always awards point to opponent of server
        val pointWinner = if (serveType == ServeType.DOUBLE_FAULT) 1 - state.serving else winner

        val nextPointNum = state.pointNum + 1
        val setNumBefore  = state.completedSets.size + 1
        val gameNumBefore = state.games[0] + state.games[1] + 1
        val scoreBefore   = state.scoreLabel()
        val ctxBefore     = state.ctx()
        val serverName    = state.names.getOrElse(state.serving) { "P${state.serving + 1}" }
        val winnerName    = state.names.getOrElse(pointWinner) { "P${pointWinner + 1}" }
        val serveLabel    = when (serveType) {
            ServeType.FIRST        -> "1st"
            ServeType.SECOND       -> "2nd"
            ServeType.DOUBLE_FAULT -> "Double Fault"
        }
        val modeLabel = when (state.mode) {
            "normal" -> "regular"
            else     -> state.mode
        }

        val next = when (state.mode) {
            "tiebreak", "supertb" -> scoreTiebreakPoint(state, pointWinner)
            else -> scoreNormalPoint(state, pointWinner)
        }.copy(lastServeType = serveType.label(), pointNum = nextPointNum)

        // Build log entry in the exact format the phone's renderLog + computeStats expect
        val entry: Map<String, Any> = mapOf(
            "point"     to nextPointNum,
            "winner"    to winnerName,
            "winnerIdx" to pointWinner,
            "server"    to serverName,
            "set"       to setNumBefore,
            "gameNum"   to gameNumBefore,
            "ctx"       to ctxBefore,
            "score"     to scoreBefore,
            "mode"      to modeLabel,
            "serve"     to serveLabel,
            "ts"        to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        )

        return next.copy(pointLog = state.pointLog + entry)
    }

    private fun scoreNormalPoint(state: MatchState, winner: Int): MatchState {
        val loser = 1 - winner
        val myPts = state.pts[winner]
        val oppPts = state.pts[loser]

        if (myPts == 3 && oppPts == 3) {
            // no-ad: decisive point at deuce — win the game immediately
            if (state.noAd) return winGame(state, winner)
            val newPts = state.pts.toMutableList(); newPts[winner] = 4
            return state.copy(pts = newPts)
        }
        if (myPts == 4) return winGame(state, winner)
        if (oppPts == 4) {
            val newPts = state.pts.toMutableList(); newPts[loser] = 3
            return state.copy(pts = newPts)
        }
        if (myPts == 3) return winGame(state, winner)

        val newPts = state.pts.toMutableList(); newPts[winner] = myPts + 1
        return state.copy(pts = newPts)
    }

    private fun winGame(state: MatchState, winner: Int): MatchState {
        val newGames = state.games.toMutableList()
        newGames[winner]++
        val newServing = 1 - state.serving
        val g0 = newGames[0]; val g1 = newGames[1]

        val setWinner = when {
            (g0 >= 6 && g0 - g1 >= 2) || g0 == 7 -> 0
            (g1 >= 6 && g1 - g0 >= 2) || g1 == 7 -> 1
            else -> -1
        }

        if (setWinner >= 0) {
            val newSets = state.completedSets + listOf(newGames.toList())
            val setsWon = IntArray(2)
            newSets.forEach { s -> if (s[0] > s[1]) setsWon[0]++ else setsWon[1]++ }
            val matchDone = setsWon[0] >= state.setsNeeded || setsWon[1] >= state.setsNeeded
            return state.copy(
                pts = listOf(0, 0), games = listOf(0, 0), tbPts = listOf(0, 0),
                completedSets = newSets, serving = newServing, mode = "normal", done = matchDone
            )
        }

        val isDeciding = state.completedSets.size == state.totalSets - 1
        val newMode = if (g0 == 6 && g1 == 6) {
            when {
                isDeciding && state.dec == "stb" -> "supertb"   // 10-point super tiebreak
                isDeciding && state.dec == "adv" -> "normal"    // advantage set — keep playing
                else                             -> "tiebreak"  // regular 7-point tiebreak
            }
        } else "normal"

        return state.copy(pts = listOf(0, 0), games = newGames, serving = newServing, mode = newMode)
    }

    private fun scoreTiebreakPoint(state: MatchState, winner: Int): MatchState {
        val newTb = state.tbPts.toMutableList()
        newTb[winner]++
        val target = if (state.mode == "supertb") 10 else 7
        val w0 = newTb[0]; val w1 = newTb[1]

        val totalPoints = w0 + w1
        val newServing = if (totalPoints == 1 || (totalPoints > 1 && (totalPoints - 1) % 2 == 0))
            1 - state.serving else state.serving

        val tbWinner = when {
            w0 >= target && w0 - w1 >= 2 -> 0
            w1 >= target && w1 - w0 >= 2 -> 1
            else -> -1
        }

        if (tbWinner >= 0) {
            val finalGames = state.games.toMutableList()
            finalGames[tbWinner]++
            val newSets = state.completedSets + listOf(finalGames.toList())
            val setsWon = IntArray(2)
            newSets.forEach { s -> if (s[0] > s[1]) setsWon[0]++ else setsWon[1]++ }
            val matchDone = setsWon[0] >= state.setsNeeded || setsWon[1] >= state.setsNeeded
            return state.copy(
                pts = listOf(0, 0), games = listOf(0, 0), tbPts = listOf(0, 0),
                completedSets = newSets, serving = newServing, mode = "normal", done = matchDone
            )
        }

        return state.copy(tbPts = newTb, serving = newServing)
    }

    private fun ServeType.label() = when (this) {
        ServeType.FIRST        -> "1st"
        ServeType.SECOND       -> "2nd"
        ServeType.DOUBLE_FAULT -> "df"
    }
}
