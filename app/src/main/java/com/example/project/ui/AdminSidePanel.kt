package com.example.project.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.project.R

class AdminSidePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        // "Nadmuchujemy" layout do tego widoku
        LayoutInflater.from(context).inflate(R.layout.activity_admin_side_panel, this, true)
        setupSidePanel()
    }

    private fun setupSidePanel() {
        // Inicjalizacja komponentów panelu administratora, np. ustawianie listenerów itp.
    }
}
