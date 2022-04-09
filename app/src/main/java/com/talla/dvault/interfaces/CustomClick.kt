package com.talla.dvault.interfaces

import com.talla.dvault.models.CustomItemModel

interface CustomClick
{
    fun itemClick(itemsList:MutableSet<CustomItemModel>)
}