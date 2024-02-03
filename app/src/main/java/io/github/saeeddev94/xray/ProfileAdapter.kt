package io.github.saeeddev94.xray

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.saeeddev94.xray.database.ProfileList

class ProfileAdapter(
    private var context: Context,
    private var profiles: List<ProfileList>,
    private var callback: ProfileClickListener,
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    override fun onCreateViewHolder(container: ViewGroup, type: Int): ViewHolder {
        val linearLayout = LinearLayout(context)
        val item: View = LayoutInflater.from(context).inflate(R.layout.item_recycler_main, linearLayout, false)
        return ViewHolder(item)
    }

    override fun getItemCount(): Int {
        return profiles.size
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val profile = profiles[index]
        profile.index = index
        val color = if (Settings.selectedProfile == profile.id) R.color.active else R.color.cardColor
        holder.activeIndicator.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
        holder.profileName.text = profile.name
        holder.profileCard.setOnClickListener {
            callback.profileSelect(index, profile)
        }
        holder.profileEdit.setOnClickListener {
            callback.profileEdit(index, profile)
        }
        holder.profileDelete.setOnClickListener {
            callback.profileDelete(index, profile)
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        var activeIndicator: LinearLayout = item.findViewById(R.id.activeIndicator)
        var profileCard: CardView = item.findViewById(R.id.profileCard)
        var profileName: TextView = item.findViewById(R.id.profileName)
        var profileEdit: LinearLayout = item.findViewById(R.id.profileEdit)
        var profileDelete: LinearLayout = item.findViewById(R.id.profileDelete)
    }
}
