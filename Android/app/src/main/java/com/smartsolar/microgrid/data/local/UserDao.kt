package com.smartsolar.microgrid.data.local

import android.content.ContentValues
import android.content.Context
import com.smartsolar.microgrid.model.User

/**
 * Handles local CRUD operations for the logged-in user.
 */
class UserDao(context: Context) {

    // SQLite database helper.
    private val databaseHelper = AppDatabaseHelper(context)

    /**
     * Saves the currently authenticated user and JWT token locally.
     */
    fun saveUser(user: User, token: String) {

        // Open database for writing.
        val database = databaseHelper.writableDatabase

        // Prepare user data.
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_ID, user.id)
            put(AppDatabaseHelper.COLUMN_NIC, user.nic)
            put(AppDatabaseHelper.COLUMN_NAME, user.name)
            put(AppDatabaseHelper.COLUMN_EMAIL, user.email)
            put(AppDatabaseHelper.COLUMN_ROLE, user.role)
            put(AppDatabaseHelper.COLUMN_STATUS, user.status)
            put(AppDatabaseHelper.COLUMN_TOKEN, token)
        }

        /*
         * Replace any previous session because only one mobile
         * session is required for this application.
         */
        database.insertWithOnConflict(
            AppDatabaseHelper.TABLE_USERS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        database.close()
    }

    /**
     * Returns the locally stored user.
     */
    fun getUser(): User? {

        val database = databaseHelper.readableDatabase

        val cursor = database.query(
            AppDatabaseHelper.TABLE_USERS,
            null,
            null,
            null,
            null,
            null,
            null,
            "1"
        )

        val user = if (cursor.moveToFirst()) {

            User(
                id = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_ID
                    )
                ),
                nic = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_NIC
                    )
                ),
                name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_NAME
                    )
                ),
                email = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_EMAIL
                    )
                ),
                role = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_ROLE
                    )
                ),
                status = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        AppDatabaseHelper.COLUMN_STATUS
                    )
                )
            )

        } else {
            null
        }

        cursor.close()
        database.close()

        return user
    }

    /**
     * Returns the locally stored JWT token.
     */
    fun getToken(): String? {

        val database = databaseHelper.readableDatabase

        val cursor = database.query(
            AppDatabaseHelper.TABLE_USERS,
            arrayOf(AppDatabaseHelper.COLUMN_TOKEN),
            null,
            null,
            null,
            null,
            null,
            "1"
        )

        val token = if (cursor.moveToFirst()) {
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    AppDatabaseHelper.COLUMN_TOKEN
                )
            )
        } else {
            null
        }

        cursor.close()
        database.close()

        return token
    }

    /**
     * Removes the locally stored user session.
     */
    fun clearUser() {

        val database = databaseHelper.writableDatabase

        database.delete(
            AppDatabaseHelper.TABLE_USERS,
            null,
            null
        )

        database.close()
    }
}