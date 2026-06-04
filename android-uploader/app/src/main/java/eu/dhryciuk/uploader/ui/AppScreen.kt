package eu.dhryciuk.uploader.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.dhryciuk.uploader.model.RepoSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun DHUploaderApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.showSettings) {
        SettingsScreen(state, viewModel)
    } else {
        UploadScreen(state, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadScreen(state: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) {
        viewModel.addUris(it)
    }
    val categories = (state.albums.map { it.category } + state.selectedCategory).distinct().sorted()
    val categoryAlbums = state.albums.filter { it.category == state.selectedCategory }
    val canPublish = state.selectedUris.isNotEmpty() &&
        (state.createNewAlbum && state.newAlbumTitle.isNotBlank() || !state.createNewAlbum && state.selectedAlbumId != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DH Uploader", fontWeight = FontWeight.SemiBold)
                        Text("dhryciuk.eu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshGallery, enabled = !state.busy) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież albumy")
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dhryciuk.eu/gallery.html")))
                    }) {
                        Icon(Icons.Default.Language, contentDescription = "Otwórz stronę")
                    }
                    IconButton(onClick = viewModel::openSettings, enabled = !state.busy) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionTitle("1. Wybierz folder")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.displayName()) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.createNewAlbum,
                        onClick = { viewModel.setCreateNew(false) },
                        label = { Text("Istniejący album") }
                    )
                    FilterChip(
                        selected = state.createNewAlbum,
                        onClick = { viewModel.setCreateNew(true) },
                        label = { Text("Nowy album") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.createNewAlbum) {
                    OutlinedTextField(
                        value = state.newAlbumTitle,
                        onValueChange = viewModel::setNewAlbumTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nazwa nowego albumu") },
                        singleLine = true
                    )
                } else if (categoryAlbums.isEmpty()) {
                    Text("W tej kategorii nie ma jeszcze albumów.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryAlbums, key = { it.id }) { album ->
                            FilterChip(
                                selected = state.selectedAlbumId == album.id,
                                onClick = { viewModel.setAlbum(album.id) },
                                label = { Text("${album.title} · ${album.photos.size}") }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("2. Dodaj zdjęcia")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.selectedUris.isEmpty()) "Wybierz zdjęcia" else "Dodaj kolejne zdjęcia")
                }
                if (state.selectedUris.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wybrano: ${state.selectedUris.size}")
                        OutlinedButton(onClick = viewModel::clearSelection, enabled = !state.busy) {
                            Text("Wyczyść")
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(96.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.selectedUris, key = { it.toString() }) { uri ->
                            SelectedPhoto(uri, enabled = !state.busy) { viewModel.removeUri(uri) }
                        }
                    }
                }
            }

            item {
                SectionTitle("3. Opublikuj")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = viewModel::publish,
                    enabled = canPublish && !state.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Opublikuj na stronie")
                }
                if (state.busy || state.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
                if (state.status.isNotBlank()) {
                    Text(
                        state.status,
                        modifier = Modifier.padding(top = 10.dp),
                        color = if (state.lastCommit != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(state: AppUiState, viewModel: AppViewModel) {
    var owner by remember(state.settings.owner) { mutableStateOf(state.settings.owner) }
    var repo by remember(state.settings.repo) { mutableStateOf(state.settings.repo) }
    var branch by remember(state.settings.branch) { mutableStateOf(state.settings.branch) }
    var token by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Połączenie z GitHub") },
                navigationIcon = {
                    if (state.tokenConfigured) {
                        IconButton(onClick = viewModel::closeSettings, enabled = !state.busy) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Aplikacja zapisuje token wyłącznie w zaszyfrowanym magazynie telefonu. Token powinien mieć dostęp „Contents: Read and write” tylko do repozytorium strony.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(owner, { owner = it }, Modifier.fillMaxWidth(), label = { Text("Właściciel repozytorium") }, singleLine = true)
            }
            item {
                OutlinedTextField(repo, { repo = it }, Modifier.fillMaxWidth(), label = { Text("Repozytorium") }, singleLine = true)
            }
            item {
                OutlinedTextField(branch, { branch = it }, Modifier.fillMaxWidth(), label = { Text("Gałąź") }, singleLine = true)
            }
            item {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (state.tokenConfigured) "Nowy token (zostaw puste, aby zachować)" else "Fine-grained token GitHub") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = { viewModel.saveSettings(RepoSettings(owner, repo, branch), token) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Sprawdź i zapisz")
                }
                if (state.busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
                if (state.status.isNotBlank()) {
                    Text(state.status, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectedPhoto(uri: Uri, enabled: Boolean, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = 320f / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier.size(104.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription = "Wybrane zdjęcie", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier.align(Alignment.TopEnd).size(36.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Usuń zdjęcie", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

private fun String.displayName(): String = replace('-', ' ')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl", "PL")) else it.toString() }
