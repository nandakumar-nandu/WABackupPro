package com.wabackuppro.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.wabackuppro.databinding.ActivityAboutBinding
import org.json.JSONObject

class AboutActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityAboutBinding

    // UPI placeholder (swappable)
    private val upiId = "nandakumar@upi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Links
        binding.btnPrivacy.setOnClickListener { openUrl("https://example.com/privacy") }
        binding.btnTerms.setOnClickListener { openUrl("https://example.com/terms") }
        
        // Payments & Donations
        Checkout.preload(applicationContext)

        binding.btnDonateRazorpay.setOnClickListener { startRazorpayCheckout() }
        binding.btnDonateUpi.setOnClickListener { startUpiPayment() }
        binding.btnDonateCoffee.setOnClickListener { openUrl("https://buymeacoffee.com/nandakumar") }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun startUpiPayment() {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", "WABackupPro Developer")
            .appendQueryParameter("tn", "Donation for WABackupPro")
            .appendQueryParameter("am", "100.00")
            .appendQueryParameter("cu", "INR")
            .build()
        val upiPayIntent = Intent(Intent.ACTION_VIEW, uri)
        
        // Create chooser to show all UPI apps
        val chooser = Intent.createChooser(upiPayIntent, "Pay with")
        if (chooser.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRazorpayCheckout() {
        val checkout = Checkout()
        // Boilerplate Key. Usually placed in Manifest or set here.
        checkout.setKeyID("rzp_test_YOUR_KEY_HERE")
        
        try {
            val options = JSONObject()
            options.put("name", "WABackupPro")
            options.put("description", "Support Development")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg")
            options.put("theme.color", "#4CAF50")
            options.put("currency", "INR")
            options.put("amount", "10000") // Amount is in paise (Rs. 100)
            
            val preFill = JSONObject()
            preFill.put("email", "test@example.com")
            preFill.put("contact", "9876543210")
            options.put("prefill", preFill)
            
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in starting Razorpay Checkout", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Toast.makeText(this, "Payment Successful: $razorpayPaymentID", Toast.LENGTH_LONG).show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}
