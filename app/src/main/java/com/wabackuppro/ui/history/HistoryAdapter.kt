package com.wabackuppro.ui.history

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wabackuppro.R
import com.wabackuppro.data.local.entities.BackupRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<BackupRecord, HistoryAdapter.ViewHolder>(BackupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_backup_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDate: TextView = itemView.findViewById(R.id.txt_date)
        private val badgeStatus: TextView = itemView.findViewById(R.id.badge_status)
        private val txtFilesCount: TextView = itemView.findViewById(R.id.txt_files_count)
        private val txtDuration: TextView = itemView.findViewById(R.id.txt_duration)
        private val btnOpenDrive: Button = itemView.findViewById(R.id.btn_open_drive)

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        fun bind(record: BackupRecord) {
            txtDate.text = dateFormat.format(Date(record.timestamp))
            txtFilesCount.text = "Files: ${record.totalFiles} (Success: ${record.successCount}, Failed: ${record.failCount})"
            txtDuration.text = "Duration: ${record.durationSeconds}s"

            if (record.failCount == 0 && record.successCount > 0) {
                badgeStatus.text = "SUCCESS"
                badgeStatus.setBackgroundResource(R.drawable.bg_badge_success)
            } else if (record.successCount > 0) {
                badgeStatus.text = "PARTIAL"
                badgeStatus.setBackgroundResource(R.drawable.bg_badge_success) // Could use a different color
            } else {
                badgeStatus.text = "FAILED"
                badgeStatus.setBackgroundResource(R.drawable.bg_badge_error)
            }

            if (!record.driveFolderLink.isNullOrEmpty()) {
                btnOpenDrive.visibility = View.VISIBLE
                btnOpenDrive.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.driveFolderLink))
                    it.context.startActivity(intent)
                }
            } else {
                btnOpenDrive.visibility = View.GONE
            }
        }
    }
}

class BackupDiffCallback : DiffUtil.ItemCallback<BackupRecord>() {
    override fun areItemsTheSame(oldItem: BackupRecord, newItem: BackupRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BackupRecord, newItem: BackupRecord): Boolean {
        return oldItem == newItem
    }
}
