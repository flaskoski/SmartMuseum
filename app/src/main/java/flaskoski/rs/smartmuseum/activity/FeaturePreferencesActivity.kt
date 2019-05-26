package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.FeaturesListAdapter
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_feature_preferences.*
import java.util.*

class FeaturePreferencesActivity : AppCompatActivity() {

    val featureList = ArrayList<Feature>()
    val db = RatingDAO()
    private val TAG = "##--FeaturePreferences"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_preferences)

        if(!ApplicationProperties.userNotDefinedYet())
            txt_username.setText(ApplicationProperties.user?.name)

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
        //No username
        if(txt_username.text.isBlank()){
            Toast.makeText(applicationContext, "Nome em branco! Por favor, informe um nome de usuário.", Toast.LENGTH_SHORT).show()
            txt_username.setError("Nome em branco!")
            return
        }

        //username informed
        if(ApplicationProperties.userNotDefinedYet())
            saveCurrentUserOnDb()
        //username already exists -> update name and keep id
        else ApplicationProperties.user!!.name = txt_username.text.toString()

        var ratings = ArrayList<Rating>()
        //save ratings
        ApplicationProperties.user?.id?.let {
            for(feature in featureList) {
                var rating = Rating(it, feature.name, feature.rating, Rating.TYPE_FEATURE)
                db.add(rating)
                ratings.add(rating)
                Log.i(TAG, rating.toString())
            }
        }

        //TODO check if correctly saved on db
        val returningRatingsIntent = Intent()
        returningRatingsIntent.putExtra("featureRatings", ratings)
        setResult(Activity.RESULT_OK, returningRatingsIntent)
        finish()
    }

    private fun saveCurrentUserOnDb() {
        ApplicationProperties.user = User(UUID.randomUUID().toString(), txt_username.text.toString() )
    }
}
