package com.polar.androidblesdk

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.polar.sdk.api.errors.PolarInvalidArgument

class CustomAdapter(private val context: Context, private val items: List<String>, private val clickListener: (Int) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

//        val itemText = view.findViewById<TextView>(R.id.item_text)
        val itemButton = view.findViewById<Button>(R.id.text_item)

        itemButton.text = items[position]

        itemButton.setOnClickListener {
            Toast.makeText(context, "Button clicked at position: $position", Toast.LENGTH_SHORT).show()
            // Handle button click logic here
            clickListener(position)

        }

        return view
    }
}
