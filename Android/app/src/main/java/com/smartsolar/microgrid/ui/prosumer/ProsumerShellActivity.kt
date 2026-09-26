package com.smartsolar.microgrid.ui.prosumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.utils.SessionManager

/**
 * Base activity for every prosumer-facing screen. Provides the
 * shared toolbar with a hamburger button and the sliding left
 * navigation drawer that lists Dashboard, Booking, My
 * Reservations, Profile, and Logout at the bottom.
 *
 * Subclasses just supply their content layout via
 * [contentLayoutRes] and mark which section they represent via
 * [currentSection] so that item is highlighted in the drawer.
 */
abstract class ProsumerShellActivity : AppCompatActivity() {

    enum class NavSection { DASHBOARD, BOOKING, MY_RESERVATIONS, PROFILE }

    @get:LayoutRes
    protected abstract val contentLayoutRes: Int

    protected abstract val currentSection: NavSection

    /** Text shown as the toolbar title. */
    @get:StringRes
    protected open val toolbarTitleRes: Int = R.string.app_name

    protected lateinit var drawerLayout: DrawerLayout
        private set

    protected lateinit var toolbar: Toolbar
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prosumer_shell)

        // Inflate the concrete screen into the frame.
        val frame = findViewById<FrameLayout>(R.id.contentFrame)
        layoutInflater.inflate(contentLayoutRes, frame, true)

        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open, R.string.drawer_close,
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val user = SessionManager(applicationContext).getCurrentUser()
        findViewById<TextView>(R.id.tvDrawerUser).text =
            user?.name ?: "Prosumer"

        wireDrawerItem(
            R.id.navDashboard, R.string.nav_dashboard,
            R.drawable.ic_nav_dashboard,
            NavSection.DASHBOARD,
        ) { navigateTo(ProsumerDashboardActivity::class.java) }

        wireDrawerItem(
            R.id.navBooking, R.string.nav_booking,
            R.drawable.ic_nav_book,
            NavSection.BOOKING,
        ) { navigateTo(BookingActivity::class.java) }

        wireDrawerItem(
            R.id.navMyReservations, R.string.nav_my_reservations,
            R.drawable.ic_nav_list,
            NavSection.MY_RESERVATIONS,
        ) { navigateTo(MyReservationsActivity::class.java) }

        wireDrawerItem(
            R.id.navProfile, R.string.nav_profile,
            R.drawable.ic_nav_person,
            NavSection.PROFILE,
        ) { navigateTo(ProsumerProfileActivity::class.java) }

        wireDrawerItem(
            R.id.navLogout, R.string.nav_logout,
            R.drawable.ic_nav_logout,
            null,
        ) { logout() }
    }

    private fun wireDrawerItem(
        rootId: Int,
        @StringRes labelRes: Int,
        @DrawableRes iconRes: Int,
        section: NavSection?,
        onClick: () -> Unit,
    ) {
        val root = findViewById<View>(rootId)
        root.findViewById<TextView>(R.id.navLabel).setText(labelRes)
        root.findViewById<ImageView>(R.id.navIcon)
            .setImageResource(iconRes)
        root.isSelected = section != null && section == currentSection
        root.setOnClickListener {
            drawerLayout.closeDrawers()
            onClick()
        }
    }

    private fun navigateTo(target: Class<*>) {
        if (this::class.java == target) return
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun logout() {
        SessionManager(applicationContext).logout()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK,
                ),
        )
        finish()
    }
}
