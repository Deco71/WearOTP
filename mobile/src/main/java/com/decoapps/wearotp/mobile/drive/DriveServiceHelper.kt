package com.decoapps.wearotp.mobile.drive

import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DriveServiceHelper(private val driveService: Drive) {

    /**
     * Creates a text file in the user's My Drive folder.
     */
    suspend fun createTextFile(file: java.io.File): String? {
       return withContext(Dispatchers.IO) {
           val fileMetadata = File().apply {
               name = "my_app_backup.db"
               // This line is crucial: it targets the hidden folder
               parents = listOf("appDataFolder")
           }

           // 2. Define Content
           val mediaContent = FileContent("text/plain", file)

           // 3. Execute Upload
           val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
               .setFields("id")
               .execute()

           println("Backup created with ID: ${uploadedFile.id}")
           uploadedFile.id
        }
    }

    suspend fun listAppDataFiles(): List<File> {
        return withContext(Dispatchers.IO) {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()
            result.files ?: emptyList()
        }
    }

    companion object {

        fun getDriveService(accessToken: String): Drive {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            // Create the Drive service using the token directly
            val driveService = Drive.Builder(
                transport,
                jsonFactory,
                { request ->
                    // This manually adds the token to every Drive request
                    request.headers.authorization = "Bearer $accessToken"
                }
            ).setApplicationName("WearOTP").build()

            return driveService
        }
    }
}
