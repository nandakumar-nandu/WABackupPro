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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnTimePicker: Button = view.findViewById(R.id.btn_time_picker)
        val switchWifiOnly: MaterialSwitch = view.findViewById(R.id.switch_wifi_only)
        val etHistoryDays: EditText = view.findViewById(R.id.et_history_days)
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
        etHistoryDays.setText(prefs.getInt(PREF_HISTORY_DAYS, 30).toString())

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
                
                // Reschedule the WorkManager with the new time
                BackupScheduler(requireContext()).scheduleFridayBackup(LocalTime.of(selectedHour, selectedMinute))
            }, currentHour, currentMinute, false).show()
        }

        // Wi-Fi Only Logic
        switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_WIFI_ONLY, isChecked).apply()
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
            // Shortcut to trigger the backup worker immediately
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
