package flaskoski.rs.smartmuseum.recommender

import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.grid_item.view.*
import net.librec.recommender.Recommender
import net.librec.recommender.cf.UserKNNRecommender

class RecommenderManager(var recommender : Recommender? = null){

    fun getPrediction(userId : String, itemId : String) : Float? {
        if(recommender == null) return null

        val userIndex = (recommender as UserKNNRecommender).userMappingData.get(userId)
        val itemIndex = (recommender as UserKNNRecommender).itemMappingData.get(itemId)
        if (userIndex != null && itemIndex != null)
            return (recommender as UserKNNRecommender).predict(userIndex, itemIndex).toFloat()
        else return null
    }

}