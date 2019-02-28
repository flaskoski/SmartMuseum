package flaskoski.rs.smartmuseum

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.nav_header_main.view.*

class ItemsGridListAdapter(private val itemsList : List<Item>, private val context : Context) : RecyclerView.Adapter<ItemsGridListAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }
    override fun onBindViewHolder(p0: ItemsGridListAdapter.ItemViewHolder, p1: Int) {
        p0.itemView.textView.text = itemsList.get(p1).title
    }

    override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item, p0, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

}