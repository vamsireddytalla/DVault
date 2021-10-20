package com.talla.dvault.utills

import com.talla.dvault.database.entities.CategoriesModel

class DataGenerator
{
 companion object{
     fun getDefaultCatList():List<CategoriesModel>{
         return listOf(
             CategoriesModel("Img","Images",0),
             CategoriesModel("Vdo","Videos",0),
             CategoriesModel("Aud","Audios",0),
             CategoriesModel("Doc","Docs",0)
         )
     }
 }
}
