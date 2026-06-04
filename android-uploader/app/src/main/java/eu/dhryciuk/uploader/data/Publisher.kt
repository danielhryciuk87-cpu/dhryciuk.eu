package eu.dhryciuk.uploader.data

import android.content.ContentResolver
import android.net.Uri
import eu.dhryciuk.uploader.model.Album
import eu.dhryciuk.uploader.model.GalleryIndex
import eu.dhryciuk.uploader.model.GalleryJson
import eu.dhryciuk.uploader.model.PhotoEntry
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class Publisher(
    private val api: GitHubApi,
    resolver: ContentResolver
) {
    private val processor = ImageProcessor(resolver)

    fun publish(
        uris: List<Uri>,
        existingAlbumId: String?,
        newAlbumTitle: String,
        category: String,
        progress: (String, Float) -> Unit
    ): String {
        require(uris.isNotEmpty()) { "Najpierw wybierz zdjęcia." }
        progress("Pobieram aktualną galerię", 0.03f)
        val gallery = api.getGallery()
        val existing = existingAlbumId?.let { id -> gallery.albums.firstOrNull { it.id == id } }
        val title = existing?.title ?: newAlbumTitle.trim().ifBlank { throw IllegalArgumentException("Podaj nazwę nowego albumu.") }
        val albumCategory = existing?.category ?: slug(category).ifBlank { "pozostale" }
        val albumId = existing?.id ?: uniqueAlbumId(gallery, title, albumCategory)
        val treeEntries = mutableListOf<TreeEntry>()
        val addedPhotos = mutableListOf<PhotoEntry>()
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        uris.forEachIndexed { index, uri ->
            val start = 0.08f + index.toFloat() / uris.size * 0.72f
            progress("Przetwarzam zdjęcie ${index + 1} z ${uris.size}", start)
            val image = processor.process(uri)
            val name = "$stamp-${(index + 1).toString().padStart(3, '0')}.webp"
            val base = "galleries/$albumCategory/$albumId"
            val fullPath = "$base/photos/$name"
            val thumbPath = "$base/thumbs/$name"
            progress("Wysyłam zdjęcie ${index + 1} z ${uris.size}", start + 0.03f)
            treeEntries += TreeEntry(fullPath, api.createBlob(image.full))
            treeEntries += TreeEntry(thumbPath, api.createBlob(image.thumbnail))
            addedPhotos += PhotoEntry(
                src = fullPath,
                thumbnail = thumbPath,
                alt = "$title - zdjęcie ${existing?.photos?.size.orZero() + index + 1}",
                width = image.width,
                height = image.height
            )
        }

        progress("Aktualizuję indeks galerii", 0.84f)
        val updatedAlbum = (existing ?: Album(albumId, title, albumCategory)).copy(
            photos = existing?.photos.orEmpty() + addedPhotos
        )
        val updatedAlbums = if (existing == null) {
            gallery.albums + updatedAlbum
        } else {
            gallery.albums.map { if (it.id == existing.id) updatedAlbum else it }
        }
        val updatedGallery = GalleryIndex(Instant.now().toString(), updatedAlbums)
        treeEntries += TreeEntry("gallery.json", api.createBlob(GalleryJson.encode(updatedGallery).toByteArray()))

        progress("Tworzę publikację", 0.92f)
        val parentSha = api.getHeadSha()
        val baseTreeSha = api.getTreeSha(parentSha)
        val treeSha = api.createTree(baseTreeSha, treeEntries)
        val commitSha = api.createCommit("Dodaj ${uris.size} zdjęć do albumu $title", treeSha, parentSha)
        api.updateBranch(commitSha)
        progress("Gotowe", 1f)
        return commitSha
    }

    private fun uniqueAlbumId(gallery: GalleryIndex, title: String, category: String): String {
        val base = slug(title).ifBlank { "album" }
        val existingIds = gallery.albums.map { it.id }.toSet()
        if (base !in existingIds) return base
        var suffix = 2
        while ("$base-$suffix" in existingIds) suffix++
        return "$base-$suffix"
    }

    private fun slug(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    private fun Int?.orZero() = this ?: 0
}
