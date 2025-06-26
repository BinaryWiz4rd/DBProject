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

/**
 * A [Fragment] that displays a list of doctors for patients.
 * This fragment allows patients to view all doctors, search for specific doctors,
 * and navigate to a doctor's details or booking page.
 */
class PatientDoctorsFragment : Fragment() {

    private var _binding: FragmentPatientDoctorsBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth

    /**
     * Inflates the layout for this fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initializes UI components, FirestoreHelper, and FirebaseAuth.
     * Sets up the search functionality and loads all doctors.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()

        setupUI()
        loadAllDoctors()
    }

    /**
     * Sets up the search view's query text listener.
     * When a query is submitted, it searches for doctors.
     * When the text changes, it reloads all doctors if the query is empty,
     * or searches if the query length is greater than 2 characters.
     */
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

    /**
     * Loads all doctors from Firestore and displays them in the RecyclerView.
     * Shows a progress bar while loading and an empty state if no doctors are found.
     */
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
                        navigateToDoctorDetails(doctor.uid)
                    }
                } else {
                    showEmptyState("No doctors found")
                }
            }
        }
    }

    /**
     * Searches for doctors based on the provided query and displays the results.
     * Shows a progress bar while searching and an empty state if no doctors match the query.
     *
     * @param query The search query string.
     */
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

    /**
     * Navigates to the doctor details or booking flow for the selected doctor.
     *
     * @param doctorId The unique ID of the selected doctor.
     */
    private fun navigateToDoctorDetails(doctorId: String) {
        try {
            (activity as? MainPatientActivity)?.navigateToBookingWithDoctor(doctorId)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to view doctor details", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays an empty state message when no doctors are found.
     *
     * @param message The message to display in the empty state.
     */
    private fun showEmptyState(message: String) {
        binding.recyclerViewDoctors.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.emptyStateText.text = message
    }

    /**
     * Cleans up the binding when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}