@file:Suppress("DEPRECATION")
package com.tennis.tracker.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.ambient.AmbientModeSupport
import com.tennis.tracker.wear.data.ServeType
import com.tennis.tracker.wear.presentation.AuthState
import com.tennis.tracker.wear.presentation.MatchIdScreen
import com.tennis.tracker.wear.presentation.MatchViewModel
import com.tennis.tracker.wear.presentation.ScoreScreen
import com.tennis.tracker.wear.presentation.SignInScreen

@Suppress("DEPRECATION")
class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    companion object {
        const val ACTION_MATCH_RECEIVED = "com.tennis.tracker.wear.MATCH_RECEIVED"
        const val EXTRA_MATCH_ID        = "matchId"
        private const val PREF_KEY_MATCH = "last_match_id"
    }

    private val viewModel: MatchViewModel by viewModels()

    @Suppress("DEPRECATION")
    private lateinit var vibrator: Vibrator

    private var isAmbient by mutableStateOf(false)

    @Suppress("DEPRECATION")
    override fun getAmbientCallback() = object : AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) { isAmbient = true }
        override fun onExitAmbient() { isAmbient = false }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModelScope {
            val user = viewModel.authRepo.handleSignInResult(result.data)
            if (user != null) {
                // Re-run initAuth so autoConnect wires up the Firestore listener
                viewModel.initAuth(this@MainActivity)
            }
        }
    }

    private val matchReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            intent.getStringExtra(EXTRA_MATCH_ID)?.let { id ->
                getPreferences(Context.MODE_PRIVATE).edit().putString(PREF_KEY_MATCH, id).apply()
                viewModel.connectMatch(id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        AmbientModeSupport.attach(this)

        @Suppress("DEPRECATION")
        vibrator = getSystemService(Vibrator::class.java)

        registerReceiver(matchReceiver, IntentFilter(ACTION_MATCH_RECEIVED), RECEIVER_NOT_EXPORTED)

        viewModel.initAuth(this)

        setContent {
            val authState      by viewModel.authState.collectAsStateWithLifecycle()
            val matchId        by viewModel.matchId.collectAsStateWithLifecycle()
            val state          by viewModel.state.collectAsStateWithLifecycle()
            val saving         by viewModel.isSaving.collectAsStateWithLifecycle()
            val canUndo        by viewModel.canUndo.collectAsStateWithLifecycle()
            val deciderPending   by viewModel.deciderPending.collectAsStateWithLifecycle()
            val uePending        by viewModel.uePending.collectAsStateWithLifecycle()
            val uePendingPlayer  by viewModel.uePendingPlayer.collectAsStateWithLifecycle()
            val frozenDisplayPts by viewModel.frozenDisplayPts.collectAsStateWithLifecycle()

            when (authState) {
                is AuthState.Loading -> {
                    SignInScreen(isLoading = true, onSignIn = {})
                }
                is AuthState.SignedOut -> {
                    SignInScreen(isLoading = false, onSignIn = {
                        signInLauncher.launch(viewModel.authRepo.getSignInIntent(this@MainActivity))
                    })
                }
                is AuthState.Connecting -> {
                    MatchIdScreen(
                        isConnecting = true,
                        onConnect = { id -> viewModel.connectMatch(id) }
                    )
                }
                is AuthState.SignedIn -> {
                    if (matchId != null && state == null) {
                        // Match ID known but Firestore hasn't streamed state yet — show spinner
                        SignInScreen(isLoading = true, onSignIn = {})
                    } else if (matchId != null && state != null) {
                        ScoreScreen(
                            state = state!!,
                            isSaving = saving,
                            isAmbient = isAmbient,
                            matchId = matchId,
                            canUndo = canUndo,
                            deciderPending = deciderPending,
                            uePending = uePending,
                            uePendingPlayer = uePendingPlayer,
                            frozenDisplayPts = frozenDisplayPts,
                            onPoint = { player, serveType ->
                                hapticPoint(serveType)
                                viewModel.awardPoint(player, serveType)
                            },
                            onUndo = { viewModel.undoPoint() },
                            onHome = {
                                getPreferences(Context.MODE_PRIVATE).edit()
                                    .remove(PREF_KEY_MATCH).apply()
                                viewModel.resetMatch()
                            },
                            onConfirmDecider = { dec -> viewModel.confirmDecider(dec) },
                            onTagUE = { viewModel.tagUE() },
                            onDismissUE = { viewModel.dismissUE() }
                        )
                    } else {
                        MatchIdScreen(
                            isConnecting = false,
                            onConnect = { id -> viewModel.connectMatch(id) }
                        )
                    }
                }
            }
        }
    }

    private fun viewModelScope(block: suspend () -> Unit) {
        lifecycleScope.launch { block() }
    }

    @Suppress("DEPRECATION")
    private fun hapticPoint(serveType: ServeType) {
        val pattern = when (serveType) {
            ServeType.FIRST        -> longArrayOf(0, 40)
            ServeType.SECOND       -> longArrayOf(0, 40, 60, 40)
            ServeType.DOUBLE_FAULT -> longArrayOf(0, 80, 80, 80)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(matchReceiver)
    }
}
