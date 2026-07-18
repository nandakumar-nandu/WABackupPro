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
                Snackbar.make(binding.root, "Sign-in failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
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
            
            // Check if status contains an error, if so, show retry
            if (status.contains("Error", ignoreCase = true) || status.contains("Failed", ignoreCase = true) || status.contains("expired", ignoreCase = true) || status.contains("halted", ignoreCase = true)) {
                binding.btnRetryBackup.visibility = View.VISIBLE
                binding.btnStartBackup.visibility = View.GONE
            } else {
                binding.btnRetryBackup.visibility = View.GONE
            }
        }

        // 🔗 Observe backup progress updates
        viewModel.backupProgress.observe(viewLifecycleOwner) { progress ->
            android.transition.TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            if (progress != null && progress.totalFiles > 0) {
                binding.progressBackup.visibility = View.VISIBLE
                binding.txtProgressCount.visibility = View.VISIBLE
                binding.txtCurrentFile.visibility = View.VISIBLE
                binding.btnStartBackup.isEnabled = false

                binding.progressBackup.max = progress.totalFiles
                binding.progressBackup.progress = progress.uploadedFiles
                
                binding.txtProgressCount.text = "Uploading ${progress.uploadedFiles} of ${progress.totalFiles} files"
                binding.txtCurrentFile.text = progress.currentFileName
            } else {
                binding.progressBackup.visibility = View.GONE
                binding.txtProgressCount.visibility = View.GONE
                binding.txtCurrentFile.visibility = View.GONE
                binding.btnStartBackup.isEnabled = true
            }
        }

        // 🔗 Observe active activity log changes
        viewModel.activityLogs.observe(viewLifecycleOwner) { logs ->
            logsAdapter.updateLogs(logs)
        }

        // 🔗 Observe Google account for UI state
        viewModel.googleAccount.observe(viewLifecycleOwner) { account ->
            if (account != null) {
                binding.btnGoogleLogin.text = "Sign Out (${account.email})"
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
            // Android 13+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            // Android < 13
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        when {
            permissionsNeeded.isEmpty() -> {
                viewModel.startBackup()
            }
            shouldShowRequestPermissionRationale(permissionsNeeded[0]) -> {
                showPermissionRationaleDialog(permissionsNeeded.toTypedArray())
            }
            else -> {
                requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
            }
        }
    }

    /**
     * Shows a rationale dialog to the user explaining why permissions are needed.
     */
    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_rationale_storage)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Handles cases where the user denies permissions.
     */
    private fun handlePermissionDenied() {
        Snackbar.make(
            binding.root,
            R.string.permission_denied_message,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
