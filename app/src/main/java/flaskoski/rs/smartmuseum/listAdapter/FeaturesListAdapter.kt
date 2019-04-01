package flaskoski.rs.smartmuseum.listAdapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Feature
import kotlinx.android.synthetic.main.feature_item.view.*

class FeaturesListAdapter(private val featuresList: List<Feature>, private val context: Context) : RecyclerView.Adapter<FeaturesListAdapter.ItemViewHolder>() {
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.feature_item, p0, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return featuresList.size
    }

    override fun onBindViewHolder(p0: ItemViewHolder, p1: Int) {
        val rate = fun(v : View) {
            featuresList.get(p1).rating = 1.0F
            p0.itemView.img_star2.setImageResource(android.R.drawable.btn_star_big_off)
            p0.itemView.img_star3.setImageResource(android.R.drawable.btn_star_big_off)
            p0.itemView.img_star4.setImageResource(android.R.drawable.btn_star_big_off)
            p0.itemView.img_star5.setImageResource(android.R.drawable.btn_star_big_off)
            p0.itemView.img_star1.setImageResource(android.R.drawable.btn_star_big_on)

            if (v != p0.itemView.img_star1) {
                p0.itemView.img_star2.setImageResource(android.R.drawable.btn_star_big_on)
                if (v != p0.itemView.img_star2) {
                    p0.itemView.img_star3.setImageResource(android.R.drawable.btn_star_big_on)
                    if (v != p0.itemView.img_star3) {
                        p0.itemView.img_star4.setImageResource(android.R.drawable.btn_star_big_on)
                        if (v != p0.itemView.img_star4) {
                            p0.itemView.img_star5.setImageResource(android.R.drawable.btn_star_big_on)
                            featuresList.get(p1).rating = 5F
                        } else {
                            featuresList.get(p1).rating = 4F
                        }
                    } else {
                        featuresList.get(p1).rating = 3F
                    }
                } else {
                    featuresList.get(p1).rating = 2F
                }
            }
        }

        p0.itemView.txt_featureName.text = featuresList.get(p1).name
        p0.itemView.img_star1.setImageResource(android.R.drawable.btn_star_big_off)
        p0.itemView.img_star2.setImageResource(android.R.drawable.btn_star_big_off)
        p0.itemView.img_star3.setImageResource(android.R.drawable.btn_star_big_off)
        p0.itemView.img_star4.setImageResource(android.R.drawable.btn_star_big_off)
        p0.itemView.img_star5.setImageResource(android.R.drawable.btn_star_big_off)

        p0.itemView.img_star1.setOnClickListener(rate)
        p0.itemView.img_star2.setOnClickListener(rate)
        p0.itemView.img_star3.setOnClickListener(rate)
        p0.itemView.img_star4.setOnClickListener(rate)
        p0.itemView.img_star5.setOnClickListener(rate)
    }



}