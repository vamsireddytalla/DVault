package com.talla.dvault.database.entities

import java.io.Serializable

data class SourcesModel(var sourceFilePath:String,
                        var fileName:String,
                        var fileSize:Long,
                        var fileMimeType:String,
                        var folderTable: FolderTable):Serializable
