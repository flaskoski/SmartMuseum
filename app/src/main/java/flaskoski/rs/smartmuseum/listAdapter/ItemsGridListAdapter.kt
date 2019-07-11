package flaskoski.rs.smartmuseum.listAdapter

import android.content.Context
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.grid_item.view.*

class ItemsGridListAdapter(private val itemsList: List<Item>,
                           private val context: Context,
                           val mainActivityCallback : OnShareClickListener,
                           var recommenderManager: RecommenderManager)
    : RecyclerView.Adapter<ItemsGridListAdapter.ItemViewHolder>() {


    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }
    override fun onBindViewHolder(p0: ItemViewHolder, p1: Int) {
        p0.itemView.lb_item_name.text = itemsList.get(p1).title
        p0.itemView.img_itemThumb.setImageResource(context.resources.getIdentifier(itemsList[p1].photoId, "drawable", context.packageName))
        //p0.itemView.ratingBar.rating = itemsList.get(p1).avgRating

        if(itemsList[p1].isVisited) {
            p0.itemView.setBackgroundResource(R.color.colorVisitedItem)
            p0.itemView.icon_visited.setBackgroundResource(context.resources.getIdentifier("baseline_done_black_24",
                    "drawable", context.packageName))
            p0.itemView.icon_visited.visibility = View.VISIBLE
        }
        else if(itemsList[p1].recommendedOrder != Int.MAX_VALUE){
            p0.itemView.setBackgroundResource(R.color.colorRecommendedItem)
            p0.itemView.icon_visited.visibility = View.GONE
        }
        else{
            p0.itemView.setBackgroundResource(android.R.color.white)
            p0.itemView.icon_visited.visibility = View.GONE
        }
        if(!ApplicationProperties.userNotDefinedYet()) {
            val rating = recommenderManager.getPrediction(ApplicationProperties.user!!.id, itemsList.get(p1).id)
            if(rating != null)
                p0.itemView.ratingBar.rating = rating
            else p0.itemView.ratingBar.rating = 0F
        }else p0.itemView.ratingBar.rating = 0F

        p0.itemView.setOnClickListener{
            mainActivityCallback.shareOnItemClicked(p1)
//            val viewItemDetails = Intent(context, ItemDetailActivity::class.java)
//            viewItemDetails.putExtra("itemHash", itemsList.get(p1).id)
//
//            startActivity(context, viewItemDetails, null)
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item, p0, false)
        view.img_itemThumb.setImageResource(R.mipmap.image_not_found)

        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    interface OnShareClickListener{
        fun shareOnItemClicked(p1 : Int, isArrived : Boolean = false)
    }
}

