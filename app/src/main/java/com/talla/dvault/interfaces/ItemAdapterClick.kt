package com.talla.dvault.interfaces

import com.talla.dvault.database.entities.ItemModel

interface ItemAdapterClick
{
    fun onItemClick(myItemIdsSet: MutableSet<Int>)

    fun deleteParticularItem(itemModel:ItemModel)

    fun unlockParticularItem(itemModel:ItemModel)

}