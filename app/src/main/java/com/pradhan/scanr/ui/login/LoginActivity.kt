package com.pradhan.scanr.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.pradhan.scanr.databinding.ActivityLoginBinding
import com.pradhan.scanr.ui.homepage.HomepageActivity

private const val clientID = "1099469054905-7melk7nk30l3db50ami6p822qq0aeqaf.apps.googleusercontent.com"
private const val GOOGLE_SIGN_IN = 1
private const val EXTRA_GOOGLE_SIGNIN_IMAGE = "EXTRA_GOOGLE_SIGNIN_IMAGE"
private const val EXTRA_GOOGLE_SIGNIN_FNAME = "EXTRA_GOOGLE_SIGNIN_FNAME"
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSigninButton: Button
    private lateinit var guestSigninButton: Button
    private lateinit var gso : GoogleSignInOptions
    private lateinit var mGoogleSignInClient : GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(clientID)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            handleSuccessfulSignIn(account, true)
            return
        }
        Toast.makeText(applicationContext,"Sign-in",Toast.LENGTH_LONG).show()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        googleSigninButton = binding.loginGoogleButton
        guestSigninButton = binding.loginGuestButton
        setContentView(binding.root)
        guestSigninButton.setOnClickListener{
            val intent = Intent(this@LoginActivity,HomepageActivity::class.java)
            startActivity(intent)
            finish()
            Toast.makeText(applicationContext,"Guest Sign-in",Toast.LENGTH_SHORT).show()
        }
        googleSigninButton.setOnClickListener{
            Toast.makeText(applicationContext,"Google Sign-in",Toast.LENGTH_SHORT).show()
            googleSignIn()
        }
    }

    private fun googleSignIn(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            handleSuccessfulSignIn(account, false)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TODO: REMOVE", "signInResult:failed code=" + e.statusCode)
            //updateUI(null)
        }
    }

    private fun handleSuccessfulSignIn(account: GoogleSignInAccount, isAutoLogIn: Boolean) {
        val intent = Intent(this@LoginActivity,HomepageActivity::class.java)
        intent.putExtra(EXTRA_GOOGLE_SIGNIN_IMAGE, account.photoUrl)
        intent.putExtra(EXTRA_GOOGLE_SIGNIN_FNAME, account.givenName)
        // Signed in successfully, show authenticated UI.
        var toastString = ""
        toastString = if (isAutoLogIn){
            "Auto-signing in using ${account.email}"
        } else {
            "Successfully logged in ${account.email}!"
        }
        Toast.makeText(applicationContext,toastString,Toast.LENGTH_SHORT).show()
        startActivity(intent)
        finish()
    }


}