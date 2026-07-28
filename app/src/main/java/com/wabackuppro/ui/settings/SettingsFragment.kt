package com.wabackuppro.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wabackuppro.data.local.AppDatabase
import com.wabackuppro.databinding.FragmentSettingsBinding
import com.wabackuppro.domain.models.BackupCategory
import com.wabackuppro.ui.about.AboutActivity
import com.wabackuppro.ui.main.MainViewModel
import com.wabackuppro.utils.BackupScheduler
import com.wabackuppro.workers.BackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * SettingsFragment provides user preference controls, selective category toggles,
 * account controls, demo mode toggles, and manual backup triggers.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    
    // SharedPreferences key constants
    companion object {
        const val PREFS_NAME = "WABackupPrefs"
        const val PREF_BACKUP_TIME_HOUR = "backup_time_hour"
        const val PREF_BACKUP_TIME_MINUTE = "backup_time_minute"
        const val PREF_WIFI_ONLY = "wifi_only"
        const val PREF_HISTORY_DAYS = "history_days"
        const val PREF_FORCE_FULL_BACKUP = "force_full_backup"

        // 🗂️ Selective Backup Category SharedPreferences keys
        const val PREF_CAT_DOCUMENTS = "cat_documents"
        const val PREF_CAT_IMAGES = "cat_images"
        const val PREF_CAT_VIDEO = "cat_video"
        const val PREF_CAT_AUDIO = "cat_audio"
        const val PREF_CAT_VOICE_NOTES = "cat_voice_notes"

        /**
         * Helper method to read selected categories from SharedPreferences.
         */
        fun getSelectedCategories(context: Context): Set<BackupCategory> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val selected = mutableSetOf<BackupCategory>()

            if (prefs.getBoolean(PREF_CAT_DOCUMENTS, true)) selected.add(BackupCategory.DOCUMENTS)
            if (prefs.getBoolean(PREF_CAT_IMAGES, true)) selected.add(BackupCategory.IMAGES)
            if (prefs.getBoolean(PREF_CAT_VIDEO, true)) selected.add(BackupCategory.VIDEO)
            if (prefs.getBoolean(PREF_CAT_AUDIO, true)) selected.add(BackupCategory.AUDIO)
            if (prefs.getBoolean(PREF_CAT_VOICE_NOTES, true)) selected.add(BackupCategory.VOICE_NOTES)

            return selected
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load existing preferences safely
        val savedHour = prefs.getInt(PREF_BACKUP_TIME_HOUR, 2)
        val savedMinute = prefs.getInt(PREF_BACKUP_TIME_MINUTE, 0)
        binding.btnTimePicker.text = formatTime(savedHour, savedMinute)
        
        binding.switchWifiOnly.isChecked = prefs.getBoolean(PREF_WIFI_ONLY, true)
        binding.switchForceFullBackup.isChecked = prefs.getBoolean(PREF_FORCE_FULL_BACKUP, false)
        binding.etHistoryDays.setText(prefs.getInt(PREF_HISTORY_DAYS, 30).toString())

        // Load Category switch states
        binding.switchCatDocuments.isChecked = prefs.getBoolean(PREF_CAT_DOCUMENTS, true)
        binding.switchCatImages.isChecked = prefs.getBoolean(PREF_CAT_IMAGES, true)
        binding.switchCatVideo.isChecked = prefs.getBoolean(PREF_CAT_VIDEO, true)
        binding.switchCatAudio.isChecked = prefs.getBoolean(PREF_CAT_AUDIO, true)
        binding.switchCatVoiceNotes.isChecked = prefs.getBoolean(PREF_CAT_VOICE_NOTES, true)

        // Time Picker Logic
        binding.btnTimePicker.setOnClickListener {
            val currentHour = prefs.getInt(PREF_BACKUP_TIME_HOUR, 2)
            val currentMinute = prefs.getInt(PREF_BACKUP_TIME_MINUTE, 0)
            
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                prefs.edit()
                    .putInt(PREF_BACKUP_TIME_HOUR, selectedHour)
                    .putInt(PREF_BACKUP_TIME_MINUTE, selectedMinute)
                    .apply()
                binding.btnTimePicker.text = formatTime(selectedHour, selectedMinute)
                
                BackupScheduler(requireContext()).scheduleFridayBackup(LocalTime.of(selectedHour, selectedMinute))
            }, currentHour, currentMinute, false).show()
        }

        // Wi-Fi Only & Force Full Backup Logic
        binding.switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_WIFI_ONLY, isChecked).apply()
        }

        binding.switchForceFullBackup.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_FORCE_FULL_BACKUP, isChecked).apply()
            val msg = if (isChecked) "Force full backup enabled (Delta detection bypassed)." else "Incremental backup enabled (Delta detection active)."
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Category Switch Change Listeners
        binding.switchCatDocuments.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_DOCUMENTS, isChecked).apply()
        }
        binding.switchCatImages.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_IMAGES, isChecked).apply()
        }
        binding.switchCatVideo.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_VIDEO, isChecked).apply()
        }
        binding.switchCatAudio.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_AUDIO, isChecked).apply()
        }
        binding.switchCatVoiceNotes.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_VOICE_NOTES, isChecked).apply()
        }

        // Select All / Select None Shortcut Buttons
        binding.btnSelectAllCategories.setOnClickListener {
            binding.switchCatDocuments.isChecked = true
            binding.switchCatImages.isChecked = true
            binding.switchCatVideo.isChecked = true
            binding.switchCatAudio.isChecked = true
            binding.switchCatVoiceNotes.isChecked = true

            prefs.edit()
                .putBoolean(PREF_CAT_DOCUMENTS, true)
                .putBoolean(PREF_CAT_IMAGES, true)
                .putBoolean(PREF_CAT_VIDEO, true)
                .putBoolean(PREF_CAT_AUDIO, true)
                .putBoolean(PREF_CAT_VOICE_NOTES, true)
                .apply()

            Toast.makeText(requireContext(), "All categories selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnSelectNoneCategories.setOnClickListener {
            binding.switchCatDocuments.isChecked = false
            binding.switchCatImages.isChecked = false
            binding.switchCatVideo.isChecked = false
            binding.switchCatAudio.isChecked = false
            binding.switchCatVoiceNotes.isChecked = false

            prefs.edit()
                .putBoolean(PREF_CAT_DOCUMENTS, false)
                .putBoolean(PREF_CAT_IMAGES, false)
                .putBoolean(PREF_CAT_VIDEO, false)
                .putBoolean(PREF_CAT_AUDIO, false)
                .putBoolean(PREF_CAT_VOICE_NOTES, false)
                .apply()

            Toast.makeText(requireContext(), "All categories deselected", Toast.LENGTH_SHORT).show()
        }

        // History Days Logic
        binding.etHistoryDays.doAfterTextChanged { editable ->
            val days = editable?.toString()?.toIntOrNull() ?: 30
            prefs.edit().putInt(PREF_HISTORY_DAYS, days).apply()
        }

        // Account Logic
        viewModel.googleAccount.observe(viewLifecycleOwner) { account ->
            if (account != null) {
                val email = account.email ?: "demo.user@gmail.com"
                binding.txtAccountEmail.text = "Signed in as: $email"
                binding.btnSignOut.isEnabled = true
            } else {
                binding.txtAccountEmail.text = "Not signed in"
                binding.btnSignOut.isEnabled = false
            }
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
        }

        // Manual Trigger
        binding.btnForceBackup.setOnClickListener {
            val workRequest = OneTimeWorkRequestBuilder<BackupWorker>().build()
            WorkManager.getInstance(requireContext()).enqueue(workRequest)
            Toast.makeText(requireContext(), "Backup job queued in background.", Toast.LENGTH_SHORT).show()
        }

        // Safe About Button Handler
        binding.btnAbout.setOnClickListener {
            try {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
            } catch (e: Exception) {
                AlertDialog.Builder(requireContext())
                    .setTitle("WABackupPro v1.3.0")
                    .setMessage("Automated WhatsApp Business Google Drive Backup Utility.\n\nDeveloped by Antigravity Deepmind Team.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        return time.format(formatter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
