package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleSignInMgr"

data class GoogleSignedInProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUri: String
)

sealed interface GoogleSignInOutcome {
    data class Success(val profile: GoogleSignedInProfile) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class Failed(val message: String) : GoogleSignInOutcome
}

class GoogleAccountSignInManager(private val appContext: Context) {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(appContext)
    }

    fun currentSignedInProfile(): GoogleSignedInProfile? {
        val user = Firebase.auth.currentUser ?: return null
        val email = user.email.orEmpty().trim()
        if (email.isBlank() && user.displayName.isNullOrBlank()) return null
        return GoogleSignedInProfile(
            uid = user.uid,
            email = email,
            displayName = user.displayName.orEmpty().trim(),
            photoUri = user.photoUrl?.toString().orEmpty()
        )
    }

    suspend fun attemptSilentSignIn(): GoogleSignedInProfile? {
        currentSignedInProfile()?.let { return it }
        val clientId = resolveWebClientId() ?: return null
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(appContext, request)
            handleIdTokenCredential(result.credential)
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google auto sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): GoogleSignInOutcome {
        val clientId = resolveWebClientId()
            ?: return GoogleSignInOutcome.Failed(
                "Google Sign-In configuration missing: default_web_client_id not found."
            )

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val uiContext = (activityContext as? Activity) ?: activityContext
            val result = CredentialManager.create(uiContext).getCredential(uiContext, request)
            val profile = handleIdTokenCredential(result.credential)
            if (profile != null) {
                GoogleSignInOutcome.Success(profile)
            } else {
                GoogleSignInOutcome.Failed("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleSignInOutcome.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleSignInOutcome.Failed(
                e.localizedMessage ?: "Google Sign-In failed. Please verify Play Services & try again."
            )
        }
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
            }
        }
    }

    private suspend fun handleIdTokenCredential(credential: androidx.credentials.Credential): GoogleSignedInProfile? {
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return null
        }
        val idTokenCred = GoogleIdTokenCredential.createFrom(credential.data)
        val authCredential = GoogleAuthProvider.getCredential(idTokenCred.idToken, null)
        val authResult = Firebase.auth.signInWithCredential(authCredential).await()
        val fbUser = authResult.user
        val email = idTokenCred.id.ifBlank { fbUser?.email.orEmpty() }.trim()
        val name = idTokenCred.displayName.orEmpty().ifBlank { fbUser?.displayName.orEmpty() }.trim()
        val photo = idTokenCred.profilePictureUri?.toString()
            ?: fbUser?.photoUrl?.toString().orEmpty()

        return GoogleSignedInProfile(
            uid = fbUser?.uid.orEmpty(),
            email = email,
            displayName = name,
            photoUri = photo
        )
    }

    private fun resolveWebClientId(): String? = runCatching {
        appContext.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    }.getOrNull()
}
