package com.talla.dvault.utills

import com.talla.dvault.R
import com.talla.dvault.database.entities.CategoriesModel

class DataGenerator
{
 companion object{
     fun getDefaultCatList():List<CategoriesModel>{
         var catType="application/vnd.google-apps.folder"
         var catType2="file/*"
         return listOf(
             CategoriesModel("Img","Images","",catType),
             CategoriesModel("Vdo","Videos","",catType),
             CategoriesModel("Aud","Audios","",catType),
             CategoriesModel("Doc","Docs","",catType),
             CategoriesModel("DB","DataBases","",catType),
             CategoriesModel("DVault.db","DVault.db","",catType2),
             CategoriesModel("DVault.db-wal","DVault.db-wal","",catType2),
             CategoriesModel("DVault.db-shm","DVault.db-shm","",catType2)

         )
     }
 }
}
