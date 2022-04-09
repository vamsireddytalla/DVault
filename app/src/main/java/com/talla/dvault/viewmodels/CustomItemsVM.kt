package com.talla.dvault.viewmodels

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import com.talla.dvault.models.CustomItemModel
import com.talla.dvault.repositories.VaultRepository
import com.talla.dvault.utills.sdk30AndUp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


private const val TAG = "CustomItemsVM"
@HiltViewModel
class CustomItemsVM @Inject constructor(
    private val repository: VaultRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    suspend fun loadItems(): List<CustomItemModel> {
        val collection = sdk30AndUp {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val photos = mutableListOf<CustomItemModel>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        return withContext(Dispatchers.IO) {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val columnId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mime_type = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val date = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(columnId)
                    val displayName = cursor.getString(displayName)
                    val mime = cursor.getString(mime_type)
                    val size = cursor.getInt(size)
                    val contentUris: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val dateCreated=cursor.getLong(date)
                    photos.add(CustomItemModel(id,displayName,mime,contentUris.toString(),size,dateCreated.toString()))
                }
                photos.toList()
            } ?: listOf<CustomItemModel>()
        }
    }



}