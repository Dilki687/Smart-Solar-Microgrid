package com.smartsolar.microgrid.ui.operator

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.utils.SessionManager

class OperatorProfileActivity : OperatorBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_profile)

        val user = SessionManager(applicationContext).getCurrentUser() ?: return
        findViewById<TextView>(R.id.tvProfileName).text = user.name
        findViewById<TextView>(R.id.tvProfileNic).text = user.nic
        findViewById<TextView>(R.id.tvProfileEmail).text = user.email
        findViewById<TextView>(R.id.tvProfileRole).text = user.role
        findViewById<MaterialButton>(R.id.btnProfileLogout).setOnClickListener {
            it.isEnabled = false
            logout()
        }
        findViewById<MaterialButton>(R.id.btnProfileBack).setOnClickListener { finish() }
    }
}