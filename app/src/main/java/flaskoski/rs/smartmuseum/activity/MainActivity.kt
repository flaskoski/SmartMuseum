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
import flaskoski.rs.smartmuseum.model.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener {

    private val REQUEST_GET_PREFERENCES: Int = 1
    private val REQUEST_ITEM_RATING_CHANGE: Int = 2

    override fun shareOnItemClicked(p1: Int) {
        val viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        viewItemDetails.putExtra("itemHash", itemsList[p1].id)

        startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }

    companion object{
        val user = User("Felipe", "Felipe")
    }

    private val itemsList = ArrayList<Item>()
    private var ratingsList  = ArrayList<Rating>()
    private val TAG = "MainActivity"
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
              //  message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

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
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        //---------------------------------------------------------------------

        parallelRequestsManager = ParallelRequestsManager(2)

        // Access a Cloud Firestore instance from your Activity

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, this)
        itemsGridList.adapter = adapter

        val itemDAO = ItemDAO()
        itemDAO.getAllItems {
            itemsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            buildRecommender()
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            buildRecommender()
        }
    }

    private fun buildRecommender() {
        if(parallelRequestsManager.isComplete!!){
            val recommender = RecommenderBuilder().buildKNNRecommender(ratingsList, applicationContext)

            adapter.recommender = recommender
            adapter.notifyDataSetChanged()
        }
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
            if(requestCode == REQUEST_GET_PREFERENCES || requestCode == REQUEST_ITEM_RATING_CHANGE) {
                Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
                updateRecommender()
            }
        }
    }

    private fun updateRecommender() {
        RatingDAO().getAllItems{
            ratingsList = it as ArrayList<Rating>
            adapter.recommender = RecommenderBuilder().buildKNNRecommender(it, applicationContext)
            adapter.notifyDataSetChanged()
            Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
        }
    }

}
