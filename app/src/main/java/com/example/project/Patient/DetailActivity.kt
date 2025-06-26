package com.example.project.Patient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.project.databinding.ActivityDetailBinding

/**
 * [DetailActivity] displays detailed information about a selected doctor.
 *
 * This activity receives a [DoctorsModel] object via an Intent and populates
 * its UI with the doctor's name, specialization, patient count, biography,
 * address, experience, and rating. It also provides interactive buttons for
 * visiting the doctor's website, sending a message, making a call, getting
 * directions, and sharing the doctor's details.
 */
class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var item: DoctorsModel

    /**
     * Called when the activity is first created.
     *
     * Initializes the view binding and retrieves the [DoctorsModel] object
     * passed through the intent.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState]. Note: Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getBundle()
    }

    /**
     * Retrieves the [DoctorsModel] object from the Intent and populates
     * the UI elements with the doctor's details.
     *
     * Sets up click listeners for various action buttons:
     * - **backBtn**: Finishes the activity.
     * - **websiteBtn**: Opens the doctor's website in a browser.
     * - **messageBtn**: Opens the SMS application to send a message to the doctor.
     * - **callBtn**: Initiates a phone call to the doctor.
     * - **directionBtn**: Opens a map application for directions to the doctor's location.
     * - **shareBtn**: Allows sharing the doctor's name, address, and mobile number.
     * It also loads the doctor's picture using Glide.
     */
    private fun getBundle() {
        item = intent.getParcelableExtra("object")!!

        binding.apply {
            titleTxt.text = item.Name
            specialTxt.text = item.Special
            patiensTxt.text = item.Patiens
            bioTxt.text = item.Biography
            addressTxt.text = item.Address

            experienceTxt.text = item.Expriense.toString() + " Years"
            ratingTxt.text = "${item.Rating}"
            backBtn.setOnClickListener { finish() }

            websiteBtn.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(item.Site))
                startActivity(i)
            }

            messageBtn.setOnClickListener {
                val uri = Uri.parse("smsto:${item.Mobile}")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                intent.putExtra("sms_body", "the SMS text")
                startActivity(intent)
            }

            callBtn.setOnClickListener {
                val uri = "tel:" + item.Mobile.trim()
                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse(uri)
                )
                startActivity(intent)
            }
            directionBtn.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(item.Location)
                )
                startActivity(intent)
            }

            shareBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.setType("text/plain")
                intent.putExtra(Intent.EXTRA_SUBJECT, item.Name)
                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    item.Name + " " + item.Address + " " + item.Mobile
                )
                startActivity(Intent.createChooser(intent, "Choose one"))
            }

            Glide.with(this@DetailActivity)
                .load(item.Picture)
                .into(img)
        }
    }
}