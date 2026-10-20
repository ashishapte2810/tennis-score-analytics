package com.tennis.tracker.wear.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.firebase.auth.FirebaseUser
import com.tennis.tracker.wear.complication.PREF_DETAIL
import com.tennis.tracker.wear.complication.PREF_FILE
import com.tennis.tracker.wear.complication.PREF_MATCH_ID
import com.tennis.tracker.wear.complication.PREF_SCORE
import com.tennis.tracker.wear.complication.TennisComplicationService
import com.tennis.tracker.wear.data.AuthRepository
import com.tennis.tracker.wear.data.FirestoreRepository
import com.tennis.tracker.wear.data.MatchState
import com.tennis.tracker.wear.data.ScoringEngine
import com.tennis.tracker.wear.data.ServeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MatchViewModel : ViewModel() {

    private val repo   = FirestoreRepository()
    val authRepo       = AuthRepository()
    private val deviceId = "wear-" + UUID.randomUUID().toString().take(8)
    private var appContext: Context? = null

    private val _user           = MutableStateFlow<FirebaseUser?>(null)
    private val _matchId        = MutableStateFlow<String?>(null)
    private val _state          = MutableStateFlow<MatchState?>(null)
    private val _isSaving       = MutableStateFlow(false)
    private val _toast          = MutableStateFlow<String?>(null)
    private val _authState      = MutableStateFlow<AuthState>(AuthState.Loading)
    private val _history        = mutableListOf<MatchState>()
    private val _canUndo        = MutableStateFlow(false)
    private val _deciderPending   = MutableStateFlow(false)
    private val _uePending        = MutableStateFlow(false)
    private val _uePendingPlayer  = MutableStateFlow(0)
    private val _frozenDisplayPts = MutableStateFlow<List<String>?>(null)

    val user:           StateFlow<FirebaseUser?>  = _user.asStateFlow()
    val matchId:        StateFlow<String?>        = _matchId.asStateFlow()
    val state:          StateFlow<MatchState?>    = _state.asStateFlow()
    val isSaving:       StateFlow<Boolean>        = _isSaving.asStateFlow()
    val toast:          StateFlow<String?>        = _toast.asStateFlow()
    val authState:      StateFlow<AuthState>      = _authState.asStateFlow()
    val canUndo:          StateFlow<Boolean>           = _canUndo.asStateFlow()
    val deciderPending:   StateFlow<Boolean>           = _deciderPending.asStateFlow()
    val uePending:        StateFlow<Boolean>           = _uePending.asStateFlow()
    val uePendingPlayer:  StateFlow<Int>               = _uePendingPlayer.asStateFlow()
    val frozenDisplayPts: StateFlow<List<String>?>     = _frozenDisplayPts.asStateFlow()

    private fun updateComplication(state: MatchState?) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        if (state == null || state.done) {
            prefs.edit().putString(PREF_SCORE, "🎾").putString(PREF_DETAIL, "No match").apply()
        } else {
            val score  = "${state.games[0]}–${state.games[1]}"
            val detail = "${state.displayPts(0)}–${state.displayPts(1)}"
            prefs.edit().putString(PREF_SCORE, score).putString(PREF_DETAIL, detail).apply()
        }
        ComplicationDataSourceUpdateRequester
            .create(ctx, android.content.ComponentName(ctx, TennisComplicationService::class.java))
            .requestUpdateAll()
    }

    fun initAuth(context: Context) {
        appContext = context.applicationContext
        val existing = authRepo.currentUser
        if (existing != null) {
            _user.value = existing
            _authState.value = AuthState.SignedIn(existing)
            autoConnect(existing.uid)
        } else {
            viewModelScope.launch {
                val user = authRepo.silentSignIn(context)
                if (user != null) {
                    _user.value = user
                    _authState.value = AuthState.SignedIn(user)
                    autoConnect(user.uid)
                } else {
                    _authState.value = AuthState.SignedOut
                }
            }
        }
    }

    private fun autoConnect(uid: String) {
        // Fast-path: if we have a cached match ID from a previous session, connect immediately
        // without showing the Connecting screen — Firestore will stream state in the background
        val ctx = appContext
        val cachedId = ctx?.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            ?.getString(PREF_MATCH_ID, null)
        if (cachedId != null) {
            connectMatch(cachedId)
        } else {
            _authState.value = AuthState.Connecting
        }
        viewModelScope.launch {
            repo.watchActiveMatch(uid).collect { activeMatchId ->
                if (activeMatchId != null && _matchId.value != activeMatchId) {
                    connectMatch(activeMatchId)
                } else if (activeMatchId == null && _matchId.value == null) {
                    _authState.value = AuthState.SignedIn(_user.value!!)
                }
            }
        }
    }

    fun connectMatch(id: String) {
        _matchId.value = id
        _authState.value = AuthState.SignedIn(_user.value ?: return)
        appContext?.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            ?.edit()?.putString(PREF_MATCH_ID, id)?.apply()
        viewModelScope.launch {
            repo.watchMatch(id).collect { incoming ->
                if (incoming != null) _state.value = incoming
            }
        }
    }

    fun awardPoint(player: Int, serveType: ServeType) {
        val current = _state.value ?: return
        val id      = _matchId.value ?: return
        if (current.done || _deciderPending.value) return

        _history.add(current)
        _canUndo.value = true

        // Capture display pts BEFORE the state update so frozen display never flickers to "0"
        val prevPts = listOf(current.displayPts(0), current.displayPts(1))

        val next = ScoringEngine.awardPoint(current, player, serveType)
        _state.value = next
        updateComplication(next)

        // Show UE window for non-DF points on the winning side
        if (!next.done && !_deciderPending.value && serveType != ServeType.DOUBLE_FAULT) {
            _frozenDisplayPts.value = prevPts
            _uePendingPlayer.value = player
            _uePending.value = true
        }

        // Show check-in when the deciding set just started
        if (!next.done &&
            next.completedSets.size > current.completedSets.size &&
            next.completedSets.size == next.totalSets - 1) {
            _frozenDisplayPts.value = null
            _uePending.value = false
            _deciderPending.value = true
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repo.saveState(id, next, deviceId)
            } catch (e: Exception) {
                _toast.value = "Sync failed — point saved locally"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun tagUE() {
        val current = _state.value ?: return
        val id      = _matchId.value ?: return
        _uePending.value = false
        _frozenDisplayPts.value = null
        val log = current.pointLog.toMutableList()
        if (log.isEmpty()) return
        val last = log.last().toMutableMap()
        last["ue"] = true
        log[log.size - 1] = last
        val updated = current.copy(pointLog = log)
        _state.value = updated
        viewModelScope.launch {
            try { repo.saveState(id, updated, deviceId) } catch (_: Exception) {}
        }
    }

    fun dismissUE() {
        _uePending.value = false
        _frozenDisplayPts.value = null
    }

    fun confirmDecider(dec: String) {
        val current = _state.value ?: return
        val id      = _matchId.value ?: return
        // "stb" = entire deciding set is a 10-point super tiebreak — start immediately
        // "tb"/"adv" = normal games first; tiebreak fires at 6-6 via ScoringEngine
        val updated = if (dec == "stb") {
            current.copy(dec = dec, mode = "supertb", tbPts = listOf(0, 0))
        } else {
            current.copy(dec = dec, mode = "normal")
        }
        _state.value = updated
        _deciderPending.value = false
        viewModelScope.launch {
            try { repo.saveState(id, updated, deviceId) } catch (_: Exception) {}
        }
    }

    fun undoPoint() {
        _uePending.value = false
        _frozenDisplayPts.value = null
        val prev = _history.removeLastOrNull() ?: return
        val id   = _matchId.value ?: return
        _canUndo.value = _history.isNotEmpty()
        _state.value = prev
        updateComplication(prev)

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repo.saveState(id, prev, deviceId)
            } catch (e: Exception) {
                _toast.value = "Sync failed — undo saved locally"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearToast() { _toast.value = null }

    fun resetMatch() {
        _matchId.value = null
        _state.value = null
        _history.clear()
        _canUndo.value = false
        _deciderPending.value = false
        _uePending.value = false
        _frozenDisplayPts.value = null
        updateComplication(null)
        appContext?.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            ?.edit()?.remove(PREF_MATCH_ID)?.apply()
        val u = _user.value
        if (u != null) _authState.value = AuthState.SignedIn(u)
    }

    fun signOut(context: Context) {
        authRepo.signOut(context)
        _user.value = null
        _matchId.value = null
        _state.value = null
        _authState.value = AuthState.SignedOut
    }
}

sealed class AuthState {
    object Loading    : AuthState()
    object SignedOut  : AuthState()
    object Connecting : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
}
