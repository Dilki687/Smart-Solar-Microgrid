package com.smartsolar.microgrid.ui.backoffice

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.SystemUserRepository
import com.smartsolar.microgrid.model.CreateUserRequest
import com.smartsolar.microgrid.model.UpdateUserRequest
import com.smartsolar.microgrid.model.User
import kotlinx.coroutines.launch

/**
 * List BACKOFFICE / GRID_OPERATOR accounts. FAB opens a create
 * dialog. Tap a row for an edit / deactivate action sheet.
 */
class UserManagementActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_user_management
    override val currentSection = NavSection.USERS

    private val repo = SystemUserRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvList)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = UserAdapter { user -> showActions(user) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe.setOnRefreshListener { load() }
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showCreateDialog()
        }

        load()
    }

    private fun load() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            val result = repo.list()
            swipe.isRefreshing = false
            result.onSuccess { list ->
                adapter.submit(list)
                tvEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                adapter.submit(emptyList())
                tvEmpty.text = it.message
                    ?: getString(R.string.empty_users)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showActions(user: User) {
        AlertDialog.Builder(this)
            .setTitle(user.name)
            .setItems(arrayOf("Edit", "Deactivate", "Close")) { _, which ->
                when (which) {
                    0 -> showEditDialog(user)
                    1 -> confirmDeactivate(user)
                }
            }
            .show()
    }

    private fun confirmDeactivate(user: User) {
        AlertDialog.Builder(this)
            .setMessage("Deactivate account for ${user.name}?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    repo.deactivate(user.id).fold(
                        onSuccess = { load() },
                        onFailure = { showError(it.message) },
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateDialog() {
        val form = UserForm(this, includePassword = true)
        AlertDialog.Builder(this)
            .setTitle(R.string.qa_manage_users)
            .setView(form.root)
            .setPositiveButton(R.string.action_create, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val values = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            repo.create(
                                CreateUserRequest(
                                    nic = values.nic,
                                    name = values.name,
                                    email = values.email,
                                    phone = values.phone,
                                    address = values.address,
                                    password = values.password.orEmpty(),
                                    role = values.role,
                                ),
                            ).fold(
                                onSuccess = { dismiss(); load() },
                                onFailure = { showError(it.message) },
                            )
                        }
                    }
                }
            }.show()
    }

    private fun showEditDialog(user: User) {
        val form = UserForm(this, includePassword = false).apply {
            fill(
                nic = user.nic,
                name = user.name,
                email = user.email,
                phone = "",
                address = "",
                role = user.role,
                readOnlyNic = true,
            )
        }
        AlertDialog.Builder(this)
            .setTitle("Edit ${user.name}")
            .setView(form.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val values = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            repo.update(
                                userId = user.id,
                                request = UpdateUserRequest(
                                    name = values.name,
                                    email = values.email,
                                    phone = values.phone,
                                    address = values.address,
                                    role = values.role,
                                ),
                            ).fold(
                                onSuccess = { dismiss(); load() },
                                onFailure = { showError(it.message) },
                            )
                        }
                    }
                }
            }.show()
    }

    private fun showError(msg: String?) {
        AlertDialog.Builder(this)
            .setMessage(msg ?: "Something went wrong.")
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    // -------- adapter --------

    private class UserAdapter(
        private val onClick: (User) -> Unit,
    ) : RecyclerView.Adapter<UserAdapter.VH>() {

        private val items = mutableListOf<User>()

        fun submit(list: List<User>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_user, parent, false),
            )

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvName = v.findViewById<TextView>(R.id.tvName)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val tvRole = v.findViewById<TextView>(R.id.tvRole)
            private val tvStatus = v.findViewById<TextView>(R.id.tvStatus)

            fun bind(u: User) {
                tvName.text = u.name
                tvMeta.text = "${u.nic}\n${u.email}"
                tvRole.text = if (u.role == "GRID_OPERATOR") "Grid Op" else u.role
                tvStatus.text = u.status
                tvStatus.setBackgroundResource(
                    when (u.status) {
                        "ACTIVE" -> R.drawable.bg_badge_success
                        "PENDING_DEACTIVATION" -> R.drawable.bg_badge_pending
                        else -> R.drawable.bg_badge_neutral
                    },
                )
                itemView.setOnClickListener { onClick(u) }
            }
        }
    }
}

