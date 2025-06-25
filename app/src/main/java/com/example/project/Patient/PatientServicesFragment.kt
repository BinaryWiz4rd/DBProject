package com.example.project.Patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.databinding.FragmentPatientServicesBinding

/**
 * Fragment displaying available services for patients.
 * Handles navigation to browse services and supports pre-selecting a doctor.
 */
class PatientServicesFragment : Fragment() {
    
    private var _binding: FragmentPatientServicesBinding? = null
    private val binding get() = _binding!!
    
    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState The saved instance state bundle.
     * @return The root view for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called when the fragment's view has been created.
     * Sets up UI state and navigates if a doctor was pre-selected.
     *
     * @param view The view returned by onCreateView.
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        
        // Check if a specific doctor was pre-selected
        val selectedDoctorId = arguments?.getString("selected_doctor_id")
        if (selectedDoctorId != null) {
            // Navigate directly to browse services with the pre-selected doctor
            navigateToBrowseServicesWithDoctor(selectedDoctorId)
        }
    }
    
    private fun setupUI() {
        // Navigate to the existing PatientBrowseServicesFragment
        binding.browseServicesButton.setOnClickListener {
            navigateToBrowseServices()
        }
    }
    
    private fun navigateToBrowseServices() {
        val fragment = PatientBrowseServicesFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToBrowseServicesWithDoctor(doctorId: String) {
        val fragment = PatientBrowseServicesFragment()
        val bundle = Bundle()
        bundle.putString("selected_doctor_id", doctorId)
        fragment.arguments = bundle
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    /**
     * Cleans up the binding when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
