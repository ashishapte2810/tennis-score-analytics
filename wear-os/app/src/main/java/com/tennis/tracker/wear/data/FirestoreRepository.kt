package com.tennis.tracker.wear.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    fun watchMatch(matchId: String): Flow<MatchState?> = callbackFlow {
        val ref = db.collection("matches").document(matchId)
        val reg: ListenerRegistration = ref.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener  // ignore errors, keep last state
            val json = snap.getString("stateJson") ?: return@addSnapshotListener
            try {
                trySend(parseState(json))
            } catch (e: Exception) {
                // parse failure — keep last good state, don't go to home screen
            }
        }
        awaitClose { reg.remove() }
    }

    fun watchActiveMatch(uid: String): Flow<String?> = callbackFlow {
        val ref = db.collection("users").document(uid)
        val reg: ListenerRegistration = ref.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(snap.getString("activeMatchId"))
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveState(matchId: String, state: MatchState, deviceId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        db.collection("matches").document(matchId).set(
            mapOf(
                "stateJson" to serializeState(state),
                "updatedBy" to deviceId,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "ownerId"   to uid
            )
        ).await()
    }

    private fun parseState(json: String): MatchState {
        val o = JSONObject(json)

        fun intList(key: String) = o.getJSONArray(key).let { a ->
            (0 until a.length()).map { a.getInt(it) }
        }
        fun strList(key: String) = o.getJSONArray(key).let { a ->
            (0 until a.length()).map { a.getString(it) }
        }
        fun intMatrix(key: String) = o.getJSONArray(key).let { outer ->
            (0 until outer.length()).map { i ->
                val inner = outer.getJSONArray(i)
                (0 until inner.length()).map { j -> inner.getInt(j) }
            }
        }

        // Parse pointLog as raw maps — preserve all fields the phone wrote
        val pointLog = mutableListOf<Map<String, Any>>()
        if (o.has("pointLog")) {
            val arr = o.getJSONArray("pointLog")
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val entry = mutableMapOf<String, Any>()
                val keys = e.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    entry[k] = e.get(k)
                }
                pointLog.add(entry)
            }
        }

        return MatchState(
            names         = strList("names"),
            pts           = intList("pts"),
            games         = intList("games"),
            tbPts         = if (o.has("tbPts")) intList("tbPts") else listOf(0, 0),
            completedSets = if (o.has("completedSets")) intMatrix("completedSets") else emptyList(),
            serving       = o.optInt("serving", 0),
            mode          = o.optString("mode", "normal"),
            totalSets     = o.optInt("totalSets", 3),
            setsNeeded    = o.optInt("setsNeeded", 2),
            dec           = o.optString("dec", "stb"),
            done          = o.optBoolean("done", false),
            pointNum      = o.optInt("pointNum", 0),
            pointLog      = pointLog
        )
    }

    private fun serializeState(s: MatchState): String {
        val o = JSONObject()
        o.put("names",         JSONArray(s.names))
        o.put("pts",           JSONArray(s.pts))
        o.put("games",         JSONArray(s.games))
        o.put("tbPts",         JSONArray(s.tbPts))
        o.put("serving",       s.serving)
        o.put("mode",          s.mode)
        o.put("totalSets",     s.totalSets)
        o.put("setsNeeded",    s.setsNeeded)
        o.put("dec",           s.dec)
        o.put("done",          s.done)
        o.put("pointNum",      s.pointNum)

        val sets = JSONArray()
        s.completedSets.forEach { set -> sets.put(JSONArray(set)) }
        o.put("completedSets", sets)

        // Serialize pointLog preserving all fields exactly as the phone wrote them
        val log = JSONArray()
        s.pointLog.forEach { entry ->
            val e = JSONObject()
            entry.forEach { (k, v) -> e.put(k, v) }
            log.put(e)
        }
        o.put("pointLog", log)

        return o.toString()
    }
}
