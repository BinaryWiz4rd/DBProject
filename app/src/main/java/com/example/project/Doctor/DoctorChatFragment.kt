package com.example.project.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.project.R

/**
 * A [Fragment] for displaying the doctor's chat interface.
 * This is currently a placeholder and does not contain any logic.
 */
class DoctorChatFragment : Fragment() {

    /**
     * Inflates the layout for this fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_chat, container, false)
    }
}