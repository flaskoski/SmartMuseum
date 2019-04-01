package flaskoski.rs.smartmuseum.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.FeaturesListAdapter
import flaskoski.rs.smartmuseum.model.Feature
import kotlinx.android.synthetic.main.activity_feature_preferences.*

class FeaturePreferencesActivity : AppCompatActivity() {

    val featureList = ArrayList<Feature>()

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

    fun saveFeaturePreferences() {
//        val databaseIORequests: DatabaseIORequests = DatabaseIORequests(applicationContext)
        var ratings = ""
        for(feature in featureList)
            ratings = ratings.plus("Felipe ${feature.name} ${feature.rating}\n")
//        databaseIORequests.write(ratings)
        finish()
    }
}
