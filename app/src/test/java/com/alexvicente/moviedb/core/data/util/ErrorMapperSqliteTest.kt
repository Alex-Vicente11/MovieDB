package com.alexvicente.moviedb.core.data.util

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.alexvicente.moviedb.core.util.Constants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SQLiteException vive en android.jar: en JVM puro sus constructores lanzan "Stub!".
// application = Application::class evita MovieDbApplication (Crashlytics falla bajo Robolectric).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ErrorMapperSqliteTest {

    @Test
    fun whenSQLiteException_mapsToDatabase() {
        assertThat(ErrorMapper.map(SQLiteException("disk I/O error")))
            .isEqualTo(AppError.Database(Constants.ERROR_DATABASE))
    }

    @Test
    fun whenSQLiteSubclass_mapsToDatabase() {
        assertThat(ErrorMapper.map(SQLiteConstraintException("UNIQUE constraint failed")))
            .isEqualTo(AppError.Database(Constants.ERROR_DATABASE))
    }
}