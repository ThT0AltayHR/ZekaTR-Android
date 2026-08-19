package com.muhammed.zekatr

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * GERCEK Google Sign-In entegrasyonu (sahte/mockup degil).
 *
 * ONEMLI - bunu calistirmadan once mutlaka yap:
 *  1) Google Cloud Console'da bu paket adi (com.muhammed.zekatr) ve UYGULAMANI
 *     IMZALAYACAGIN keystore'un SHA-1 parmak izi ile eslesen bir "Android" tipi
 *     OAuth 2.0 Client ID olustur.
 *  2) O ekranda uretilen "Web application" tipi client ID'yi (Android client ID
 *     DEGIL, Web client ID) asagidaki strings.xml icindeki
 *     default_web_client_id degerine yapistir.
 *  3) SHA-1 eslesmezse ya da client ID yanlissa girisin basarisiz olur;
 *     bunu ben senin adina dogrulayamam cunku imzalama anahtarina erisimim yok.
 *
 * Sohbette bana yapistirdigin ID'yi default_web_client_id olarak strings.xml'e
 * ekledim; ANCAK bunun gecerli/senin projene ait oldugunu benim tarafimdan
 * dogrulamam mumkun degil - lutfen Google Cloud Console'dan kendi projenle
 * eslestigini teyit et.
 */
object GoogleAuthHelper {

    fun buildClient(activity: Activity): GoogleSignInClient {
        val webClientId = activity.getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun signInIntent(activity: Activity): Intent = buildClient(activity).signInIntent

    /** onActivityResult icinde cagir. Basarili olursa hesabi, olmazsa null dondurur. */
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            null
        }
    }

    fun currentAccount(activity: Activity): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)

    fun signOut(activity: Activity, onDone: () -> Unit) {
        buildClient(activity).signOut().addOnCompleteListener { onDone() }
    }
}
