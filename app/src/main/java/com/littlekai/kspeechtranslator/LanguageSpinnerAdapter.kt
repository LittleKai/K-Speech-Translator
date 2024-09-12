package com.littlekai.kspeechtranslator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.littlekai.kspeechtranslator.R

class LanguageSpinnerAdapter(context: Context, items: List<LanguageItem>) :
    ArrayAdapter<LanguageItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = recycledView ?: LayoutInflater.from(context).inflate(
            R.layout.spinner_item_language, parent, false
        )

        val languageText = view.findViewById<TextView>(R.id.languageText)
        val statusIcon = view.findViewById<ImageView>(R.id.statusIcon)

        languageText.text = item?.name
//        statusIcon.setVisibility
//            if (item?.isDownloaded == true) R.drawable.ic_check
//            else R.drawable.ic_download
//        )

        return view
    }
}