package com.example.project.presenter

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.project.R
import com.google.android.material.navigation.NavigationView

class AdminSidePanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {

    private var isAdmin: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.admin_side_panel, this, true)

        val openButton = findViewById<ImageButton>(R.id.openSidePanelButton)
        val sidePanel = findViewById<NavigationView>(R.id.sidePanel)

        // Open the side panel when clicking the button
        openButton.setOnClickListener {
            openDrawer(GravityCompat.END)
        }
    }

    fun showIfAdmin(isAdmin: Boolean) {
        this.isAdmin = isAdmin
        visibility = if (isAdmin) View.VISIBLE else View.GONE
    }
}