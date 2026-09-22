package com.smartsolar.microgrid.ui.prosumer

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.UserRepository
import com.smartsolar.microgrid.utils.SessionManager
import com.smartsolar.microgrid.model.UpdateProsumerRequest
import kotlinx.coroutines.launch

/**
 * Allows an authenticated Prosumer to:
 *
 * - View their profile
 * - Edit their profile
 * - Request account deactivation
 */
class ProsumerProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var userRepository: UserRepository

    private lateinit var tvStatus: TextView
    private lateinit var tvNic: TextView
    private lateinit var tvCreatedAt: TextView

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText

    private lateinit var btnSave: Button
    private lateinit var btnDeactivate: Button
    private lateinit var progressBar: ProgressBar

    private var prosumerNic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prosumer_profile)

        // Initialize dependencies.
        sessionManager =
            SessionManager(applicationContext)

        userRepository =
            UserRepository()

        initializeViews()

        /*
         * Retrieve NIC from the locally authenticated user.
         */
        val currentUser =
            sessionManager.getCurrentUser()

        if (currentUser == null ||
            currentUser.role != "PROSUMER"
        ) {

            finish()

            return
        }

        prosumerNic = currentUser.nic

        // Load latest profile from the server.
        loadProfile()

        // Save button.
        btnSave.setOnClickListener {
            updateProfile()
        }

        // Deactivation request.
        btnDeactivate.setOnClickListener {
            showDeactivationConfirmation()
        }
    }

    /**
     * Initializes XML views.
     */
    private fun initializeViews() {

        tvStatus =
            findViewById(R.id.tvStatus)

        tvNic =
            findViewById(R.id.tvNic)

        tvCreatedAt =
            findViewById(R.id.tvCreatedAt)

        etName =
            findViewById(R.id.etName)

        etEmail =
            findViewById(R.id.etEmail)

        etPhone =
            findViewById(R.id.etPhone)

        etAddress =
            findViewById(R.id.etAddress)

        btnSave =
            findViewById(R.id.btnSave)

        btnDeactivate =
            findViewById(R.id.btnDeactivate)

        progressBar =
            findViewById(R.id.progressBar)
    }

    /**
     * Retrieves the latest Prosumer information from the API.
     */
    private fun loadProfile() {

        val nic = prosumerNic ?: return

        setLoading(true)

        lifecycleScope.launch {

            val result =
                userRepository.getProsumer(nic)

            setLoading(false)

            result.onSuccess { prosumer ->

                tvStatus.text =
                    "Status: ${formatStatus(prosumer.status)}"

                tvNic.text =
                    "NIC: ${prosumer.nic}"

                tvCreatedAt.text =
                    "Created: ${prosumer.createdAt ?: "Not available"}"

                etName.setText(prosumer.name)
                etEmail.setText(prosumer.email)
                etPhone.setText(prosumer.phone)
                etAddress.setText(prosumer.address)

                /*
                 * Account status determines which actions
                 * are available.
                 */
                updateActionsForStatus(
                    prosumer.status
                )
            }

            result.onFailure { exception ->

                Toast.makeText(
                    this@ProsumerProfileActivity,
                    exception.message
                        ?: "Unable to load profile.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sends updated profile information to the API.
     */
    private fun updateProfile() {

        val nic = prosumerNic ?: return

        // Read values.
        val name =
            etName.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        val phone =
            etPhone.text.toString().trim()

        val address =
            etAddress.text.toString().trim()

        // Basic validation.
        if (name.isEmpty()) {
            etName.error = "Enter your name"
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Enter your email"
            return
        }

        if (phone.isEmpty()) {
            etPhone.error = "Enter your phone number"
            return
        }

        if (address.isEmpty()) {
            etAddress.error = "Enter your address"
            return
        }

        val request =
            UpdateProsumerRequest(
                name = name,
                email = email,
                phone = phone,
                address = address
            )

        setLoading(true)

        lifecycleScope.launch {

            val result =
                userRepository.updateProsumer(
                    nic,
                    request
                )

            setLoading(false)

            result.onSuccess { response ->

                Toast.makeText(
                    this@ProsumerProfileActivity,
                    response.message,
                    Toast.LENGTH_SHORT
                ).show()

                // Reload the server's latest values.
                loadProfile()
            }

            result.onFailure { exception ->

                Toast.makeText(
                    this@ProsumerProfileActivity,
                    exception.message
                        ?: "Profile update failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Displays a confirmation dialog before requesting
     * account deactivation.
     */
    private fun showDeactivationConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Request Account Deactivation")
            .setMessage(
                "Are you sure you want to request " +
                        "deactivation of your account?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Request"
            ) { _, _ ->

                requestDeactivation()
            }
            .show()
    }

    /**
     * Sends the deactivation request to the server.
     */
    private fun requestDeactivation() {

        val nic = prosumerNic ?: return

        setLoading(true)

        lifecycleScope.launch {

            val result =
                userRepository.requestDeactivation(nic)

            setLoading(false)

            result.onSuccess { response ->

                Toast.makeText(
                    this@ProsumerProfileActivity,
                    response.message,
                    Toast.LENGTH_LONG
                ).show()

                // Refresh status from the server.
                loadProfile()
            }

            result.onFailure { exception ->

                Toast.makeText(
                    this@ProsumerProfileActivity,
                    exception.message
                        ?: "Deactivation request failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Controls profile actions according to account status.
     */
    private fun updateActionsForStatus(
        status: String
    ) {

        when (status) {

            "ACTIVE" -> {

                etName.isEnabled = true
                etEmail.isEnabled = true
                etPhone.isEnabled = true
                etAddress.isEnabled = true

                btnSave.isEnabled = true
                btnDeactivate.isEnabled = true

                btnDeactivate.text =
                    "REQUEST DEACTIVATION"
            }

            "PENDING_DEACTIVATION" -> {

                etName.isEnabled = false
                etEmail.isEnabled = false
                etPhone.isEnabled = false
                etAddress.isEnabled = false

                btnSave.isEnabled = false
                btnDeactivate.isEnabled = false

                btnDeactivate.text =
                    "DEACTIVATION REQUEST PENDING"
            }

            "INACTIVE" -> {

                etName.isEnabled = false
                etEmail.isEnabled = false
                etPhone.isEnabled = false
                etAddress.isEnabled = false

                btnSave.isEnabled = false
                btnDeactivate.isEnabled = false

                btnDeactivate.text =
                    "ACCOUNT INACTIVE"
            }
        }
    }

    /**
     * Enables/disables the profile loading state.
     */
    private fun setLoading(
        loading: Boolean
    ) {

        progressBar.visibility =
            if (loading)
                View.VISIBLE
            else
                View.GONE

        btnSave.isEnabled = !loading
    }

    /**
     * Converts API status values into readable text.
     */
    private fun formatStatus(
        status: String
    ): String {

        return when (status) {

            "ACTIVE" ->
                "Active"

            "PENDING_DEACTIVATION" ->
                "Deactivation Request Pending"

            "INACTIVE" ->
                "Inactive"

            else ->
                status
        }
    }
}