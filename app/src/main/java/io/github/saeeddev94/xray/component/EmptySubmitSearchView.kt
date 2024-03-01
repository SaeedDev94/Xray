package io.github.saeeddev94.xray.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SearchView

class EmptySubmitSearchView : SearchView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @SuppressLint("RestrictedApi")
    override fun setOnQueryTextListener(listener: OnQueryTextListener?) {
        super.setOnQueryTextListener(listener)
        val searchAutoComplete = this.findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete.setOnEditorActionListener { _, _, _ ->
            listener?.onQueryTextSubmit(query.toString())
            return@setOnEditorActionListener true
        }
    }

}
