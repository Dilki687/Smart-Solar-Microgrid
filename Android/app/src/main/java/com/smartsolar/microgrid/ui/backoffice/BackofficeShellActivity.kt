package com.smartsolar.microgrid.ui.backoffice

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
 * Base activity for every backoffice / grid-operator screen.
 * Provides the shared toolbar with a hamburger button and the
 * sliding left drawer that lists every management section
 * (Dashboard, Users, Prosumers, Deactivation, Slots, Stations,
 * Reservations) with Logout pinned at the bottom.
 */
abstract class BackofficeShellActivity : AppCompatActivity() {

    enum class NavSection {
        DASHBOARD,
        USERS,
        PROSUMERS,
        DEACTIVATION,
        SLOTS,
        STATIONS,
        RESERVATIONS,
    }

    @get:LayoutRes
    protected abstract val contentLayoutRes: Int

    protected abstract val currentSection: NavSection

    protected lateinit var drawerLayout: DrawerLayout
        private set

    protected lateinit var toolbar: Toolbar
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backoffice_shell)

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
        findViewById<TextView>(R.id.tvDrawerUser).text = when {
            user == null -> "Backoffice"
            user.role == "GRID_OPERATOR" -> "${user.name} · Grid Operator"
            user.role == "BACKOFFICE" -> "${user.name} · Backoffice"
            else -> user.name
        }

        wire(
            R.id.navDashboard, R.string.nav_dashboard,
            R.drawable.ic_nav_dashboard, NavSection.DASHBOARD,
        ) { go(BackofficeDashboardActivity::class.java) }

        wire(
            R.id.navUsers, R.string.nav_users,
            R.drawable.ic_nav_person, NavSection.USERS,
        ) { go(UserManagementActivity::class.java) }

        wire(
            R.id.navProsumers, R.string.nav_prosumers_manage,
            R.drawable.ic_nav_group, NavSection.PROSUMERS,
        ) { go(ProsumerLookupActivity::class.java) }

        wire(
            R.id.navDeactivation, R.string.nav_deactivation,
            R.drawable.ic_nav_warning, NavSection.DEACTIVATION,
        ) { go(DeactivationRequestsActivity::class.java) }

        wire(
            R.id.navSlots, R.string.nav_slots,
            R.drawable.ic_nav_book, NavSection.SLOTS,
        ) { go(BookingSlotManagementActivity::class.java) }

        wire(
            R.id.navStations, R.string.nav_stations,
            R.drawable.ic_nav_station, NavSection.STATIONS,
        ) { go(StationManagementActivity::class.java) }

        wire(
            R.id.navReservations, R.string.nav_reservations,
            R.drawable.ic_nav_list, NavSection.RESERVATIONS,
        ) {
            go(
                com.smartsolar.microgrid.ui.operator.OperatorReservationsActivity::class.java,
            )
        }

        wire(
            R.id.navLogout, R.string.nav_logout,
            R.drawable.ic_nav_logout, null,
        ) { logout() }
    }

    private fun wire(
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

    private fun go(target: Class<*>) {
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
