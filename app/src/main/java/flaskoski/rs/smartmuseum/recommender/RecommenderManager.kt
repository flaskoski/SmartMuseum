package flaskoski.rs.smartmuseum.recommender

import android.util.Log
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.grid_item.view.*
import net.librec.recommender.AbstractRecommender
import net.librec.recommender.Recommender
import net.librec.recommender.cf.ItemKNNRecommender
import net.librec.recommender.cf.UserKNNRecommender

class RecommenderManager(var recommender : AbstractRecommender? = null){

    private val TAG: String = "RecommenderManager"

    fun getPrediction(userId : String, itemId : String) : Float? {
        if(recommender == null){
            Log.e(TAG, "predict called but recommender is null!")
            return null
        }

        val userIndex = recommender!!.userMappingData[userId]
        val itemIndex = recommender!!.itemMappingData[itemId]
        if (userIndex != null && itemIndex != null)
            return (recommender as UserKNNRecommender).predict(userIndex, itemIndex).toFloat()
        else return null
    }

}