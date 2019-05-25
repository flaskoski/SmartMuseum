package flaskoski.rs.smartmuseum.listAdapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import kotlinx.android.synthetic.main.feature_item.view.*

class FeaturesListAdapter(private val featuresList: List<Feature>, private val context: Context) : RecyclerView.Adapter<FeaturesListAdapter.ItemViewHolder>() {
    init{
        //get feature names
        val featureNames = ArrayList<String>()
        for(feature in featuresList){
            featureNames.add(feature.name)
        }
        //look for feature ratings on db and update
        RatingDAO().getAllByType(Rating.TYPE_FEATURE) {
            Toast.makeText(context, "Carregando valores...", Toast.LENGTH_SHORT)
            for(rating in it){
                featuresList.find{ i -> i.name == rating.item }.let { feature -> feature?.rating = rating.rating }
            }
            notifyDataSetChanged()
        }
    }

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
        val starViews = listOf<View>(p0.itemView.img_star1, p0.itemView.img_star2, p0.itemView.img_star3, p0.itemView.img_star4, p0.itemView.img_star5)
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

        if( isRatingAlreadySet(p1))
            rate(starViews.get((featuresList.get(p1).rating-1).toInt()))
    }

    private fun isRatingAlreadySet(p1: Int): Boolean{
        return featuresList.get(p1).rating > 0
    }
}