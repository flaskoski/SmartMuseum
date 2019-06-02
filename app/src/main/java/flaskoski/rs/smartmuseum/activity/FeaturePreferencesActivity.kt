package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Toast
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.listAdapter.FeaturesListAdapter
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_feature_preferences.*
import java.util.*
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.support.v4.content.ContextCompat.getSystemService
import android.R




class FeaturePreferencesActivity : AppCompatActivity(), FeaturesListAdapter.OnShareClickListener {

    val featureList = ArrayList<Feature>()
    val db = RatingDAO()
    private val TAG = "##--FeaturePreferences"
    private var allFeaturesRated : Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(flaskoski.rs.smartmuseum.R.layout.activity_feature_preferences)

        if(!ApplicationProperties.userNotDefinedYet())
            txt_username.setText(ApplicationProperties.user?.name)

        featureList.add(Feature("Física"))
        featureList.add(Feature("Química"))
        featureList.add(Feature("Astronomia"))
        featureList.add(Feature("Geologia"))
        featureList.add(Feature("Biologia"))

        val adapter = FeaturesListAdapter(featureList, applicationContext, this)
        list_features.layoutManager = LinearLayoutManager(applicationContext)
        list_features.adapter = adapter

//        this.currentFocus?.let {
//            val inputManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
//        }
            txt_username.setOnFocusChangeListener { v, focused ->
                if (!focused) {
                    val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

                }
            }

    }

    fun saveFeaturePreferences(v : View) {
        //No username
        if(txt_username.text.isBlank()){
            Toast.makeText(applicationContext, "Nome em branco! Por favor, informe um nome de usuário.", Toast.LENGTH_SHORT).show()
            txt_username.setError("Nome em branco!")
            return
        }
        if(!allFeaturesRated){
            Toast.makeText(applicationContext, "Por favor, informe seu nível de interesse em cada categoria antes de avançar.", Toast.LENGTH_LONG).show()
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

    override fun onRatingsCompleted() {
        allFeaturesRated = true
    }
}
