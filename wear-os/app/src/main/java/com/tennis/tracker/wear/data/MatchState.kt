package com.tennis.tracker.wear.data

/** Mirrors the JS match state object stored in Firestore stateJson */
data class MatchState(
    val names: List<String> = listOf("Player 1", "Player 2"),
    val pts: List<Int> = listOf(0, 0),
    val games: List<Int> = listOf(0, 0),
    val tbPts: List<Int> = listOf(0, 0),
    val completedSets: List<List<Int>> = emptyList(),
    val serving: Int = 0,
    val mode: String = "normal",
    val totalSets: Int = 3,
    val setsNeeded: Int = 2,
    val dec: String = "stb",   // deciding set format: "stb" | "tb" | "adv"
    val done: Boolean = false,
    val lastServeType: String = "1st",
    val pointNum: Int = 0,
    val pointLog: List<Map<String, Any>> = emptyList()
) {
    val setScores: List<Int> get() {
        val w = intArrayOf(0, 0)
        completedSets.forEach { s -> if (s[0] > s[1]) w[0]++ else w[1]++ }
        return w.toList()
    }

    fun displayPts(p: Int): String {
        if (mode == "tiebreak" || mode == "supertb") return tbPts[p].toString()
        val myPts = pts[p]; val oppPts = pts[1 - p]
        return when {
            myPts == oppPts && myPts >= 3 -> "D"      // deuce (3-3, 4-4, 5-5…)
            myPts > oppPts && myPts >= 4  -> "Ad"     // this player has advantage
            myPts < oppPts && oppPts >= 4 -> "–"      // opponent has advantage
            else -> listOf("0", "15", "30", "40").getOrElse(myPts) { myPts.toString() }
        }
    }

    fun ptsLabel(p: Int): String {
        if (mode == "tiebreak" || mode == "supertb") return tbPts[p].toString()
        val labels = listOf("0", "15", "30", "40", "Ad")
        return labels.getOrElse(pts[p]) { "?" }
    }

    fun scoreLabel(): String {
        val sets = completedSets.joinToString(" ") { "${it[0]}-${it[1]}" }
        val ptsStr = "${ptsLabel(0)}-${ptsLabel(1)}"
        val gamesStr = "G:${games[0]}-${games[1]}"
        return if (sets.isNotEmpty()) "$sets | $ptsStr | $gamesStr" else "$ptsStr | $gamesStr"
    }

    fun ctx(): String = when (mode) {
        "tiebreak" -> "S${completedSets.size + 1} TB"
        "supertb"  -> "Super TB"
        else       -> "S${completedSets.size + 1} G${games[0] + games[1] + 1}"
    }

    fun statusLine(): String = when (mode) {
        "tiebreak" -> "Tiebreak"
        "supertb"  -> "Match Tiebreak"
        else       -> "${setScores[0]}–${setScores[1]} sets · ${games[0]}–${games[1]} games"
    }
}

enum class ServeType { FIRST, SECOND, DOUBLE_FAULT }