// ---------------------------------------------------------------
// Shared inline form used for both create and edit dialogs.
// ---------------------------------------------------------------

private class UserForm(
    ctx: android.content.Context,
    includePassword: Boolean,
) {
    val root: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ctx, 20), dp(ctx, 4), dp(ctx, 20), dp(ctx, 4))
    }

    private val nic = field(ctx, "NIC")
    private val name = field(ctx, "Name")
    private val email = field(ctx, "Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
    private val phone = field(ctx, "Phone", InputType.TYPE_CLASS_PHONE)
    private val address = field(ctx, "Address")
    private val password =
        if (includePassword)
            field(ctx, "Password (min 8)", InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT)
        else null

    private val roleGroup = RadioGroup(ctx).apply {
        orientation = RadioGroup.HORIZONTAL
        val bo = RadioButton(ctx).apply { text = "BACKOFFICE"; id = View.generateViewId() }
        val go = RadioButton(ctx).apply { text = "GRID_OPERATOR"; id = View.generateViewId() }
        addView(bo); addView(go); check(bo.id)
    }

    init {
        listOfNotNull(nic, name, email, phone, address, password).forEach {
            root.addView(it.layout)
        }
        val roleLabel = TextView(ctx).apply {
            text = "Role"; setPadding(0, dp(ctx, 8), 0, dp(ctx, 4))
        }
        root.addView(roleLabel); root.addView(roleGroup)
    }

    fun fill(
        nic: String, name: String, email: String,
        phone: String, address: String, role: String,
        readOnlyNic: Boolean = false,
    ) {
        this.nic.edit.setText(nic); this.nic.edit.isEnabled = !readOnlyNic
        this.name.edit.setText(name)
        this.email.edit.setText(email)
        this.phone.edit.setText(phone)
        this.address.edit.setText(address)
        val idx = if (role == "GRID_OPERATOR") 1 else 0
        (roleGroup.getChildAt(idx) as RadioButton).isChecked = true
    }

    data class Values(
        val nic: String, val name: String, val email: String,
        val phone: String, val address: String,
        val password: String?, val role: String,
    )

    fun read(): Values? {
        val nicV = nic.edit.text.toString().trim()
        val nameV = name.edit.text.toString().trim()
        val emailV = email.edit.text.toString().trim()
        val phoneV = phone.edit.text.toString().trim()
        val addressV = address.edit.text.toString().trim()
        val passV = password?.edit?.text?.toString()
        if (nicV.isEmpty() || nameV.isEmpty() || emailV.isEmpty()) return null
        val roleV = if (roleGroup.checkedRadioButtonId ==
            (roleGroup.getChildAt(1) as RadioButton).id
        ) "GRID_OPERATOR" else "BACKOFFICE"
        return Values(nicV, nameV, emailV, phoneV, addressV, passV, roleV)
    }

    private data class Field(val layout: TextInputLayout, val edit: TextInputEditText)

    companion object {
        private fun dp(ctx: android.content.Context, v: Int): Int =
            (v * ctx.resources.displayMetrics.density).toInt()

        private fun field(
            ctx: android.content.Context, hint: String,
            inputType: Int = InputType.TYPE_CLASS_TEXT,
        ): Field {
            val til = TextInputLayout(ctx).apply {
                this.hint = hint
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(ctx, 6)
            }
            val et = TextInputEditText(til.context).apply {
                this.inputType = inputType
            }
            til.addView(et)
            return Field(til, et)
        }
    }
}
