package flaskoski.rs.smartmuseum.listAdapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_feature_preferences.*
import kotlinx.android.synthetic.main.feature_item.view.*
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class FeaturesListAdapter(isFeaturePreferences : Boolean = true, private val featuresList: List<Feature>, private val context: Context,
                          private val onRatingsCompletedCallback: OnShareClickListener) : RecyclerView.Adapter<FeaturesListAdapter.ItemViewHolder>(){
    var ratingsChanged : Boolean = false
    private var featuresRated = 0
    init{
        featuresRated = 0
        //look for feature ratings on db and update
        ApplicationProperties.user?.id?.let {userId ->
            RatingDAO().getAllFromUserByType(userId, Rating.TYPE_FEATURE, isFeaturePreferences) {featureRatings ->
                Toast.makeText(context, "Carregando valores...", Toast.LENGTH_SHORT).show()
                for(rating in featureRatings){
                    featuresList.find{ i -> i.id == rating.item }.let { feature ->
                        feature?.rating = rating.rating
                        featuresRated++
                    }
                }
                notifyDataSetChanged()
                checkIfRatingsCompletedAndSetFlag()
            }
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

    private fun setStars(rating: Float, starViews: List<ImageView>) {
        var count = 0
        starViews.forEach{
            if(count++ < rating)
                it.setImageResource(android.R.drawable.btn_star_big_on)
            else
                it.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }


    override fun onBindViewHolder(p0: ItemViewHolder, p1: Int) {
        val starViews = listOf<ImageView>(p0.itemView.img_star1, p0.itemView.img_star2, p0.itemView.img_star3, p0.itemView.img_star4, p0.itemView.img_star5)

        val rate = fun(v : View) {
            closeKeyboard(v)
            ratingsChanged = true

            if(!isRatingAlreadySet(p1)) featuresRated++
            val index = starViews.indexOf(v)
            val rating = (index+1).toFloat()
            featuresList.get(p1).rating = rating
            setStars(rating, starViews)

            checkIfRatingsCompletedAndSetFlag()
        }

        p0.itemView.txt_featureName.text = featuresList[p1].description
        starViews.forEach{ star ->
            star.setImageResource(android.R.drawable.btn_star_big_off)
            star.setOnClickListener(rate)
        }

        if( isRatingAlreadySet(p1))
            rate(starViews.get((featuresList[p1].rating-1).toInt()))


    }

    fun closeKeyboard(v : View){
        val imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
    private fun checkIfRatingsCompletedAndSetFlag() {
        if (featuresRated >= featuresList.size) {
            onRatingsCompletedCallback.onRatingsCompleted()
            featuresRated = 0 //dont need to set anymore
        }
    }

    private fun isRatingAlreadySet(p1: Int): Boolean{
        return featuresList.get(p1).rating > 0
    }

    interface OnShareClickListener{
        fun onRatingsCompleted()
    }
}