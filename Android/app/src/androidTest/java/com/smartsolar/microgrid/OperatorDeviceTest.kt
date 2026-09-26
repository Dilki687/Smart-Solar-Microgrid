package com.smartsolar.microgrid

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.journeyapps.barcodescanner.CaptureActivity
import com.smartsolar.microgrid.data.local.UserDao
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.ui.operator.OperatorDashboardActivity
import com.smartsolar.microgrid.ui.prosumer.ReservationQrActivity
import com.smartsolar.microgrid.utils.SessionManager
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the disposable HTTP fixture and isolated -PoperatorCheck=true app ID only. */
@RunWith(AndroidJUnit4::class)
class OperatorDeviceTest {
    @Test fun concurrentSessionReadsDoNotCloseEachOthersDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(context.packageName.endsWith(".operatorcheck"))
        val dao = UserDao(context)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(8)
        try {
            val reads = (1..80).map { java.util.concurrent.Callable { dao.getToken(); dao.getUser() } }
            executor.invokeAll(reads).forEach { it.get() }
        } finally { executor.shutdownNow() }
    }

    @Test fun loginDashboardVerifyCompleteLogout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(context.packageName.endsWith(".operatorcheck") && args.containsKey("password"))
        UserDao(context).clearUser()
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.etIdentifier)).perform(replaceText("OP1"))
        onView(withId(R.id.etPassword)).perform(replaceText(args.getString("password")!!), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        awaitText(R.id.tvOperatorName, "Test OP1")
        assertEquals("GRID_OPERATOR", SessionManager(context).getUserRole())
        onView(withId(R.id.tvCompletedCount)).check(matches(withText(containsString("Completed:"))))

        onView(withId(R.id.btnAvailability)).perform(click())
        awaitText(R.id.tvAvailabilityMessage, "Reserved/consumed")
        onView(withId(R.id.etOperatorCapacity)).perform(replaceText("130"), closeSoftKeyboard())
        onView(withId(R.id.btnSaveAvailability)).perform(click())
        awaitText(R.id.tvAvailabilityMessage, "Available: 80")
        androidx.test.espresso.Espresso.pressBack()
        awaitText(R.id.tvOperatorName, "Test OP1")

        // Permission is exercised on the actual device; denied access stays in the app.
        onView(withId(R.id.btnScan)).perform(click())
        val denied = denyCameraIfShown(instrumentation)
        assertTrue("Start with a fresh isolated test install so camera denial can be exercised", denied)
        awaitText(R.id.tvTransferMessage, "Camera permission is needed")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
        ).use { it.readBytes() }
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(android.Manifest.permission.CAMERA))

        // Open the real camera once, then return without an optical scan.
        val cameraMonitor = instrumentation.addMonitor(CaptureActivity::class.java.name, null, false)
        onView(withId(R.id.btnScanAgain)).perform(scrollTo(), click())
        val camera = instrumentation.waitForMonitorWithTimeout(cameraMonitor, 5000)
        assertNotNull("Native camera Activity opens after permission grant", camera)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(750)
        instrumentation.runOnMainSync { camera.finish() }
        instrumentation.removeMonitor(cameraMonitor)
        awaitText(R.id.tvTransferMessage, "Scanning cancelled")

        // Supply the camera Activity's decoded result; optical decoding is covered by the QR round-trip test.
        scanResult(instrumentation, "TRX:" + "F".repeat(64)) {
            onView(withId(R.id.btnScanAgain)).perform(scrollTo(), click())
            awaitText(R.id.tvTransferMessage, "not found")
        }
        scanResult(instrumentation, args.getString("qrToken")!!) {
            onView(withId(R.id.btnScanAgain)).perform(scrollTo(), click())
            awaitText(R.id.tvTransferMessage, "QR Verified")
        }
        onView(withId(R.id.tvTransferDetails)).check(matches(withText(containsString("Test P1"))))
        onView(withId(R.id.btnCompleteTransfer)).perform(scrollTo(), click())
        onView(withText(R.string.operator_confirm)).perform(click())
        awaitText(R.id.tvTransferMessage, "COMPLETED")
        onView(withId(R.id.btnCompleteTransfer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        scanResult(instrumentation, args.getString("qrToken")!!) {
            onView(withId(R.id.btnScanAgain)).perform(scrollTo(), click())
            awaitText(R.id.tvTransferMessage, "Already completed")
        }
        onView(withId(R.id.btnTransferDashboard)).perform(scrollTo(), click())
        awaitText(R.id.tvOperatorName, "Test OP1")
        onView(withId(R.id.btnOperatorLogout)).perform(click())
        awaitView(R.id.btnLogin)
        assertFalse(SessionManager(context).isLoggedIn())
        // Directly reopening a protected Activity after logout must send us back to login.
        ActivityScenario.launch(OperatorDashboardActivity::class.java)
        awaitView(R.id.btnLogin)
        assertFalse(SessionManager(context).isLoggedIn())
    }

    @Test fun prosumerDisplaysServerIssuedQr() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(context.packageName.endsWith(".operatorcheck") && args.containsKey("displayReservationId"))
        UserDao(context).clearUser()
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.etIdentifier)).perform(replaceText("P1"))
        onView(withId(R.id.etPassword)).perform(replaceText(args.getString("password")!!), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        awaitCheck { assertEquals("PROSUMER", SessionManager(context).getUserRole()) }
        // Wait for the existing post-login router before opening the QR screen.
        SystemClock.sleep(600)
        ActivityScenario.launch<ReservationQrActivity>(Intent(context, ReservationQrActivity::class.java)
            .putExtra("reservationId", args.getString("displayReservationId")))
        awaitText(R.id.tvQrMessage, "Present this QR")
        onView(withId(R.id.imgReservationQr)).check { view, error ->
            if (error != null) throw error
            assertNotNull((view as android.widget.ImageView).drawable)
        }
        onView(withId(R.id.btnRefreshQr)).perform(scrollTo(), click())
        awaitText(R.id.tvQrMessage, "Present this QR")
    }

    private fun scanResult(instrumentation: Instrumentation, token: String, block: () -> Unit) {
        val result = Intent().putExtra("SCAN_RESULT", token).putExtra("SCAN_RESULT_FORMAT", "QR_CODE")
        val monitor = instrumentation.addMonitor(CaptureActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_OK, result), true)
        try { block(); assertTrue(monitor.hits > 0) } finally { instrumentation.removeMonitor(monitor) }
    }

    private fun denyCameraIfShown(instrumentation: Instrumentation): Boolean {
        repeat(40) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val deny = root?.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_deny_button")?.firstOrNull()
            if (deny != null) { deny.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }
            SystemClock.sleep(100)
        }
        return false
    }

    private fun awaitText(id: Int, text: String) = awaitCheck { onView(withId(id)).check(matches(withText(containsString(text)))) }
    private fun awaitView(id: Int) = awaitCheck { onView(withId(id)).check(matches(isDisplayed())) }
    private fun awaitCheck(check: () -> Unit) {
        var failure: Throwable? = null
        repeat(100) {
            try { check(); return } catch (error: AssertionError) { failure = error }
            catch (error: androidx.test.espresso.NoMatchingViewException) { failure = error }
            SystemClock.sleep(100)
        }
        throw AssertionError("UI did not reach expected state", failure)
    }
}
