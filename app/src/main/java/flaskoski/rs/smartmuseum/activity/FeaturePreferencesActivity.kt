package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.FeaturesListAdapter
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import kotlinx.android.synthetic.main.activity_feature_preferences.*

class FeaturePreferencesActivity : AppCompatActivity() {

    val featureList = ArrayList<Feature>()
    val db = RatingDAO()
    private val TAG = "##--FeaturePreferences"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_preferences)



        featureList.add(Feature("Física"))
        featureList.add(Feature("Química"))
        featureList.add(Feature("Astronomia"))
        featureList.add(Feature("Geologia"))
        featureList.add(Feature("Biologia"))

        val adapter = FeaturesListAdapter(featureList, applicationContext)
        list_features.layoutManager = LinearLayoutManager(applicationContext)
        list_features.adapter = adapter

    }

    fun saveFeaturePreferences(v : View) {
//        val databaseIORequests: DatabaseIORequests = DatabaseIORequests(applicationContext)
        var rating : Rating
        for(feature in featureList) {
            rating = Rating("Felipe", feature.name, feature.rating)
            db.add(rating)
            Log.i(TAG, rating.toString())
        }

        setResult(Activity.RESULT_OK)
        finish()
    }
}
