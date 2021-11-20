package com.talla.dvault.utills

import com.talla.dvault.R
import com.talla.dvault.database.entities.CategoriesModel

class DataGenerator
{
 companion object{
     fun getDefaultCatList():List<CategoriesModel>{
         val catType="application/vnd.google-apps.folder"
         var catType2="file/*"
         return listOf(
             CategoriesModel("Img","Images",catType,"",""),
             CategoriesModel("Vdo","Videos",catType,"",""),
             CategoriesModel("Aud","Audios",catType,"",""),
             CategoriesModel("Doc","Docs",catType,"","")
         )
     }
 }
}
