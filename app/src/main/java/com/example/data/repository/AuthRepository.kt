package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_auth_prefs")

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val loginType: String, // "GOOGLE", "EMAIL", "GUEST"
    val isLoggedIn: Boolean
)

class AuthRepository(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_LOGIN_TYPE = stringPreferencesKey("login_type")
        private val KEY_PHOTO_URL = stringPreferencesKey("photo_url")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false
        val userId = prefs[KEY_USER_ID] ?: "guest_123"
        val userName = prefs[KEY_USER_NAME] ?: "Guest User"
        val userEmail = prefs[KEY_USER_EMAIL] ?: "guest@faizulai.app"
        val loginType = prefs[KEY_LOGIN_TYPE] ?: "GUEST"
        val photoUrl = prefs[KEY_PHOTO_URL]

        UserProfile(
            id = userId,
            name = userName,
            email = userEmail,
            photoUrl = photoUrl,
            loginType = loginType,
            isLoggedIn = isLoggedIn
        )
    }

    suspend fun loginAsGuest() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_ID] = "guest_${System.currentTimeMillis()}"
            prefs[KEY_USER_NAME] = "Faizul AI Guest"
            prefs[KEY_USER_EMAIL] = "guest@faizulai.app"
            prefs[KEY_LOGIN_TYPE] = "GUEST"
            prefs[KEY_PHOTO_URL] = ""
        }
    }

    suspend fun loginWithEmail(email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_ID] = "user_${email.hashCode()}"
            prefs[KEY_USER_NAME] = name.ifBlank { email.substringBefore("@") }
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_LOGIN_TYPE] = "EMAIL"
            prefs[KEY_PHOTO_URL] = ""
        }
    }

    suspend fun loginWithGoogle(email: String, name: String, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_ID] = "google_${email.hashCode()}"
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_LOGIN_TYPE] = "GOOGLE"
            prefs[KEY_PHOTO_URL] = photoUrl ?: ""
        }
    }

    suspend fun updateProfile(name: String, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            if (photoUrl != null) prefs[KEY_PHOTO_URL] = photoUrl
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_USER_NAME] = "Guest User"
            prefs[KEY_USER_EMAIL] = "guest@faizulai.app"
            prefs[KEY_LOGIN_TYPE] = "GUEST"
        }
    }
}
