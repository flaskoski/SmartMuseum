package flaskoski.rs.smartmuseum.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener {

    private val REQUEST_GET_PREFERENCES: Int = 1
    private val REQUEST_ITEM_RATING_CHANGE: Int = 2

    private val itemsList = ArrayList<Item>()
    private var ratingsList  = HashSet<Rating>()
    private val TAG = "MainActivity"
//    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
//        when (item.itemId) {
//            R.id.navigation_home -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_dashboard -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_notifications -> {
//              //  message.setText(R.string.title_notifications)
//                return@OnNavigationItemSelectedListener true
//            }
//        }
//        false
//    }

    private lateinit var adapter: ItemsGridListAdapter

    private lateinit var parallelRequestsManager: ParallelRequestsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        //supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF677589")))

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        //---------------------------------------------------------------------

        parallelRequestsManager = ParallelRequestsManager(2)

        // Access a Cloud Firestore instance from your Activity

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, this, RecommenderManager())
        itemsGridList.adapter = adapter

        val itemDAO = ItemDAO()
        itemDAO.getAllItems {
            itemsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            if(parallelRequestsManager.isComplete!!)
                buildRecommender()
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            if(parallelRequestsManager.isComplete!!)
                buildRecommender()
        }
        if(ApplicationProperties.userNotDefinedYet()){
            val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
            startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
        }
    }


    private fun updateRecommender() {
            buildRecommender()
            Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
    }

    private fun buildRecommender() {
        adapter.recommenderManager.recommender = RecommenderBuilder().buildKNNRecommender(ratingsList, applicationContext)

        if(!ApplicationProperties.userNotDefinedYet()) {
            for(item in itemsList){
                val rating = adapter.recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
                if (rating != null)
                    item.recommedationRating = rating
                else item.recommedationRating = 0F
            }
            itemsList.sortByDescending{it.recommedationRating}
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_features -> {
                val goToFeaturePreferences = Intent(applicationContext, FeaturePreferencesActivity::class.java)
               // goToPlayerProfileIntent.putExtra("uid", uid)
                startActivityForResult(goToFeaturePreferences, REQUEST_GET_PREFERENCES)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
            if(requestCode == REQUEST_GET_PREFERENCES) {
                if(data != null)
                    (data.getSerializableExtra("featureRatings") as List<Rating>).forEach{
                        ratingsList.add(it)
                    }
            }
            if(requestCode == REQUEST_ITEM_RATING_CHANGE){
                if(data != null)
                    ratingsList.add(data.getSerializableExtra("itemRating") as Rating)
            }
            updateRecommender()
        }
    }


    override fun shareOnItemClicked(p1: Int) {
        if(ApplicationProperties.user == null)
        {
            Toast.makeText(applicationContext, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return
        }
        val viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        val itemId = itemsList[p1].id
        var itemRating : Float
        ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == itemId }?.let {
            itemRating = it.rating
            viewItemDetails.putExtra("itemRating", itemRating)
        }

        viewItemDetails.putExtra("itemClicked", itemsList[p1])
        startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }
}
