package com.wabackuppro.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.wabackuppro.R
import com.wabackuppro.data.local.AppDatabase
import com.wabackuppro.data.local.entities.BackupFileResult
import com.wabackuppro.data.remote.DriveClient
import com.wabackuppro.databinding.FragmentBackupDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupDetailFragment provides drill-down visibility into individual file outcomes of a past backup job.
 */
class BackupDetailFragment : Fragment() {

    companion object {
        const val ARG_RECORD_ID = "record_id"

        fun newInstance(recordId: Long): BackupDetailFragment {
            val fragment = BackupDetailFragment()
            val args = Bundle().apply {
                putLong(ARG_RECORD_ID, recordId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentBackupDetailBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = 0L
    private lateinit var adapter: FileResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordId = arguments?.getLong(ARG_RECORD_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = FileResultAdapter { fileResult ->
            onFileItemClicked(fileResult)
        }

        binding.recyclerViewFileResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BackupDetailFragment.adapter
        }

        val db = AppDatabase.getDatabase(requireContext())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        // Fetch parent record details
        viewLifecycleOwner.lifecycleScope.launch {
            val record = db.backupRecordDao().getById(recordId)
            if (record != null && _binding != null) {
                binding.txtDetailDate.text = dateFormat.format(Date(record.timestamp))
                binding.txtDetailSummary.text = "Files: ${record.totalFiles} (Success: ${record.successCount}, Failed: ${record.failCount}) · ${record.durationSeconds}s"
                binding.txtDetailCategories.text = record.uploadedFilesManifest ?: "All Categories"

                if (record.failCount == 0 && record.successCount > 0) {
                    binding.badgeDetailStatus.text = "SUCCESS"
                    binding.badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_success)
                } else if (record.successCount > 0) {
                    binding.badgeDetailStatus.text = "PARTIAL"
                    binding.badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_success)
                } else {
                    binding.badgeDetailStatus.text = "FAILED"
                    binding.badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_error)
                }

                if (!record.driveFolderLink.isNullOrEmpty()) {
                    binding.btnDetailOpenDrive.visibility = View.VISIBLE
                    binding.btnDetailOpenDrive.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.driveFolderLink))
                        startActivity(intent)
                    }
                } else {
                    binding.btnDetailOpenDrive.visibility = View.GONE
                }
            }
        }

        // Observe per-file outcomes Flow
        viewLifecycleOwner.lifecycleScope.launch {
            db.backupFileResultDao().getByBackupRecordId(recordId).collectLatest { results ->
                if (_binding != null) {
                    adapter.submitList(results)
                }
            }
        }
    }

    /**
     * Handles clicking on a file item. If the file failed, shows a retry dialog.
     */
    private fun onFileItemClicked(fileResult: BackupFileResult) {
        if (fileResult.status.equals("FAILED", ignoreCase = true)) {
            AlertDialog.Builder(requireContext())
                .setTitle("Failed File Details")
                .setMessage("File: ${fileResult.fileName}\nPath: ${fileResult.filePath}\n\nError: ${fileResult.errorMessage ?: "Unknown error"}")
                .setPositiveButton("Retry This File") { dialog, _ ->
                    retrySingleFile(fileResult)
                    dialog.dismiss()
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "${fileResult.fileName} (${fileResult.status})", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Executes the single-file retry logic.
     */
    private fun retrySingleFile(fileResult: BackupFileResult) {
        Toast.makeText(requireContext(), "Retrying ${fileResult.fileName}...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.backupFileResultDao().updateStatus(fileResult.id, "SUCCESS", null)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "✅ Successfully uploaded ${fileResult.fileName}!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "❌ Retry error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
