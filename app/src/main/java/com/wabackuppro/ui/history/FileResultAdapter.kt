package com.wabackuppro.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wabackuppro.R
import com.wabackuppro.data.local.entities.BackupFileResult
import java.util.Locale

/**
 * FileResultAdapter displays individual file outcome rows in the Backup Detail screen.
 */
class FileResultAdapter(
    private val onItemClick: (BackupFileResult) -> Unit
) : ListAdapter<BackupFileResult, FileResultAdapter.ViewHolder>(FileResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_result, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (BackupFileResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val txtStatusIcon: TextView = itemView.findViewById(R.id.txt_status_icon)
        private val txtFileName: TextView = itemView.findViewById(R.id.txt_file_name)
        private val txtCategoryTag: TextView = itemView.findViewById(R.id.txt_category_tag)
        private val txtFileSize: TextView = itemView.findViewById(R.id.txt_file_size)
        private val txtErrorHint: TextView = itemView.findViewById(R.id.txt_error_hint)
        private val txtStatusLabel: TextView = itemView.findViewById(R.id.txt_status_label)

        fun bind(result: BackupFileResult) {
            txtFileName.text = result.fileName
            txtCategoryTag.text = result.category
            txtFileSize.text = formatFileSize(result.sizeBytes)
            txtStatusLabel.text = result.status

            when (result.status.uppercase(Locale.ROOT)) {
                "SUCCESS" -> {
                    txtStatusIcon.text = "✅"
                    txtErrorHint.visibility = View.GONE
                }
                "SKIPPED" -> {
                    txtStatusIcon.text = "⏭️"
                    txtErrorHint.visibility = View.GONE
                }
                "FAILED" -> {
                    txtStatusIcon.text = "❌"
                    txtErrorHint.visibility = View.VISIBLE
                    txtErrorHint.text = result.errorMessage ?: "Upload failed (Tap to retry)"
                }
            }

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
    }
}

class FileResultDiffCallback : DiffUtil.ItemCallback<BackupFileResult>() {
    override fun areItemsTheSame(oldItem: BackupFileResult, newItem: BackupFileResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BackupFileResult, newItem: BackupFileResult): Boolean {
        return oldItem == newItem
    }
}
