package eu.dhryciuk.uploader.data

import android.util.Base64
import eu.dhryciuk.uploader.model.GalleryIndex
import eu.dhryciuk.uploader.model.GalleryJson
import eu.dhryciuk.uploader.model.RepoSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TreeEntry(val path: String, val sha: String)

class GitHubApi(
    private val settings: RepoSettings,
    private val token: String
) {
    fun testConnection() {
        request("GET", "/repos/${settings.owner}/${settings.repo}")
    }

    fun getGallery(): GalleryIndex {
        val response = request(
            "GET",
            "/repos/${settings.owner}/${settings.repo}/contents/gallery.json?ref=${settings.branch}"
        )
        val encoded = JSONObject(response).getString("content").replace("\n", "")
        return GalleryJson.decode(String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8))
    }

    fun getHeadSha(): String {
        val response = request(
            "GET",
            "/repos/${settings.owner}/${settings.repo}/git/ref/heads/${settings.branch}"
        )
        return JSONObject(response).getJSONObject("object").getString("sha")
    }

    fun getTreeSha(commitSha: String): String {
        val response = request(
            "GET",
            "/repos/${settings.owner}/${settings.repo}/git/commits/$commitSha"
        )
        return JSONObject(response).getJSONObject("tree").getString("sha")
    }

    fun createBlob(bytes: ByteArray): String {
        val body = JSONObject()
            .put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
            .put("encoding", "base64")
        return JSONObject(
            request("POST", "/repos/${settings.owner}/${settings.repo}/git/blobs", body)
        ).getString("sha")
    }

    fun createTree(baseTreeSha: String, entries: List<TreeEntry>): String {
        val tree = JSONArray()
        entries.forEach { entry ->
            tree.put(
                JSONObject()
                    .put("path", entry.path)
                    .put("mode", "100644")
                    .put("type", "blob")
                    .put("sha", entry.sha)
            )
        }
        val body = JSONObject().put("base_tree", baseTreeSha).put("tree", tree)
        return JSONObject(
            request("POST", "/repos/${settings.owner}/${settings.repo}/git/trees", body)
        ).getString("sha")
    }

    fun createCommit(message: String, treeSha: String, parentSha: String): String {
        val body = JSONObject()
            .put("message", message)
            .put("tree", treeSha)
            .put("parents", JSONArray().put(parentSha))
        return JSONObject(
            request("POST", "/repos/${settings.owner}/${settings.repo}/git/commits", body)
        ).getString("sha")
    }

    fun updateBranch(commitSha: String) {
        val body = JSONObject().put("sha", commitSha).put("force", false)
        request(
            "PATCH",
            "/repos/${settings.owner}/${settings.repo}/git/refs/heads/${settings.branch}",
            body
        )
    }

    private fun request(method: String, path: String, body: JSONObject? = null): String {
        val connection = (URL("https://api.github.com$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "dhryciuk-android-uploader")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) {
            val message = runCatching { JSONObject(response).optString("message") }.getOrNull()
            throw GitHubApiException(status, message?.takeIf { it.isNotBlank() } ?: "Błąd GitHub API")
        }
        return response
    }
}

class GitHubApiException(val status: Int, message: String) : Exception("$message (HTTP $status)")
