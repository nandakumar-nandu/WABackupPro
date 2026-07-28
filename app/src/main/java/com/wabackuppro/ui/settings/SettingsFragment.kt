package com.wabackuppro.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.wabackuppro.R
import com.wabackuppro.domain.models.BackupCategory
import com.wabackuppro.ui.about.AboutActivity
import com.wabackuppro.ui.main.MainViewModel
import com.wabackuppro.utils.BackupScheduler
import com.wabackuppro.workers.BackupWorker
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * SettingsFragment provides user preference controls and triggers manual actions.
 */
class SettingsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    
    // SharedPreferences key constants
    companion object {
        const val PREFS_NAME = "WABackupPrefs"
        const val PREF_BACKUP_TIME_HOUR = "backup_time_hour"
        const val PREF_BACKUP_TIME_MINUTE = "backup_time_minute"
        const val PREF_WIFI_ONLY = "wifi_only"
        const val PREF_HISTORY_DAYS = "history_days"

        // 🔄 Force Full Backup override key constant
        const val PREF_FORCE_FULL_BACKUP = "force_full_backup"

        // 🗂️ Selective Backup Category SharedPreferences key constants
        // Each key stores a Boolean flag indicating whether the category is enabled for backup.
        const val PREF_CAT_DOCUMENTS = "cat_documents"
        const val PREF_CAT_IMAGES = "cat_images"
        const val PREF_CAT_VIDEO = "cat_video"
        const val PREF_CAT_AUDIO = "cat_audio"
        const val PREF_CAT_VOICE_NOTES = "cat_voice_notes"

        /**
         * Helper method to read the currently selected set of [BackupCategory] values from SharedPreferences.
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnTimePicker: Button = view.findViewById(R.id.btn_time_picker)
        val switchWifiOnly: MaterialSwitch = view.findViewById(R.id.switch_wifi_only)
        val switchForceFullBackup: MaterialSwitch = view.findViewById(R.id.switch_force_full_backup)
        val etHistoryDays: EditText = view.findViewById(R.id.et_history_days)

        // Category Switches & Buttons
        val switchCatDocs: MaterialSwitch = view.findViewById(R.id.switch_cat_documents)
        val switchCatImages: MaterialSwitch = view.findViewById(R.id.switch_cat_images)
        val switchCatVideo: MaterialSwitch = view.findViewById(R.id.switch_cat_video)
        val switchCatAudio: MaterialSwitch = view.findViewById(R.id.switch_cat_audio)
        val switchCatVoiceNotes: MaterialSwitch = view.findViewById(R.id.switch_cat_voice_notes)
        val btnSelectAll: Button = view.findViewById(R.id.btn_select_all_categories)
        val btnSelectNone: Button = view.findViewById(R.id.btn_select_none_categories)

        val txtAccountEmail: TextView = view.findViewById(R.id.txt_account_email)
        val btnSignOut: Button = view.findViewById(R.id.btn_sign_out)
        val btnForceBackup: Button = view.findViewById(R.id.btn_force_backup)
        val btnAbout: Button = view.findViewById(R.id.btn_about)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load existing preferences
        val savedHour = prefs.getInt(PREF_BACKUP_TIME_HOUR, 2)
        val savedMinute = prefs.getInt(PREF_BACKUP_TIME_MINUTE, 0)
        btnTimePicker.text = formatTime(savedHour, savedMinute)
        
        switchWifiOnly.isChecked = prefs.getBoolean(PREF_WIFI_ONLY, true)
        switchForceFullBackup.isChecked = prefs.getBoolean(PREF_FORCE_FULL_BACKUP, false)
        etHistoryDays.setText(prefs.getInt(PREF_HISTORY_DAYS, 30).toString())

        // Load Category switch states
        switchCatDocs.isChecked = prefs.getBoolean(PREF_CAT_DOCUMENTS, true)
        switchCatImages.isChecked = prefs.getBoolean(PREF_CAT_IMAGES, true)
        switchCatVideo.isChecked = prefs.getBoolean(PREF_CAT_VIDEO, true)
        switchCatAudio.isChecked = prefs.getBoolean(PREF_CAT_AUDIO, true)
        switchCatVoiceNotes.isChecked = prefs.getBoolean(PREF_CAT_VOICE_NOTES, true)

        // Time Picker Logic
        btnTimePicker.setOnClickListener {
            val currentHour = prefs.getInt(PREF_BACKUP_TIME_HOUR, 2)
            val currentMinute = prefs.getInt(PREF_BACKUP_TIME_MINUTE, 0)
            
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                prefs.edit()
                    .putInt(PREF_BACKUP_TIME_HOUR, selectedHour)
                    .putInt(PREF_BACKUP_TIME_MINUTE, selectedMinute)
                    .apply()
                btnTimePicker.text = formatTime(selectedHour, selectedMinute)
                
                BackupScheduler(requireContext()).scheduleFridayBackup(LocalTime.of(selectedHour, selectedMinute))
            }, currentHour, currentMinute, false).show()
        }

        // Wi-Fi Only & Force Full Backup Logic
        switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_WIFI_ONLY, isChecked).apply()
        }

        switchForceFullBackup.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_FORCE_FULL_BACKUP, isChecked).apply()
            val msg = if (isChecked) "Force full backup enabled (Delta detection bypassed)." else "Incremental backup enabled (Delta detection active)."
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Category Switch Change Listeners
        switchCatDocs.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_DOCUMENTS, isChecked).apply()
        }
        switchCatImages.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_IMAGES, isChecked).apply()
        }
        switchCatVideo.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_VIDEO, isChecked).apply()
        }
        switchCatAudio.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_AUDIO, isChecked).apply()
        }
        switchCatVoiceNotes.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_CAT_VOICE_NOTES, isChecked).apply()
        }

        // Select All / Select None Shortcut Buttons
        btnSelectAll.setOnClickListener {
            switchCatDocs.isChecked = true
            switchCatImages.isChecked = true
            switchCatVideo.isChecked = true
            switchCatAudio.isChecked = true
            switchCatVoiceNotes.isChecked = true

            prefs.edit()
                .putBoolean(PREF_CAT_DOCUMENTS, true)
                .putBoolean(PREF_CAT_IMAGES, true)
                .putBoolean(PREF_CAT_VIDEO, true)
                .putBoolean(PREF_CAT_AUDIO, true)
                .putBoolean(PREF_CAT_VOICE_NOTES, true)
                .apply()

            Toast.makeText(requireContext(), "All categories selected", Toast.LENGTH_SHORT).show()
        }

        btnSelectNone.setOnClickListener {
            switchCatDocs.isChecked = false
            switchCatImages.isChecked = false
            switchCatVideo.isChecked = false
            switchCatAudio.isChecked = false
            switchCatVoiceNotes.isChecked = false

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
        etHistoryDays.doAfterTextChanged { editable ->
            val days = editable?.toString()?.toIntOrNull() ?: 30
            prefs.edit().putInt(PREF_HISTORY_DAYS, days).apply()
        }

        // Account Logic
        viewModel.googleAccount.observe(viewLifecycleOwner) { account ->
            if (account != null) {
                txtAccountEmail.text = account.email
                btnSignOut.isEnabled = true
            } else {
                txtAccountEmail.text = "Not signed in"
                btnSignOut.isEnabled = false
            }
        }

        btnSignOut.setOnClickListener {
            viewModel.signOut()
        }

        btnForceBackup.setOnClickListener {
            val workRequest = OneTimeWorkRequestBuilder<BackupWorker>().build()
            WorkManager.getInstance(requireContext()).enqueue(workRequest)
            Toast.makeText(requireContext(), "Backup triggered manually.", Toast.LENGTH_SHORT).show()
        }

        btnAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        return view
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        return time.format(formatter)
    }
}
