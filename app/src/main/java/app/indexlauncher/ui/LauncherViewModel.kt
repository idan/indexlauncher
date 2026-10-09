package app.indexlauncher.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.indexlauncher.data.AppIconRepository
import app.indexlauncher.data.InstalledAppsRepository
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LauncherUiState(
    val apps: List<AppEntry> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val iconCatalogVersion: Int = 0,
)

class LauncherViewModel(
    private val repository: InstalledAppsRepository,
    private val iconRepository: AppIconRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    suspend fun loadIcon(app: AppEntry, catalogVersion: Int, shape: IconShape): Bitmap? =
        iconRepository.loadIcon(app, catalogVersion, shape)

    fun refreshApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            try {
                val apps = repository.loadApps()
                iconRepository.clear()
                _uiState.update {
                    it.copy(apps = apps, isLoading = false, iconCatalogVersion = it.iconCatalogVersion + 1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }
}
