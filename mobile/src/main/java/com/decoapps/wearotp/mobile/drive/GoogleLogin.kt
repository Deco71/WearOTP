package com.decoapps.wearotp.mobile.drive

import android.content.Context
import android.credentials.GetCredentialException
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64


//Based from https://codelabs.developers.google.com/sign-in-with-google-android#5

private const val TAG = "GoogleLogin"

suspend fun login(webClientId: String, context: Context, authorizationLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>): String? {
    // Create a Google ID option with filtering by authorized accounts enabled.
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setNonce(generateSecureRandomNonce())
        .build()

    // Create a credential request with the Google ID option.
    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    // Attempt to sign in with the created request using an authorized account
    signIn(request, context)

    // Request the Drive AppData scope to access the hidden app data folder in the user's Drive
    val requestedScopes = listOf(Scope(DriveScopes.DRIVE_APPDATA))
    val authorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(requestedScopes)
        .build()

    return try {
        val result = Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .await()

        if (result.hasResolution()) {
            val intentSenderRequest = IntentSenderRequest.Builder(
                result.pendingIntent!!.intentSender
            ).build()
            authorizationLauncher.launch(intentSenderRequest)
            null
        } else {
            Log.d("DriveAuth", "Authorization successful, no additional permissions needed.")
            result.accessToken
        }
    } catch (e: Exception) {
        Log.e("DriveAuth", "Failed: ${e.message}")
        null
    }
}

suspend fun signIn(request: GetCredentialRequest, context: Context) {
    val credentialManager = CredentialManager.create(context)
    val failureMessage = "Sign in failed!"
    delay(250)
    try {
        val credential = credentialManager.getCredential(
            request = request,
            context = context,
        ).credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "(☞ﾟヮﾟ)☞  Sign in Successful!  ☜(ﾟヮﾟ☜)")
        } else {
            Log.e(TAG, "Unexpected type of credential")
        }

    } catch (e: GetCredentialException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        Log.e(TAG, failureMessage + ": Failure getting credentials", e)

    } catch (e: GoogleIdTokenParsingException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        Log.e(TAG, failureMessage + ": Issue with parsing received GoogleIdToken", e)

    } catch (e: NoCredentialException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        Log.e(TAG, failureMessage + ": No credentials found", e)

    } catch (e: GetCredentialCustomException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        Log.e(TAG, failureMessage + ": Issue with custom credential request", e)

    } catch (e: GetCredentialCancellationException) {
        Toast.makeText(context, ": Sign-in cancelled", Toast.LENGTH_SHORT).show()
        Log.e(TAG, failureMessage + ": Sign-in was cancelled", e)
    }
}

//This function is used to generate a secure nonce to pass in with our request
fun generateSecureRandomNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom.getInstanceStrong().nextBytes(randomBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
}
