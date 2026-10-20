package com.tennis.tracker.wear.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

// Web client ID from Firebase Console → Authentication → Sign-in method → Google → Web SDK config
private const val WEB_CLIENT_ID = "413242858926-0uaft3ns83g3reemrf4k0grt05i1vtlh.apps.googleusercontent.com"

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun silentSignIn(context: Context): FirebaseUser? {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return try {
            val account: GoogleSignInAccount = GoogleSignIn
                .getClient(context, gso)
                .silentSignIn()
                .await()
            firebaseSignIn(account)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun handleSignInResult(data: Intent?): FirebaseUser? {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            firebaseSignIn(account)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun firebaseSignIn(account: GoogleSignInAccount): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return try {
            auth.signInWithCredential(credential).await().user
        } catch (e: Exception) {
            null
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }
}
