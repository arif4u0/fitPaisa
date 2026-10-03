package com.fitpaisa.app.core.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin session gate used by navigation on cold start. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    suspend fun isSignedIn(): Boolean = authRepository.isSignedIn()

    suspend fun isWelcomeAccepted(): Boolean = sessionStore.isWelcomeAccepted()

    suspend fun setWelcomeAccepted() = sessionStore.setWelcomeAccepted()
}
