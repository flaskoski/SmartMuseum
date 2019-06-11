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

        featureList.add(Feature("Física", "O que é eletricidade e como ela funciona"))
        featureList.add(Feature("Química", "A água é composta de dois átomos de hidrogênio e um de oxigênio"))
        featureList.add(Feature("Astronomia", "O diâmetro do Sol é mais de 100 vezes maior que o da terra"))
        featureList.add(Feature("Geologia", "O granito é um tipo de rocha que se origina de magma como o expelido por vulcões"))
        featureList.add(Feature("Biologia", "A bactéria é um tipo de célula e está presente em quase todos os lugares da terra"))

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
            Toast.makeText(applicationContext, "Por favor, informe seu nível de interesse para cada frase antes de avançar.", Toast.LENGTH_LONG).show()
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
                var rating = Rating(it, feature.id, feature.rating, Rating.TYPE_FEATURE)
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
