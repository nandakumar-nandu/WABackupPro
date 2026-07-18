package com.wabackuppro.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.wabackuppro.R
import com.wabackuppro.databinding.ActivityMainBinding
import com.wabackuppro.ui.history.BackupHistoryFragment
import com.wabackuppro.ui.settings.SettingsFragment
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * MainActivity is the single activity hosting the main navigation flows:
 * Backup, History, and Settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // 🛠️ Inflate and bind activity layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⚙️ Configure support action bar
        setSupportActionBar(binding.toolbar)

        // 🛠️ Set default fragment (BackupFragment)
        if (savedInstanceState == null) {
            loadFragment(BackupFragment())
        }

        // 👆 Handle bottom navigation selection events
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.navigation_backup -> BackupFragment()
                R.id.navigation_history -> BackupHistoryFragment()
                R.id.navigation_settings -> SettingsFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(fragment)
            true
        }
    }

    /**
     * Swaps the active fragment loaded inside the container view.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
