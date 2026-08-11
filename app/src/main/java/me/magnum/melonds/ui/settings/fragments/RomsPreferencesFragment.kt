package me.magnum.melonds.ui.settings.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.magnum.melonds.R
import me.magnum.melonds.common.DirectoryAccessValidator
import me.magnum.melonds.common.UriPermissionManager
import me.magnum.melonds.domain.model.SizeUnit
import me.magnum.melonds.impl.SettingsBackupManager
import me.magnum.melonds.impl.EnhancementCatalogLoader
import me.magnum.enhancements.verificationSummary
import me.magnum.melonds.ui.settings.PreferenceFragmentHelper
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider
import me.magnum.melonds.ui.settings.preferences.StoragePickerPreference
import me.magnum.melonds.ui.settings.viewmodel.RomPreferencesViewModel
import me.magnum.melonds.utils.SizeUtils
import javax.inject.Inject
import kotlin.math.pow

@AndroidEntryPoint
class RomsPreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    private val viewModel by viewModels<RomPreferencesViewModel>()
    private val helper by lazy { PreferenceFragmentHelper(this, uriPermissionManager, directoryAccessValidator) }
    @Inject lateinit var uriPermissionManager: UriPermissionManager
    @Inject lateinit var directoryAccessValidator: DirectoryAccessValidator
    @Inject lateinit var settingsBackupManager: SettingsBackupManager
    @Inject lateinit var enhancementCatalogLoader: EnhancementCatalogLoader

    private lateinit var clearRomCachePreference: Preference
    private lateinit var importEnhancementPreference: Preference

    private val enhancementImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        importEnhancement(uri)
    }

    override fun getTitle() = getString(R.string.category_roms)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_roms, rootKey)
        val cacheSizePreference = findPreference<SeekBarPreference>("rom_cache_max_size")!!
        clearRomCachePreference = findPreference("rom_cache_clear")!!
        importEnhancementPreference = findPreference("enhancement_import")!!

        helper.setupStoragePickerPreference(findPreference<StoragePickerPreference>("rom_search_dirs")!!) { uri, persistDirectory ->
            handleSettingsMirror(uri, persistDirectory)
        }

        updateMaxCacheSizePreferenceSummary(cacheSizePreference, cacheSizePreference.value)

        cacheSizePreference.setOnPreferenceChangeListener { preference, newValue ->
            updateMaxCacheSizePreferenceSummary(preference as SeekBarPreference, newValue as Int)
            true
        }
        clearRomCachePreference.setOnPreferenceClickListener {
            if (!viewModel.clearRomCache()) {
                Toast.makeText(requireContext(), R.string.error_clear_rom_cache, Toast.LENGTH_LONG).show()
            }
            true
        }
        importEnhancementPreference.setOnPreferenceClickListener {
            enhancementImportLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            true
        }
    }

    private fun importEnhancement(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val incoming = withContext(Dispatchers.IO) {
                    enhancementCatalogLoader.inspect(uri)
                }
            val existing = enhancementCatalogLoader.load().find(incoming.id)
            if (existing != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.replace_enhancement_title)
                    .setMessage(getString(R.string.replace_enhancement_message, existing.name, existing.version))
                    .setPositiveButton(R.string.replace_enhancement) { _, _ -> installEnhancement(uri, true, incoming.id) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                installEnhancement(uri, false, incoming.id)
            }
            } catch (error: Exception) {
                showEnhancementImportResult(false, error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private fun installEnhancement(uri: Uri, replace: Boolean, inspectedId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
            val manifest = withContext(Dispatchers.IO) {
                enhancementCatalogLoader.install(uri, replace)
            }
            check(manifest.id == inspectedId) { "Inspected enhancement package changed" }
            val catalogSize = enhancementCatalogLoader.load().manifests.size
            showEnhancementImportResult(
                true,
                getString(
                    R.string.enhancement_import_success,
                    manifest.id,
                    manifest.version,
                    manifest.distributionStatus,
                    manifest.verificationSummary(),
                    catalogSize,
                ),
            )
            } catch (error: Exception) {
                showEnhancementImportResult(false, error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private fun showEnhancementImportResult(success: Boolean, message: String) {
        Toast.makeText(
            requireContext(),
            if (success) message else getString(R.string.enhancement_import_failed, message),
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.romCacheSize.collect {
                    val cacheSizeRepresentation = SizeUtils.getBestSizeStringRepresentation(requireContext(), it)
                    clearRomCachePreference.summary = getString(R.string.cache_size, cacheSizeRepresentation)
                }
            }
        }
    }

    private fun updateMaxCacheSizePreferenceSummary(maxCacheSizePreference: SeekBarPreference, cacheSizeStep: Int) {
        val cacheSize = SizeUnit.MB(128) * 2.toDouble().pow(cacheSizeStep).toLong()
        maxCacheSizePreference.summary = SizeUtils.getBestSizeStringRepresentation(requireContext(), cacheSize)
    }

    private fun handleSettingsMirror(uri: Uri, persistDirectory: () -> Unit) {
        if (!settingsBackupManager.isMirrorEnabled()) {
            persistDirectory()
            return
        }

        if (!settingsBackupManager.hasMirrorAt(uri)) {
            settingsBackupManager.rememberMirrorFallback(uri)
            persistDirectory()
            settingsBackupManager.requestMirrorWrite()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_mirror_detected_title)
            .setMessage(R.string.settings_mirror_detected_message)
            .setPositiveButton(R.string.settings_mirror_restore) { _, _ ->
                settingsBackupManager.rememberMirrorFallback(uri)
                settingsBackupManager.restoreMirrorFrom(uri)
                persistDirectory()
                settingsBackupManager.requestMirrorWrite()
            }
            .setNegativeButton(R.string.settings_mirror_ignore) { _, _ ->
                settingsBackupManager.rememberMirrorFallback(uri)
                persistDirectory()
                settingsBackupManager.overwriteMirrorAt(uri)
                settingsBackupManager.requestMirrorWrite()
            }
            .show()
    }
}
