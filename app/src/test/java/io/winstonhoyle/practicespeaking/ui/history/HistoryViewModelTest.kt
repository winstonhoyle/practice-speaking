package io.winstonhoyle.practicespeaking.ui.history

import io.winstonhoyle.practicespeaking.TestApp
import io.winstonhoyle.practicespeaking.await
import io.winstonhoyle.practicespeaking.error.StorageError
import io.winstonhoyle.practicespeaking.ui.session.SessionDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A broken database must show an error on the read-only screens, not crash them. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    private lateinit var app: TestApp

    @Before
    fun setUp() {
        // Real time: the search debounce would wait forever on a test dispatcher's virtual clock.
        Dispatchers.setMain(Dispatchers.Unconfined)
        app = TestApp()
    }

    @After
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    /** Room keeps an in-memory database usable after close(), so drop the tables to make every query fail. */
    private fun breakDatabase() {
        val db = app.db.openHelper.writableDatabase
        listOf("messages_fts", "messages", "sessions").forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
    }

    @Test
    fun aHealthyDatabaseHasNoError() {
        val state = HistoryViewModel(app.sessions).state.await { !it.loading }
        assertNull(state.error)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun aFailedReadShowsABannerThatCanBeDismissed() {
        breakDatabase()
        val vm = HistoryViewModel(app.sessions)

        val state = vm.state.await { it.error != null }
        assertTrue(state.error is StorageError.DatabaseFailed)
        assertEquals(emptyList<Any>(), state.sessions)

        vm.dismissError()
        assertNull(vm.state.await { it.error == null }.error)
    }

    @Test
    fun aFailedDeleteShowsABanner() {
        val vm = HistoryViewModel(app.sessions)
        vm.state.await { !it.loading }
        breakDatabase()

        vm.delete(1)

        assertTrue(vm.state.await { it.error != null }.error is StorageError.DatabaseFailed)
    }

    @Test
    fun sessionDetailShowsAFailedRead() {
        breakDatabase()
        val state =
            SessionDetailViewModel(app.sessions, 1, app.settings, app.tutorModels, app.translator, app.speaker).state.await {
                it.error !=
                    null
            }
        assertTrue(state.error is StorageError.DatabaseFailed)
    }
}
