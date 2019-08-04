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
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.util.NetworkVerifier
import flaskoski.rs.smartmuseum.util.ParseTime
import kotlinx.android.synthetic.main.activity_questionnaire.*


class QuestionnaireActivity : AppCompatActivity(), FeaturesListAdapter.OnShareClickListener {

    private val questionList = ArrayList<Feature>()
    private val db = RatingDAO()
    private val TAG = "##--FeaturePreferences"
    private var allFeaturesRated : Boolean = false


    private lateinit var internetConnectionWarning: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(flaskoski.rs.smartmuseum.R.layout.activity_questionnaire)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Questionário"

        internetConnectionWarning = Snackbar.make(bt_confirm_questionnaire, "Conexão com a internet não encontrada. Por favor verifique sua conexão!", Snackbar.LENGTH_LONG)

        questionList.add(Feature("visit", "Qual seu nível de satisfação com a visita?"))
        questionList.add(Feature("app", "Qual seu nível de satisfação com o aplicativo do parque?"))
        questionList.add(Feature("recommendations", "Qual seu nível de satisfação com as atrações escolhidas para você?"))
        questionList.add(Feature("route", "Qual seu nível de satisfação com o caminho recomendado para você chegar até as atrações?"))

        val adapter = FeaturesListAdapter(false, questionList, applicationContext, this)
        list_questions.layoutManager = LinearLayoutManager(applicationContext)
        list_questions.adapter = adapter

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
        if(!allFeaturesRated){
            Snackbar.make(bt_confirm_questionnaire, "Por favor, informe seu nível de satisfação para as perguntas acima.", Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun checkInternetAndSave(@Suppress("UNUSED_PARAMETER") v : View){
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            internetConnectionWarning.show()
        else saveFeaturePreferences()
    }

    private fun saveFeaturePreferences() {

        if(!areFieldsCorrect()) return

        var ratings = ArrayList<Rating>()
        //save ratings
        ApplicationProperties.user?.id?.let {
            for(feature in questionList) {
                var rating = Rating(it, feature.id, feature.rating, 0F, ApplicationProperties.recommendationSystem,
                        ApplicationProperties.getCurrentVersion(applicationContext)?.let { it}?:"", type = Rating.TYPE_FEATURE)

                if((list_questions.adapter as FeaturesListAdapter).ratingsChanged) {
                    rating.date = ParseTime.getCurrentTime()
                    db.addQuestionnaireAnswer(rating) //needs an addAll function
                }
                ratings.add(rating)
            }
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onRatingsCompleted() {
        allFeaturesRated = true
    }
}
