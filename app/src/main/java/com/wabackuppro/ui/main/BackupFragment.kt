package com.wabackuppro.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.snackbar.Snackbar
import com.wabackuppro.R
import com.wabackuppro.databinding.FragmentBackupBinding

/**
 * BackupFragment displays the current backup status card, exposes a trigger
 * action button, and lists the active operation logs.
 */
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    // 🔄 Share the ViewModel scoped to the parent Activity
    private val viewModel: MainViewModel by activityViewModels()
    
    private lateinit var logsAdapter: LogsAdapter

    // 🛡️ Permission Request Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.startBackup()
        } else {
            handlePermissionDenied()
        }
    }

    // 🔑 Google Sign-In Launcher
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                viewModel.updateAccount(account)
            } catch (e: Exception) {
                showSignInError("Exception during sign-in: ${e.message}\nIf this is an ApiException (like code 10), your exported APK's SHA-1 fingerprint is not registered in the Google Cloud Console.")
            }
        } else {
            showSignInError("Sign-in cancelled or failed (Result Code: ${result.resultCode}).\nThis usually means the Google Cloud OAuth 2.0 client ID is missing the SHA-1 signature of the APK you just installed.")
        }
    }

    private fun showSignInError(error: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Google Sign-In Failed")
            .setMessage("$error\n\nWould you like to enable Demo Mode (demo.user@gmail.com) for testing and taking screenshots?")
            .setPositiveButton("Use Demo Mode") { dialog, _ ->
                viewModel.enableDemoMode()
                Toast.makeText(requireContext(), "Demo Mode Activated!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🛠️ Set up RecyclerView with LayoutManager and Adapter
        logsAdapter = LogsAdapter()
        binding.recyclerViewLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logsAdapter
        }

        // 🔗 Observe backup status messages
        viewModel.backupStatus.observe(viewLifecycleOwner) { status ->
            android.transition.TransitionManager.beginDelayedTransition(binding.cardBackupStatus)
            binding.txtStatusPlaceholder.text = status
            
            if (status.contains("Error", ignoreCase = true) || status.contains("Failed", ignoreCase = true) || status.contains("expired", ignoreCase = true) || status.contains("halted", ignoreCase = true)) {
                binding.btnStartBackup.visibility = View.GONE
                binding.btnRetryBackup.visibility = View.VISIBLE
            } else {
                binding.btnRetryBackup.visibility = View.GONE
                binding.btnStartBackup.visibility = View.VISIBLE
            }
        }

        // 📊 Observe real-time progress updates
        viewModel.backupProgress.observe(viewLifecycleOwner) { progress ->
            if (progress.totalFiles > 0) {
                binding.progressBackup.visibility = View.VISIBLE
                binding.txtProgressCount.visibility = View.VISIBLE
                binding.txtCurrentFile.visibility = View.VISIBLE

                binding.progressBackup.max = progress.totalFiles
                binding.progressBackup.progress = progress.uploadedFiles + progress.skippedFiles

                val skippedSuffix = if (progress.skippedFiles > 0) " (${progress.skippedFiles} skipped)" else ""
                binding.txtProgressCount.text = "Processing ${progress.uploadedFiles + progress.skippedFiles} of ${progress.totalFiles} files$skippedSuffix"
                binding.txtCurrentFile.text = progress.currentFileName ?: progress.status
            } else {
                binding.progressBackup.visibility = View.GONE
                binding.txtProgressCount.visibility = View.GONE
                binding.txtCurrentFile.visibility = View.GONE
            }
        }

        // 📜 Observe activity logs and update RecyclerView adapter
        viewModel.activityLogs.observe(viewLifecycleOwner) { logs ->
            logsAdapter.updateLogs(logs)
        }

        // 👤 Observe account state to update UI buttons
        viewModel.googleAccount.observe(viewLifecycleOwner) { account ->
            if (account != null) {
                val email = account.email ?: "demo.user@gmail.com"
                binding.btnGoogleLogin.text = "Sign Out ($email)"
                binding.btnTestUpload.isEnabled = true
            } else {
                binding.btnGoogleLogin.text = getString(R.string.btn_login_drive)
                binding.btnTestUpload.isEnabled = false
            }
        }

        // 🔗 Observe file count for UI feedback
        viewModel.discoveredFilesCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                Snackbar.make(binding.root, "Discovered $count files to backup", Snackbar.LENGTH_SHORT).show()
            }
        }

        // 👆 Handle start backup button click
        binding.btnStartBackup.setOnClickListener {
            checkPermissionsAndStart()
        }

        // 👆 Handle retry backup button click
        binding.btnRetryBackup.setOnClickListener {
            binding.btnRetryBackup.visibility = View.GONE
            binding.btnStartBackup.visibility = View.VISIBLE
            checkPermissionsAndStart()
        }

        // 👆 Handle login/logout button click
        binding.btnGoogleLogin.setOnClickListener {
            if (viewModel.googleAccount.value == null) {
                signInLauncher.launch(viewModel.getSignInIntent())
            } else {
                viewModel.signOut()
            }
        }

        // 💡 Long-press on login button for instant Demo Mode
        binding.btnGoogleLogin.setOnLongClickListener {
            viewModel.enableDemoMode()
            Toast.makeText(requireContext(), "Demo Mode Activated (demo.user@gmail.com)!", Toast.LENGTH_SHORT).show()
            true
        }

        // 👆 Handle test upload button click
        binding.btnTestUpload.setOnClickListener {
            viewModel.testUpload()
        }
    }

    /**
     * Checks for necessary storage permissions before triggering the backup scan.
     */
    private fun checkPermissionsAndStart() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            if (shouldShowRequestPermissionRationale(permissionsNeeded.first())) {
                showPermissionRationaleDialog(permissionsNeeded.toTypedArray())
            } else {
                requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
            }
        } else {
            viewModel.startBackup()
        }
    }

    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Storage Permissions Required")
            .setMessage("WABackupPro requires storage access to scan and back up your WhatsApp Business database and media files to Google Drive.")
            .setPositiveButton("Grant Access") { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handlePermissionDenied() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(binding.root, "Permissions permanently denied. Please enable them in Settings.", Snackbar.LENGTH_LONG)
                .setAction("Settings") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                    startActivity(intent)
                }
                .show()
        } else {
            Snackbar.make(binding.root, "Storage permission is required to scan WhatsApp files.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
