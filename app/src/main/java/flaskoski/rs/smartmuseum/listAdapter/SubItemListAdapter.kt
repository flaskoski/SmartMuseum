package flaskoski.rs.smartmuseum.listAdapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.activity.ItemDetailActivity
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Itemizable
import flaskoski.rs.smartmuseum.model.SubItem
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.grid_item.view.*
import kotlinx.android.synthetic.main.sub_item.view.*

class SubItemListAdapter(private val subItemList: List<Itemizable>,
                         private val activity : Activity) : RecyclerView.Adapter<SubItemListAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ItemViewHolder {
        val view = LayoutInflater.from(activity.applicationContext).inflate(R.layout.sub_item, p0, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return subItemList.size
    }

    override fun onBindViewHolder(p0: ItemViewHolder, p1: Int) {
//        p0.itemView.txt_featureName.text = recommendedSubItemList[p1].description
//        p0.itemView.img_itemThumb.setImageResource(context.resources.getIdentifier(recommendedSubItemList?.get(p1)?.photoId, "drawable", context.packageName))
//        p0.itemView.setBackgroundResource(context.resources.getIdentifier(recommendedSubItemList?.get(p1)?.photoId, "drawable", context.packageName))
        p0.itemView.layout_subitem.setBackgroundResource(
                activity.applicationContext.resources.getIdentifier(subItemList[p1].photoId,
                "drawable", activity.applicationContext.packageName))
        if(subItemList[p1].isVisited){
            p0.itemView.subitem_icon_visited.visibility = View.VISIBLE
            p0.itemView.subitem_icon_visited.setBackgroundResource(android.R.drawable.checkbox_on_background)
        }else p0.itemView.subitem_icon_visited.visibility = View.GONE

        val rating = subItemList[p1].recommedationRating
        if(rating != null)
            p0.itemView.subitem_ratingBar.rating = rating
        else p0.itemView.subitem_ratingBar.rating = 0F

        p0.itemView.setOnClickListener{(activity as OnShareSubItemClickListener).shareOnItemClicked(p1)}
    }

    interface OnShareSubItemClickListener{
        fun shareOnItemClicked(itemPosition : Int)
    }
}