package flaskoski.rs.smartmuseum.listAdapter

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Itemizable
import flaskoski.rs.smartmuseum.model.SubItem
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.sub_item.view.*

class SubItemListAdapter(private val subItemList: List<SubItem>,
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
        p0.itemView.lb_subitem.text = subItemList[p1].title
        if(subItemList[p1].photoId.isNotBlank())
            ItemRepository.loadBackgroundPhoto(activity.applicationContext, p0.itemView.layout_subitem, subItemList[p1].photoId)
        if(subItemList[p1].isVisited){
            p0.itemView.subitem_icon_visited.visibility = View.VISIBLE
            p0.itemView.subitem_icon_visited.setBackgroundResource(android.R.drawable.checkbox_on_background)
        }else p0.itemView.subitem_icon_visited.visibility = View.GONE

        if(!ApplicationProperties.userNotDefinedYet())
            p0.itemView.subitem_ratingBar.rating = subItemList[p1].recommedationRating
        else p0.itemView.subitem_ratingBar.rating = 0f

        p0.itemView.setOnClickListener{(activity as OnShareSubItemClickListener).shareOnItemClicked(subItemList[p1])}
    }

    interface OnShareSubItemClickListener{
        fun shareOnItemClicked(subItem: SubItem)
    }
}