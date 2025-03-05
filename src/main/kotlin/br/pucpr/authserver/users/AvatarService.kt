package br.pucpr.authserver.users

import br.pucpr.authserver.arquivos.S3Storage
import br.pucpr.authserver.errors.UnsupportedMediaTypeException
import com.amazonaws.util.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.security.MessageDigest
import javax.imageio.ImageIO

@Service
class AvatarService(val storage: S3Storage) {
    fun save(user: User, avatar: MultipartFile): String =
        try {
            val extensao = when (avatar.contentType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                else -> throw UnsupportedMediaTypeException("jpg, png")
            }

            val path = "${user.id}/av_${user.id}.$extensao"
            storage.save(user, "$ROOT/$path", avatar)
            path
        } catch (exception: Exception) {
            log.error("Unable to save avatar for ${user.id}: ${exception.message}")
            fetchAvatar(user)
        }

    fun urlFor(user: User): String {
        if (user.avatar.isBlank()) {
            user.avatar = fetchAvatar(user)
        }
        return storage.urlFor("$ROOT/${user.avatar}")
    }

    private fun fetchAvatar(user: User): String {
        val emailHash = hashSHA256(user.email.trim().lowercase())
        val gravatarUrl = "https://www.gravatar.com/avatar/${emailHash}?d=404"
        val url = try {
            URI(gravatarUrl).toURL().openStream().use { gravatarUrl }
        } catch (e: Exception) {
            "https://ui-avatars.com/api/?name=${user.name.replace(" ", "+")}&background=random&format=png"
        }

        val image = ImageIO.read(URI(url).toURL()) ?: throw Exception("Failed to read image from URL")
        val path = "${user.id}/av_${user.id}.png"
        val tempFile = java.io.File.createTempFile("avatar", ".png").apply {
            ImageIO.write(image, "png", this)
        }

        val fileContent = IOUtils.toByteArray(tempFile.inputStream())
        val multipartFile = object : MultipartFile {
            override fun getName(): String = "avatar"
            override fun getOriginalFilename(): String? = tempFile.name
            override fun getContentType(): String = "image/png"
            override fun isEmpty(): Boolean = fileContent.isEmpty()
            override fun getSize(): Long = fileContent.size.toLong()
            override fun getBytes(): ByteArray = fileContent
            override fun getInputStream(): java.io.InputStream = tempFile.inputStream()
            override fun transferTo(dest: java.io.File) {
                tempFile.inputStream().use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        storage.save(user, "$ROOT/$path", multipartFile)
        tempFile.delete()
        return path
    }

    private fun hashSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AvatarService::class.java)
        const val ROOT = "avatars"
    }
}