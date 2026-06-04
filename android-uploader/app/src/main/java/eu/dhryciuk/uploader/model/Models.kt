package eu.dhryciuk.uploader.model

import org.json.JSONArray
import org.json.JSONObject

data class RepoSettings(
    val owner: String = "danielhryciuk87-cpu",
    val repo: String = "dhryciuk.eu",
    val branch: String = "main"
)

data class PhotoEntry(
    val src: String,
    val thumbnail: String,
    val alt: String,
    val width: Int,
    val height: Int
)

data class Album(
    val id: String,
    val title: String,
    val category: String,
    val source: String = "",
    val photos: List<PhotoEntry> = emptyList()
)

data class GalleryIndex(
    val generatedAt: String,
    val albums: List<Album>
)

object GalleryJson {
    fun decode(raw: String): GalleryIndex {
        val root = JSONObject(raw)
        val albumsJson = root.optJSONArray("albums") ?: JSONArray()
        val albums = buildList {
            for (index in 0 until albumsJson.length()) {
                val albumJson = albumsJson.getJSONObject(index)
                val photosJson = albumJson.optJSONArray("photos") ?: JSONArray()
                val photos = buildList {
                    for (photoIndex in 0 until photosJson.length()) {
                        val photo = photosJson.getJSONObject(photoIndex)
                        add(
                            PhotoEntry(
                                src = photo.getString("src"),
                                thumbnail = photo.getString("thumbnail"),
                                alt = photo.optString("alt", albumJson.optString("title")),
                                width = photo.optInt("width"),
                                height = photo.optInt("height")
                            )
                        )
                    }
                }
                add(
                    Album(
                        id = albumJson.getString("id"),
                        title = albumJson.optString("title", albumJson.getString("id")),
                        category = albumJson.optString("category", "pozostale"),
                        source = albumJson.optString("source"),
                        photos = photos
                    )
                )
            }
        }
        return GalleryIndex(root.optString("generatedAt"), albums)
    }

    fun encode(index: GalleryIndex): String {
        val root = JSONObject()
        root.put("generatedAt", index.generatedAt)
        root.put("albums", JSONArray().apply {
            index.albums.forEach { album ->
                put(JSONObject().apply {
                    put("id", album.id)
                    put("title", album.title)
                    put("category", album.category)
                    put("source", album.source)
                    put("photos", JSONArray().apply {
                        album.photos.forEach { photo ->
                            put(JSONObject().apply {
                                put("src", photo.src)
                                put("thumbnail", photo.thumbnail)
                                put("alt", photo.alt)
                                put("width", photo.width)
                                put("height", photo.height)
                            })
                        }
                    })
                })
            }
        })
        return root.toString(2) + "\n"
    }
}
