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
 * action button, inline Demo Banner card, and lists operation logs.
 */
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

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
                if (_binding != null) {
                    binding.cardDemoBanner.visibility = View.GONE
                }
            } catch (e: Exception) {
                showSignInBanner()
            }
        } else {
            showSignInBanner()
        }
    }

    private fun showSignInBanner() {
        if (_binding == null) return
        binding.cardDemoBanner.visibility = View.VISIBLE
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

        logsAdapter = LogsAdapter()
        binding.recyclerViewLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logsAdapter
        }

        // Observe backup status messages safely
        viewModel.backupStatus.observe(viewLifecycleOwner) { status ->
            if (_binding == null) return@observe
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

        // Observe real-time progress updates safely
        viewModel.backupProgress.observe(viewLifecycleOwner) { progress ->
            if (_binding == null) return@observe
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

        // Observe activity logs
        viewModel.activityLogs.observe(viewLifecycleOwner) { logs ->
            if (_binding == null) return@observe
            logsAdapter.updateLogs(logs)
        }

        // Observe account state
        viewModel.googleAccount.observe(viewLifecycleOwner) { account ->
            if (_binding == null) return@observe
            if (account != null) {
                val email = account.email ?: "demo.user@gmail.com"
                binding.btnGoogleLogin.text = "Sign Out ($email)"
                binding.btnTestUpload.isEnabled = true
                binding.cardDemoBanner.visibility = View.GONE
            } else {
                binding.btnGoogleLogin.text = getString(R.string.btn_login_drive)
                binding.btnTestUpload.isEnabled = false
            }
        }

        // Observe file count feedback
        viewModel.discoveredFilesCount.observe(viewLifecycleOwner) { count ->
            if (_binding == null) return@observe
            if (count > 0) {
                Snackbar.make(binding.root, "Discovered $count files to backup", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Button Click Handlers
        binding.btnStartBackup.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.btnRetryBackup.setOnClickListener {
            binding.btnRetryBackup.visibility = View.GONE
            binding.btnStartBackup.visibility = View.VISIBLE
            checkPermissionsAndStart()
        }

        binding.btnGoogleLogin.setOnClickListener {
            if (viewModel.googleAccount.value == null) {
                signInLauncher.launch(viewModel.getSignInIntent())
            } else {
                viewModel.signOut()
            }
        }

        binding.btnQuickDemo.setOnClickListener {
            viewModel.enableDemoMode()
            binding.cardDemoBanner.visibility = View.GONE
            Toast.makeText(requireContext(), "Demo Mode Activated (demo.user@gmail.com)!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBannerEnableDemo.setOnClickListener {
            viewModel.enableDemoMode()
            binding.cardDemoBanner.visibility = View.GONE
            Toast.makeText(requireContext(), "Demo Mode Activated!", Toast.LENGTH_SHORT).show()
        }

        binding.btnTestUpload.setOnClickListener {
            viewModel.testUpload()
        }
    }

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
