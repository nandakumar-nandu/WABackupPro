package com.wabackuppro.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
            binding.txtStatusPlaceholder.text = status
        }

        // 🔗 Observe active activity log changes
        viewModel.activityLogs.observe(viewLifecycleOwner) { logs ->
            logsAdapter.updateLogs(logs)
        }

        // 👆 Handle start backup button click
        binding.btnStartBackup.setOnClickListener {
            viewModel.startBackup()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
