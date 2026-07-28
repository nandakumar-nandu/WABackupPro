package com.wabackuppro.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.wabackuppro.R
import com.wabackuppro.data.local.AppDatabase
import com.wabackuppro.data.local.entities.BackupRecord
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupHistoryFragment displays past backup executions with reactive search bar filtering,
 * status chip filters, and drill-down navigation to [BackupDetailFragment].
 */
class BackupHistoryFragment : Fragment() {

    private lateinit var historyAdapter: HistoryAdapter
    private var allRecords: List<BackupRecord> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: Int = R.id.chip_all

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView_history)
        val etSearch: EditText = view.findViewById(R.id.et_search_history)
        val chipGroup: ChipGroup = view.findViewById(R.id.chip_group_status)

        // 🔗 Row Click Listener: Opens BackupDetailFragment for drill-down inspection
        historyAdapter = HistoryAdapter { record ->
            val detailFragment = BackupDetailFragment.newInstance(record.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = historyAdapter

        val db = AppDatabase.getDatabase(requireContext())
        
        // 🔄 Observe Room DB flow
        viewLifecycleOwner.lifecycleScope.launch {
            db.backupRecordDao().getAll().collectLatest { records ->
                allRecords = records
                applyFilterAndSearch()
            }
        }

        // 🔍 Real-time Search Text Filter Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 🏷️ Filter Chips Listener
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentStatusFilter = checkedIds.firstOrNull() ?: R.id.chip_all
            applyFilterAndSearch()
        }

        return view
    }

    /**
     * Filter Query Logic:
     * Combines user search string (matching folderName or formatted timestamp) and status chip filter
     * in memory, avoiding redundant database queries while keeping the UI immediately reactive.
     */
    private fun applyFilterAndSearch() {
        val filteredList = allRecords.filter { record ->
            // 1. Check Search Query match (folderName or formatted date string)
            val formattedDate = dateFormat.format(Date(record.timestamp))
            val matchesQuery = currentSearchQuery.isEmpty() ||
                    record.folderName.contains(currentSearchQuery, ignoreCase = true) ||
                    formattedDate.contains(currentSearchQuery, ignoreCase = true)

            // 2. Check Status Chip Filter match
            val matchesStatus = when (currentStatusFilter) {
                R.id.chip_success -> record.failCount == 0 && record.successCount > 0
                R.id.chip_partial -> record.failCount > 0 && record.successCount > 0
                R.id.chip_failed -> record.successCount == 0 || (record.failCount > 0 && record.successCount == 0)
                else -> true // R.id.chip_all
            }

            matchesQuery && matchesStatus
        }

        historyAdapter.submitList(filteredList)
    }
}
