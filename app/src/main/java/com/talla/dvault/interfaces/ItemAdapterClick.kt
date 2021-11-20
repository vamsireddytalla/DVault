package com.talla.dvault.interfaces

import com.talla.dvault.database.entities.ItemModel

interface ItemAdapterClick
{
    fun onItemClick(myItemIdsSet: MutableSet<ItemModel>)

    fun deleteParticularItem(itemModel:ItemModel,pos:Int)

    fun unlockParticularItem(itemModel:ItemModel)

}