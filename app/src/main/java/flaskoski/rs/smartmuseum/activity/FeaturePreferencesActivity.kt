package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.listAdapter.FeaturesListAdapter
import flaskoski.rs.smartmuseum.model.Feature
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_feature_preferences.*
import java.util.*
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.DAO.UserDAO
import flaskoski.rs.smartmuseum.util.NetworkVerifier
import flaskoski.rs.smartmuseum.util.ParseTime


class FeaturePreferencesActivity : AppCompatActivity(), FeaturesListAdapter.OnShareClickListener {

    val featureList = ArrayList<Feature>()
    val db = RatingDAO()
    private val TAG = "##--FeaturePreferences"
    private var allFeaturesRated : Boolean = false


    private lateinit var internetConnectionWarning: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(flaskoski.rs.smartmuseum.R.layout.activity_feature_preferences)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Preferências"

        internetConnectionWarning = Snackbar.make(bt_confirm, "Conexão com a internet não encontrada. Por favor verifique sua conexão!", Snackbar.LENGTH_LONG)

        featureList.add(Feature("Física", "O que é eletricidade e como ela funciona"))
        featureList.add(Feature("Química", "A água é composta de dois átomos de hidrogênio e um de oxigênio"))
        featureList.add(Feature("Astronomia", "O diâmetro do Sol é mais de 100 vezes maior que o da terra"))
        featureList.add(Feature("Geologia", "O granito é um tipo de rocha que se origina de magma como o expelido por vulcões"))
        featureList.add(Feature("Biologia", "A bactéria é um tipo de célula e está presente em quase todos os lugares da terra"))
        featureList.add(Feature("História", "Segundo a mitologia grega, Atena é a deusa da sabedoria"))
        featureList.add(Feature("Natureza", "Andar à beira da praia ou próximo da natureza"))

        val adapter = FeaturesListAdapter(true, featureList, applicationContext, this)
        list_features.layoutManager = LinearLayoutManager(applicationContext)
        list_features.adapter = adapter

//        this.currentFocus?.let {
//            val inputManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
//        }
        txt_mm.setOnFocusChangeListener { v, focused ->
            if (!focused) {
                 val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }

        txt_hh.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if(txt_hh.text.isNotEmpty())
                    txt_mm.requestFocus()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        if(ApplicationProperties.userNotDefinedYet())
            ApplicationProperties.checkForUpdates(ApplicationProperties.getCurrentVersionCode(applicationContext)){isThereUpdates ->
                if(isThereUpdates)
                    if(ApplicationProperties.checkIfForceUpdateIsOn() == true)
                        AlertBuilder().showUpdateRequired(this@FeaturePreferencesActivity){
                            finish()
                        }
                    else{
                        AlertBuilder().showUpdateAvailable(this@FeaturePreferencesActivity)
                    }
            }
        else {
            txt_user_age.setText(ApplicationProperties.user?.age.toString())
            txt_hh.setText((ApplicationProperties.user!!.timeAvailable / 60.0).toInt().toString())
            txt_mm.setText((ApplicationProperties.user!!.timeAvailable % 60).toInt().toString())
        }

        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            internetConnectionWarning.show()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                saveFeaturePreferences()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
       saveFeaturePreferences()
    }

    private fun areFieldsCorrect(): Boolean {
        //No username
        if(txt_user_age.text.isBlank() || txt_hh.text.isBlank() || txt_mm.text.isBlank()){
            Snackbar.make(bt_confirm, "Campo em branco! Por favor, complete todos os campos.", Snackbar.LENGTH_LONG).show()

            if(txt_user_age.text.isBlank())
                txt_user_age.error = "Idade em branco!"
            if(txt_hh.text.isBlank())
                txt_hh.error = "Horas em branco!"
            if(txt_mm.text.isBlank())
                txt_mm.error = "Minutos em branco!"
            return false
        }
        if(txt_hh.text.toString()=="0" && txt_mm.text.toString().toDouble() < 30.0) {
            txt_mm.error = "O tempo mínimo de visita pelo aplicativo é de 30 minutos."
            return false
        }
        if(!allFeaturesRated){
            Snackbar.make(bt_confirm, "Por favor, informe seu nível de interesse para cada frase antes de avançar.", Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun checkInternetAndSave(v : View){
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            internetConnectionWarning.show()
        else saveFeaturePreferences()
    }

    private fun addFeatureRatingsToDB(userId : String, featureId : String, featureRating : Float): Rating {
        //var ratings = ArrayList<Rating>()
        var rating = Rating(userId, featureId, featureRating, 0F, ApplicationProperties.recommendationSystem,
                ApplicationProperties.getCurrentVersion(applicationContext)?.let { it}?:"", type = Rating.TYPE_FEATURE)

        if((list_features.adapter as FeaturesListAdapter).ratingsChanged) {
            rating.date = ParseTime.getCurrentTime()
            db.add(rating) //needs an addAll function
        }

       // ratings.add(rating)
        Log.i(TAG, "$rating added to ratings!")
        return rating
    }

    private fun saveFeaturePreferences() {

        if(!areFieldsCorrect()) return

        //username informed
        if(ApplicationProperties.userNotDefinedYet())
            saveCurrentUser()
        //username already exists -> update age and keep id
        else {
            ApplicationProperties.user!!.age = txt_user_age.text.toString().toInt()
            ApplicationProperties.user!!.timeAvailable = txt_hh.text.toString().toDouble() * 60 + txt_mm.text.toString().toDouble()
        }

        var ratings = ArrayList<Rating>()
        //save ratings
        ApplicationProperties.user?.id?.let {

            for(feature in featureList) {
                ratings.add(addFeatureRatingsToDB(it, feature.id, feature.rating))
            }
            ratings.add(addFeatureRatingsToDB(it, User.FIELD_AGE, ApplicationProperties.user!!.getAgeGroup()))
        }

        //TODO check if correctly saved on db
        val returningRatingsIntent = Intent()
        returningRatingsIntent.putExtra("featureRatings", ratings)
        returningRatingsIntent.putExtra("timeAvailable", ApplicationProperties.user?.timeAvailable)
        setResult(Activity.RESULT_OK, returningRatingsIntent)
        finish()
    }

    private fun saveCurrentUser() {
        val timeAvailable = txt_hh.text.toString().toDouble() * 60 + txt_mm.text.toString().toDouble()
        ApplicationProperties.user = User(UUID.randomUUID().toString(), txt_user_age.text.toString().toInt(), timeAvailable)
        UserDAO().add(ApplicationProperties.user!!)
    }

    override fun onRatingsCompleted() {
        allFeaturesRated = true
    }
}
