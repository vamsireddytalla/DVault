package com.talla.dvault.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.talla.dvault.R

class SpinnerAdapter(var questionList:List<String>,var context:Context) : BaseAdapter()
{

    override fun getCount(): Int {
        return questionList.size
    }

    override fun getItem(position: Int): Any {
        return questionList.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = View.inflate(context, R.layout.custom_spinner, null)
        val spinTitle = view.findViewById<TextView>(R.id.spinItem)
        val description: String = questionList.get(position)
        spinTitle.text = description
        return view
    }

}