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
import android.text.method.LinkMovementMethod
import flaskoski.rs.smartmuseum.R
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

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

        internetConnectionWarning = AlertBuilder().buildNetworkUnavailableWarning(bt_confirm, false, false)

        featureList.add(Feature("Física", "O que é eletricidade e como ela funciona"))
        featureList.add(Feature("Química", "A água é composta de dois átomos de hidrogênio e um de oxigênio"))
        featureList.add(Feature("Astronomia", "O diâmetro do Sol é mais de 100 vezes maior que o da terra"))
        featureList.add(Feature("Geologia", "O granito é um tipo de rocha que se origina de magma como o expelido por vulcões"))
        featureList.add(Feature("Biologia", "A bactéria é um tipo de micro-organismo que está presente em quase todos os lugares da terra"))
        featureList.add(Feature("História", "Segundo a mitologia grega, Atena é a deusa da sabedoria"))
        featureList.add(Feature("Natureza", "Gosto de andar próximo da natureza"))

        check_terms_acceptance.movementMethod = LinkMovementMethod.getInstance()

        val adapter = FeaturesListAdapter(true, featureList, applicationContext, this)
        list_features.layoutManager = LinearLayoutManager(applicationContext)
        list_features.adapter = adapter

//        this.currentFocus?.let {
//            val inputManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
//        }

        txt_mm.setOnFocusChangeListener { v, focused ->
            if (!focused) hideKeyboard(v)
        }

        check_terms_acceptance.setOnCheckedChangeListener { compoundButton, _ ->
            hideKeyboard(compoundButton)
            compoundButton.error = null
        }

        switch_already_visited.setOnClickListener{
            hideKeyboard(it)
            if(switch_already_visited.isChecked) switch_already_visited.text = switch_already_visited.textOn
            else switch_already_visited.text = switch_already_visited.textOff
        }
        lb_your_interests.setOnClickListener {hideKeyboard(it)}

        txt_hh.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if(txt_hh.text.isNotEmpty())
                    txt_mm.requestFocus()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        if(ApplicationProperties.userNotDefinedYet())
            ApplicationProperties.checkForUpdates(ApplicationProperties.getCurrentVersionCode(applicationContext)){isThereUpdates ->
                if(isThereUpdates == true)
                    if(ApplicationProperties.checkIfForceUpdateIsOn() == true)
                        AlertBuilder().showUpdateRequired(this@FeaturePreferencesActivity){
                            finish()
                        }
                    else{
                        AlertBuilder().showUpdateAvailable(this@FeaturePreferencesActivity)
                    }
            }
        else {
            txt_user_age.setText(ApplicationProperties.user?.age?.toString()?:"")
            check_terms_acceptance.isChecked = ApplicationProperties.user!!.termsAccepted
            switch_already_visited.isChecked = ApplicationProperties.user!!.alreadyVisited
            if(switch_already_visited.isChecked) switch_already_visited.text = switch_already_visited.textOn
            else switch_already_visited.text = switch_already_visited.textOff
            txt_hh.setText((ApplicationProperties.user!!.timeAvailable / 60.0).toInt().toString())
            txt_mm.setText((ApplicationProperties.user!!.timeAvailable % 60).toInt().toString())
        }

        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            internetConnectionWarning.show()

    }

    private fun hideKeyboard(v: View){
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
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

    private fun areFirstFieldsCorrect(): Boolean {
        //No username
        if(txt_hh.text.isBlank() || txt_mm.text.isBlank()){
            Snackbar.make(bt_confirm, "Campo em branco! Por favor, complete todos os campos.", Snackbar.LENGTH_LONG).show()

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
        if(!check_terms_acceptance.isChecked){
            check_terms_acceptance.error = "Você deve aceitar os termos de uso para continuar!"
            return false
        }
        return true
    }

    private fun areSecondFieldsCorrect() :Boolean {
        if(!allFeaturesRated){
            Snackbar.make(bt_confirm, "Por favor, informe seu nível de interesse para cada frase antes de avançar.", Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private var txt_user_ageHeight = 0
    fun toggleForm(v : View){
        if(lb_userage.visibility == View.VISIBLE){
            if(!areFirstFieldsCorrect()) return

            lb_userage.visibility = View.GONE
            txt_user_ageHeight = txt_user_age.height
            txt_user_age.height = 0
            txt_user_age.visibility = View.INVISIBLE
            lb_time_available.visibility = View.GONE
            txt_hh.visibility = View.GONE
            txt_mm.visibility = View.GONE
            lb_colon.visibility = View.GONE
            lb_already_visited.visibility = View.GONE
            switch_already_visited.visibility = View.GONE
            check_terms_acceptance.visibility = View.GONE
            group_second_part.visibility = View.VISIBLE
            bt_continue.text = getString(R.string.voltar)
            hideKeyboard(v)
        }
        else{
            lb_userage.visibility = View.VISIBLE
            txt_user_age.visibility = View.VISIBLE
            txt_user_age.height = txt_user_ageHeight
            lb_time_available.visibility = View.VISIBLE
            txt_hh.visibility = View.VISIBLE
            txt_mm.visibility = View.VISIBLE
            lb_colon.visibility = View.VISIBLE
            lb_already_visited.visibility = View.VISIBLE
            switch_already_visited.visibility = View.VISIBLE
            check_terms_acceptance.visibility = View.VISIBLE
            group_second_part.visibility = View.GONE
            bt_continue.text = getString(R.string.continuar)
        }

    }
    fun checkInternetAndSave(v : View){
        hideKeyboard(v)
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            internetConnectionWarning.show()
        else saveFeaturePreferences()
    }

    private fun addFeatureRatingsToDB(ratings: ArrayList<Rating>) {
        if((list_features.adapter as FeaturesListAdapter).ratingsChanged) {
            db.addAll(ratings) //needs an addAll function
        }
        Log.i(TAG, "$ratings added to ratings!")
    }

    private fun saveFeaturePreferences() {
        if(!areSecondFieldsCorrect()) return

        //username informed
        if(ApplicationProperties.userNotDefinedYet())
            saveCurrentUser()
        //username already exists -> update age and keep id
        else editUser()

        var ratings = ArrayList<Rating>()
        //save ratings
        ApplicationProperties.user?.id?.let {
            for(feature in featureList) {
                ratings.add(Rating(it, feature.id, feature.rating, 0F, ApplicationProperties.recommendationSystem,
                        ApplicationProperties.getCurrentVersion(applicationContext)?.let { it}?:"", type = Rating.TYPE_FEATURE) )
            }
            ApplicationProperties.user!!.getAgeGroup()?.let{ageGroup ->
                ratings.add(Rating(it, User.FIELD_AGE, ageGroup, 0F, ApplicationProperties.recommendationSystem,
                        ApplicationProperties.getCurrentVersion(applicationContext)?.let { it}?:"", type = Rating.TYPE_FEATURE))

            }
            addFeatureRatingsToDB(ratings)
        }

        //TODO check if correctly saved on db
        val returningRatingsIntent = Intent()
        returningRatingsIntent.putExtra("featureRatings", ratings)
        returningRatingsIntent.putExtra("timeAvailable", ApplicationProperties.user?.timeAvailable)
        setResult(Activity.RESULT_OK, returningRatingsIntent)
        finish()
    }

    private fun editUser() {
        ApplicationProperties.user!!.age =
            if(txt_user_age.text.isEmpty()) null else txt_user_age.text.toString().toInt()
        ApplicationProperties.user!!.alreadyVisited = switch_already_visited.isChecked
        ApplicationProperties.user!!.termsAccepted = check_terms_acceptance.isChecked
        ApplicationProperties.user!!.timeAvailable = txt_hh.text.toString().toDouble() * 60 + txt_mm.text.toString().toDouble()
    }

    private fun saveCurrentUser() {
        val timeAvailable = txt_hh.text.toString().toDouble() * 60 + txt_mm.text.toString().toDouble()
        val userAge = if(txt_user_age.text.isEmpty()) null else txt_user_age.text.toString().toInt()
        ApplicationProperties.user = User(UUID.randomUUID().toString(),
                userAge, switch_already_visited.isChecked, timeAvailable, context = applicationContext,
                termsAccepted = check_terms_acceptance.isChecked)
        UserDAO().add(ApplicationProperties.user!!)
    }

    override fun onRatingsCompleted() {
        allFeaturesRated = true
    }
}
