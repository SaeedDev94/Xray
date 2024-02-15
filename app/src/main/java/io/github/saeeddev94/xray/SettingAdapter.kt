package io.github.saeeddev94.xray

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class SettingAdapter(
    private var context: Context,
    private var tabs: List<String>,
    private var layouts: List<Int>,
    private var callback: ViewsReady,
) : PagerAdapter() {

    private val views: MutableList<View> = mutableListOf()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(layouts[position], container, false)
        container.addView(view)
        views.add(view)
        if (views.size == tabs.size) {
            callback.rendered(views)
        }
        return view
    }

    override fun getCount(): Int {
        return tabs.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabs[position]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    interface ViewsReady {
        fun rendered(views: List<View>)
    }
}
