package com.example.project.Patient

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.databinding.FragmentPatientDoctorsBinding
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth

class PatientDoctorsFragment : Fragment() {
    
    private var _binding: FragmentPatientDoctorsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()
        
        setupUI()
        loadAllDoctors()
    }
    
    private fun setupUI() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchDoctors(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadAllDoctors()
                } else if (newText.length > 2) {
                    searchDoctors(newText)
                }
                return true
            }
        })
    }
      private fun loadAllDoctors() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewDoctors.visibility = View.GONE
        
        firestoreHelper.getAllDoctors { doctors ->
            activity?.runOnUiThread {
                binding.progressBar.visibility = View.GONE
                
                if (doctors.isNotEmpty()) {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.recyclerViewDoctors.visibility = View.VISIBLE
                    
                    binding.recyclerViewDoctors.layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerViewDoctors.adapter = DoctorListAdapter(doctors) { doctor ->
                        // Navigate to doctor details or booking
                        navigateToDoctorDetails(doctor.uid)
                    }
                } else {
                    showEmptyState("No doctors found")
                }
            }
        }
    }
    
    private fun searchDoctors(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewDoctors.visibility = View.GONE
        
        firestoreHelper.searchDoctors(query) { doctors ->
            activity?.runOnUiThread {
                binding.progressBar.visibility = View.GONE
                
                if (doctors.isNotEmpty()) {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.recyclerViewDoctors.visibility = View.VISIBLE
                    
                    binding.recyclerViewDoctors.layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerViewDoctors.adapter = DoctorListAdapter(doctors) { doctor ->
                        navigateToDoctorDetails(doctor.uid)
                    }
                } else {
                    showEmptyState("No doctors found for \"$query\"")
                }
            }
        }
    }
    
    private fun navigateToDoctorDetails(doctorId: String) {
        try {
            // For now, navigate to the booking flow with the selected doctor
            (activity as? MainPatientActivity)?.navigateToBookingWithDoctor(doctorId)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to view doctor details", Toast.LENGTH_SHORT).show()
        }    }
    
    private fun showEmptyState(message: String) {
        binding.recyclerViewDoctors.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.emptyStateText.text = message
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
