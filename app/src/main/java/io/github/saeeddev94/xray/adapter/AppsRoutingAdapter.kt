package io.github.saeeddev94.xray.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.dto.AppList

class AppsRoutingAdapter(
    private var context: Context,
    private var apps: MutableList<AppList>,
    private var appsRouting: MutableSet<String>,
) : RecyclerView.Adapter<AppsRoutingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(container: ViewGroup, type: Int): ViewHolder {
        val linearLayout = LinearLayout(context)
        val item: View = LayoutInflater.from(context).inflate(R.layout.item_recycler_exclude, linearLayout, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val app = apps[index]
        val isSelected = appsRouting.contains(app.packageName)
        holder.appIcon.setImageDrawable(app.appIcon)
        holder.appName.text = app.appName
        holder.packageName.text = app.packageName
        holder.isSelected.isChecked = isSelected
        holder.appContainer.setOnClickListener {
            if (isSelected) {
                appsRouting.remove(app.packageName)
            } else {
                appsRouting.add(app.packageName)
            }
            notifyItemChanged(index)
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        var appContainer: LinearLayout = item.findViewById(R.id.appContainer)
        var appIcon: ImageView = item.findViewById(R.id.appIcon)
        var appName: TextView = item.findViewById(R.id.appName)
        var packageName: TextView = item.findViewById(R.id.packageName)
        var isSelected: MaterialCheckBox = item.findViewById(R.id.isSelected)
    }
}
