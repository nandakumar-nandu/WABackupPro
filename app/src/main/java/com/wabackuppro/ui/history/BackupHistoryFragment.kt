package com.wabackuppro.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wabackuppro.R
import com.wabackuppro.data.local.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * BackupHistoryFragment displays a list of past backup executions fetched from the local Room database.
 */
class BackupHistoryFragment : Fragment() {

    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView_history)

        historyAdapter = HistoryAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = historyAdapter

        val db = AppDatabase.getDatabase(requireContext())
        
        // Observe Room DB flow and update UI
        viewLifecycleOwner.lifecycleScope.launch {
            db.backupRecordDao().getAll().collectLatest { records ->
                historyAdapter.submitList(records)
            }
        }

        return view
    }
}
