package io.github.saeeddev94.xray.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import io.github.saeeddev94.xray.R

class ConfigAdapter(
    private var context: Context,
    private var tabs: List<String>,
    private var setup: (tab: String, view: View) -> Unit,
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.tab_config, container, false)
        container.addView(view)

        setup(tabs[position], view)

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

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
