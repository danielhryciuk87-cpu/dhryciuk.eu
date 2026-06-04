package eu.dhryciuk.uploader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.dhryciuk.uploader.data.GitHubApi
import eu.dhryciuk.uploader.data.Publisher
import eu.dhryciuk.uploader.data.SecureTokenStore
import eu.dhryciuk.uploader.model.Album
import eu.dhryciuk.uploader.model.RepoSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val settings: RepoSettings = RepoSettings(),
    val tokenConfigured: Boolean = false,
    val showSettings: Boolean = false,
    val busy: Boolean = false,
    val albums: List<Album> = emptyList(),
    val selectedUris: List<Uri> = emptyList(),
    val selectedCategory: String = "przyroda",
    val selectedAlbumId: String? = null,
    val createNewAlbum: Boolean = false,
    val newAlbumTitle: String = "",
    val status: String = "",
    val progress: Float = 0f,
    val lastCommit: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = SecureTokenStore(application)
    private val _state = MutableStateFlow(
        AppUiState(
            settings = tokenStore.loadSettings(),
            tokenConfigured = tokenStore.hasToken(),
            showSettings = !tokenStore.hasToken()
        )
    )
    val state = _state.asStateFlow()

    init {
        if (tokenStore.hasToken()) refreshGallery()
    }

    fun openSettings() = _state.update { it.copy(showSettings = true, status = "") }
    fun closeSettings() = _state.update { it.copy(showSettings = false, status = "") }
    fun setCategory(value: String) = _state.update {
        it.copy(selectedCategory = value, selectedAlbumId = null)
    }
    fun setAlbum(value: String) = _state.update { it.copy(selectedAlbumId = value) }
    fun setCreateNew(value: Boolean) = _state.update {
        it.copy(createNewAlbum = value, selectedAlbumId = if (value) null else it.selectedAlbumId)
    }
    fun setNewAlbumTitle(value: String) = _state.update { it.copy(newAlbumTitle = value) }
    fun addUris(uris: List<Uri>) = _state.update { current ->
        current.copy(selectedUris = (current.selectedUris + uris).distinct(), status = "", lastCommit = null)
    }
    fun removeUri(uri: Uri) = _state.update { it.copy(selectedUris = it.selectedUris - uri) }
    fun clearSelection() = _state.update { it.copy(selectedUris = emptyList()) }

    fun saveSettings(settings: RepoSettings, enteredToken: String) {
        launchTask("Sprawdzam dostęp do repozytorium") {
            val token = enteredToken.trim().ifBlank { tokenStore.getToken() }
                ?: throw IllegalArgumentException("Wklej token GitHub.")
            val clean = settings.copy(
                owner = settings.owner.trim(),
                repo = settings.repo.trim(),
                branch = settings.branch.trim()
            )
            require(clean.owner.isNotBlank() && clean.repo.isNotBlank() && clean.branch.isNotBlank()) {
                "Uzupełnij właściciela, repozytorium i gałąź."
            }
            val api = GitHubApi(clean, token)
            api.testConnection()
            val gallery = api.getGallery()
            tokenStore.save(clean, enteredToken.trim().takeIf { it.isNotBlank() })
            _state.update {
                it.copy(
                    settings = clean,
                    tokenConfigured = true,
                    showSettings = false,
                    albums = gallery.albums,
                    selectedCategory = gallery.albums.firstOrNull()?.category ?: it.selectedCategory,
                    status = "Połączenie działa."
                )
            }
        }
    }

    fun refreshGallery() {
        launchTask("Odświeżam albumy") {
            val api = configuredApi()
            val gallery = api.getGallery()
            _state.update { current ->
                val category = current.selectedCategory.takeIf { selected ->
                    gallery.albums.any { it.category == selected }
                } ?: gallery.albums.firstOrNull()?.category ?: current.selectedCategory
                current.copy(albums = gallery.albums, selectedCategory = category, status = "Albumy są aktualne.")
            }
        }
    }

    fun publish() {
        val snapshot = _state.value
        launchTask("Przygotowuję publikację", keepStatusOnSuccess = true) {
            val publisher = Publisher(configuredApi(), getApplication<Application>().contentResolver)
            val commit = publisher.publish(
                uris = snapshot.selectedUris,
                existingAlbumId = if (snapshot.createNewAlbum) null else snapshot.selectedAlbumId,
                newAlbumTitle = snapshot.newAlbumTitle,
                category = snapshot.selectedCategory
            ) { message, progress ->
                _state.update { it.copy(status = message, progress = progress) }
            }
            val gallery = configuredApi().getGallery()
            _state.update {
                it.copy(
                    albums = gallery.albums,
                    selectedUris = emptyList(),
                    newAlbumTitle = "",
                    progress = 1f,
                    status = "Zdjęcia opublikowane. GitHub Pages odświeży stronę za chwilę.",
                    lastCommit = commit
                )
            }
        }
    }

    private fun configuredApi(): GitHubApi {
        val token = tokenStore.getToken() ?: throw IllegalStateException("Najpierw skonfiguruj token GitHub.")
        return GitHubApi(_state.value.settings, token)
    }

    private fun launchTask(
        initialStatus: String,
        keepStatusOnSuccess: Boolean = false,
        block: suspend () -> Unit
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, status = initialStatus, progress = 0f) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { error ->
                    _state.update {
                        it.copy(busy = false, progress = 0f, status = error.message ?: "Nieznany błąd.")
                    }
                }
                .onSuccess {
                    _state.update {
                        it.copy(
                            busy = false,
                            status = if (keepStatusOnSuccess) it.status else it.status,
                            progress = if (keepStatusOnSuccess) it.progress else 0f
                        )
                    }
                }
        }
    }
}
