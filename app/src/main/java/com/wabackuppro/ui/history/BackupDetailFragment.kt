package com.wabackuppro.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.wabackuppro.R
import com.wabackuppro.data.local.AppDatabase
import com.wabackuppro.data.local.entities.BackupFileResult
import com.wabackuppro.data.local.entities.BackupRecord
import com.wabackuppro.data.remote.DriveClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupDetailFragment provides drill-down visibility into individual file outcomes of a past backup job.
 * 
 * Retry-Single-File Flow Algorithm:
 * 1. User taps a FAILED file item in the results list.
 * 2. An [AlertDialog] opens displaying the error traceback and a "Retry This File" button.
 * 3. Tapping "Retry This File" checks for a valid authenticated Google account via GoogleSignIn.
 * 4. In a background thread (Dispatchers.IO), it attempts a single-file upload using [DriveClient.uploadFile].
 * 5. Upon successful upload, [BackupFileResultDao.updateStatus] updates the row in Room DB from FAILED to SUCCESS.
 * 6. The UI automatically reflects the updated state via reactive Flow observation.
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

    private var recordId: Long = 0L
    private lateinit var adapter: FileResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordId = arguments?.getLong(ARG_RECORD_ID) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_backup_detail, container, false)

        val btnBack: ImageButton = view.findViewById(R.id.btn_back)
        val txtDetailDate: TextView = view.findViewById(R.id.txt_detail_date)
        val badgeDetailStatus: TextView = view.findViewById(R.id.badge_detail_status)
        val txtDetailSummary: TextView = view.findViewById(R.id.txt_detail_summary)
        val txtDetailCategories: TextView = view.findViewById(R.id.txt_detail_categories)
        val btnDetailOpenDrive: Button = view.findViewById(R.id.btn_detail_open_drive)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView_file_results)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = FileResultAdapter { fileResult ->
            onFileItemClicked(fileResult)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val db = AppDatabase.getDatabase(requireContext())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        // Fetch parent record details
        viewLifecycleOwner.lifecycleScope.launch {
            val record = db.backupRecordDao().getById(recordId)
            if (record != null) {
                txtDetailDate.text = dateFormat.format(Date(record.timestamp))
                txtDetailSummary.text = "Files: ${record.totalFiles} (Success: ${record.successCount}, Failed: ${record.failCount}) · ${record.durationSeconds}s"
                txtDetailCategories.text = record.uploadedFilesManifest ?: "All Categories"

                if (record.failCount == 0 && record.successCount > 0) {
                    badgeDetailStatus.text = "SUCCESS"
                    badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_success)
                } else if (record.successCount > 0) {
                    badgeDetailStatus.text = "PARTIAL"
                    badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_success)
                } else {
                    badgeDetailStatus.text = "FAILED"
                    badgeDetailStatus.setBackgroundResource(R.drawable.bg_badge_error)
                }

                if (!record.driveFolderLink.isNullOrEmpty()) {
                    btnDetailOpenDrive.visibility = View.VISIBLE
                    btnDetailOpenDrive.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.driveFolderLink))
                        startActivity(intent)
                    }
                } else {
                    btnDetailOpenDrive.visibility = View.GONE
                }
            }
        }

        // Observe per-file outcomes Flow
        viewLifecycleOwner.lifecycleScope.launch {
            db.backupFileResultDao().getByBackupRecordId(recordId).collectLatest { results ->
                adapter.submitList(results)
            }
        }

        return view
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
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {
            Toast.makeText(requireContext(), "Google Sign-In required to retry upload.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Retrying ${fileResult.fileName}...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val driveClient = DriveClient(requireContext())
                val folderId = "WABackup_Retry" // Or target existing folder
                val fileId = driveClient.uploadFile(account, fileResult.filePath, folderId, "application/octet-stream")

                if (fileId != null) {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.backupFileResultDao().updateStatus(fileResult.id, "SUCCESS", null)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "✅ Successfully uploaded ${fileResult.fileName}!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "❌ Retry failed for ${fileResult.fileName}.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "❌ Retry error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
