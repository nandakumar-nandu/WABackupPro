package com.wabackuppro.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * LogsAdapter handles binding activity log messages to the RecyclerView in BackupFragment.
 */
class LogsAdapter(private var logs: List<String> = emptyList()) :
    RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

    /**
     * ViewHolder for logs items, reusing the system default text layout.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 🛠️ Inflate Android standard layout for list item
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.id.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 🔗 Bind the log text and set visual configuration
        holder.textView.text = logs[position]
        holder.textView.textSize = 13f
        holder.textView.setPadding(16, 8, 16, 8)
    }

    override fun getItemCount(): Int = logs.size

    /**
     * Updates the data set and refreshes the list items.
     */
    fun updateLogs(newLogs: List<String>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
