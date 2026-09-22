package com.smartsolar.microgrid.utils

import android.content.Context
import com.smartsolar.microgrid.data.local.UserDao
import com.smartsolar.microgrid.model.User

/**
 * Manages the currently logged-in mobile user session.
 *
 * The actual user/session information is stored in SQLite
 * through UserDao.
 */
class SessionManager(context: Context) {

    private val userDao = UserDao(context)

    /**
     * Returns true when a local user session exists.
     */
    fun isLoggedIn(): Boolean {
        return userDao.getUser() != null &&
                !userDao.getToken().isNullOrBlank()
    }

    /**
     * Returns the currently logged-in user.
     */
    fun getCurrentUser(): User? {
        return userDao.getUser()
    }

    /**
     * Returns the current JWT token.
     */
    fun getToken(): String? {
        return userDao.getToken()
    }

    /**
     * Returns the role of the currently logged-in user.
     */
    fun getUserRole(): String? {
        return userDao.getUser()?.role
    }

    /**
     * Clears the current local session.
     */
    fun logout() {
        userDao.clearUser()
    }
}