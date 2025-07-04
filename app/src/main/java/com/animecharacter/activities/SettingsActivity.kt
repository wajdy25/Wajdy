package com.animecharacter.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import android.content.pm.PackageManager
import com.animecharacter.R
import com.animecharacter.databinding.ActivitySettingsBinding
import com.animecharacter.utils.PreferencesHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings_title)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var preferencesHelper: PreferencesHelper

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            preferencesHelper = PreferencesHelper(requireContext())
            
            setupPreferences()
        }

        private fun setupPreferences() {
            setupCharacterPreferences()
            setupVoicePreferences()
            setupAIPreferences()
            setupPrivacyPreferences()
            setupWakeWordPreferences()
            setupGameDetectionPreferences()
        }

        private fun setupCharacterPreferences() {
            // اختيار الشخصية
            findPreference<ListPreference>("selected_character")?.apply {
                entries = preferencesHelper.getAvailableCharacters().toTypedArray()
                entryValues = preferencesHelper.getAvailableCharacters().toTypedArray()
                value = preferencesHelper.getSelectedCharacter()
                summary = value
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setSelectedCharacter(newValue as String)
                    summary = newValue
                    true
                }
            }

            // حجم الشخصية
            findPreference<SeekBarPreference>("character_size")?.apply {
                value = preferencesHelper.getCharacterSize()
                min = 80
                max = 200
                showSeekBarValue = true
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setCharacterSize(newValue as Int)
                    true
                }
            }

            // شفافية الشخصية
            findPreference<SeekBarPreference>("character_transparency")?.apply {
                value = preferencesHelper.getCharacterTransparency()
                min = 30
                max = 100
                showSeekBarValue = true
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setCharacterTransparency(newValue as Int)
                    true
                }
            }
        }

        private fun setupVoicePreferences() {
            // نوع الصوت/اللغة
            findPreference<ListPreference>("voice_language")?.apply {
                val languages = preferencesHelper.getAvailableLanguages()
                entries = languages.values.toTypedArray()
                entryValues = languages.keys.toTypedArray()
                value = preferencesHelper.getVoiceLanguage()
                summary = languages[value]
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setVoiceLanguage(newValue as String)
                    summary = languages[newValue]
                    true
                }
            }

            // اللهجة
            findPreference<ListPreference>("selected_dialect")?.apply {
                val dialects = preferencesHelper.getAvailableDialects()
                entries = dialects.values.toTypedArray()
                entryValues = dialects.keys.toTypedArray()
                value = preferencesHelper.getSelectedDialect()
                summary = dialects[value]

                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setSelectedDialect(newValue as String)
                    summary = dialects[newValue]
                    true
                }
            }

            // سرعة الكلام
            findPreference<SeekBarPreference>("speech_rate")?.apply {
                value = (preferencesHelper.getSpeechRate() * 100).toInt()
                min = 50
                max = 200
                showSeekBarValue = true
                
                setOnPreferenceChangeListener { _, newValue ->
                    val rate = (newValue as Int) / 100f
                    preferencesHelper.setSpeechRate(rate)
                    true
                }
            }

            // نبرة الصوت
            findPreference<SeekBarPreference>("speech_pitch")?.apply {
                value = (preferencesHelper.getSpeechPitch() * 100).toInt()
                min = 50
                max = 200
                showSeekBarValue = true
                
                setOnPreferenceChangeListener { _, newValue ->
                    val pitch = (newValue as Int) / 100f
                    preferencesHelper.setSpeechPitch(pitch)
                    true
                }
            }
        }

        private fun setupAIPreferences() {
            // نمط الشخصية
            findPreference<ListPreference>("personality_mode")?.apply {
                entries = preferencesHelper.getAvailablePersonalities().toTypedArray()
                entryValues = preferencesHelper.getAvailablePersonalities().toTypedArray()
                value = preferencesHelper.getPersonalityMode()
                summary = value
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setPersonalityMode(newValue as String)
                    summary = newValue
                    true
                }
            }
        }

        private fun setupPrivacyPreferences() {
            // حفظ المحادثات
            findPreference<SwitchPreferenceCompat>("save_conversations")?.apply {
                isChecked = preferencesHelper.getSaveConversations()
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setSaveConversations(newValue as Boolean)
                    true
                }
            }

            // إشعارات الميكروفون
            findPreference<SwitchPreferenceCompat>("microphone_notifications")?.apply {
                isChecked = preferencesHelper.getMicrophoneNotifications()
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setMicrophoneNotifications(newValue as Boolean)
                    true
                }
            }

            // تشفير البيانات
            findPreference<SwitchPreferenceCompat>("data_encryption")?.apply {
                isChecked = preferencesHelper.getDataEncryption()
                
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setDataEncryption(newValue as Boolean)
                    true
                }
            }

            // إعادة تعيين الإعدادات
            findPreference<Preference>("reset_settings")?.apply {
                setOnPreferenceClickListener {
                    showResetConfirmationDialog()
                    true
                }
            }

            // تصدير الإعدادات
            findPreference<Preference>("export_settings")?.apply {
                setOnPreferenceClickListener {
                    exportSettings()
                    true
                }
            }
        }

        private fun setupWakeWordPreferences() {
            findPreference<SwitchPreferenceCompat>("is_wake_word_enabled")?.apply {
                isChecked = preferencesHelper.isWakeWordEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setWakeWordEnabled(newValue as Boolean)
                    true
                }
            }

            findPreference<EditTextPreference>("wake_word")?.apply {
                text = preferencesHelper.getWakeWord()
                summary = preferencesHelper.getWakeWord().ifEmpty { getString(R.string.wake_word_summary) }
                setOnPreferenceChangeListener { _, newValue ->
                    val newWakeWord = newValue as String
                    preferencesHelper.setWakeWord(newWakeWord)
                    summary = newWakeWord.ifEmpty { getString(R.string.wake_word_summary) }
                    true
                }
            }
        }

        private fun setupGameDetectionPreferences() {
            findPreference<SwitchPreferenceCompat>("hide_on_game_enabled")?.apply {
                isChecked = preferencesHelper.isHideOnGameEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesHelper.setHideOnGameEnabled(newValue as Boolean)
                    true
                }
            }

            findPreference<MultiSelectListPreference>("game_package_names")?.apply {
                val installedApps = getInstalledApplications()
                entries = installedApps.map { it.first }.toTypedArray()
                entryValues = installedApps.map { it.second }.toTypedArray()
                
                values = preferencesHelper.getGamePackageNames()
                summary = if (values.isEmpty()) {
                    getString(R.string.game_package_names_summary)
                } else {
                    "${values.size} " + getString(R.string.selected_games)
                }

                setOnPreferenceChangeListener { _, newValue ->
                    val newValues = newValue as Set<String>
                    preferencesHelper.setGamePackageNames(newValues)
                    summary = if (newValues.isEmpty()) {
                        getString(R.string.game_package_names_summary)
                    } else {
                        "${newValues.size} " + getString(R.string.selected_games)
                    }
                    true
                }
            }
        }

        private fun getInstalledApplications(): List<Pair<String, String>> {
            val pm = requireContext().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<Pair<String, String>>()
            for (app in apps) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null) { // Only include launchable apps
                    val appName = pm.getApplicationLabel(app).toString()
                    appList.add(Pair(appName, app.packageName))
                }
            }
            appList.sortBy { it.first.lowercase() }
            return appList
        }

