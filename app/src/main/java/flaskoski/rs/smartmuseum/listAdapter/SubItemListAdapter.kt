package flaskoski.rs.smartmuseum.listAdapter

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import flaskoski.rs.smartmuseum.model.Itemizable
import flaskoski.rs.smartmuseum.model.SubItem
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.sub_item.view.*

class SubItemListAdapter(private val subItemList: List<Itemizable>?, private val context: Context, private val activity : Activity) : RecyclerView.Adapter<SubItemListAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sub_item, p0, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return subItemList?.size ?: 0
    }

    override fun onBindViewHolder(p0: ItemViewHolder, p1: Int) {
//        p0.itemView.txt_featureName.text = subItemList[p1].description
        p0.itemView.img_itemThumb.setImageResource(context.resources.getIdentifier(subItemList?.get(p1)?.photoId, "drawable", context.packageName))
        bringToFront(p0.itemView.ratingBar)
        bringToFront(p0.itemView.icon_visited)
        p0.itemView.setOnClickListener {
            if(ApplicationProperties.user == null) {
                Toast.makeText(context, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            }
//            var subItems : ArrayList<Itemizable>? = null
            val viewItemDetails = Intent(context, ItemDetailActivity::class.java)
//        val itemId = journeyManager.itemsList[p1].id
//        var itemRating : Float
//        journeyManager.ratingsList.find { it.user == ApplicationProperties.user?.id
//                && it.item == itemId }?.let {
//            itemRating = it.rating
//        }

            viewItemDetails.putExtra("itemClicked",  subItemList?.get(p1) as SubItem)
            startActivityForResult(activity, viewItemDetails, 0, null)
        }
    }

    private fun bringToFront(view: View, z_value: Float = 20f){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.translationZ = z_value
            view.invalidate()
        }
        else {
            view.bringToFront()
            view.parent.requestLayout()
            //sheet_next_items.parent.invalidate()
        }
    }

    interface OnShareClickListener{
        fun onRatingsCompleted()
    }
}