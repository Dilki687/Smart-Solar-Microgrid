package com.smartsolar.microgrid.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database helper for the Smart Solar Microgrid Android app.
 *
 * The database stores locally required user/session information.
 *
 * MongoDB is NOT accessed directly by this class.
 * All server-side data is accessed through the C# Web API.
 */
class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {

        /*
         * Creates the local users table.
         *
         * Only information needed by the mobile application is
         * stored locally.
         */
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NIC TEXT NOT NULL,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_TOKEN TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        /*
         * For this initial version, recreate the table when the
         * database schema version changes.
         *
         * Later versions can use proper migration logic.
         */
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    companion object {

        // Database name.
        private const val DATABASE_NAME = "smart_solar_microgrid.db"

        // Current database version.
        private const val DATABASE_VERSION = 1

        // Users table.
        const val TABLE_USERS = "users"

        // User columns.
        const val COLUMN_ID = "id"
        const val COLUMN_NIC = "nic"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_ROLE = "role"
        const val COLUMN_STATUS = "status"
        const val COLUMN_TOKEN = "token"
    }
}