package flaskoski.rs.smartmuseum.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val itemsList: ArrayList<Item> = ArrayList<Item>()
    private val ratings = ArrayList<Rating>()
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
        val db = FirebaseFirestore.getInstance()

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext)
        itemsGridList.adapter = adapter



        //add items to grid from DB
        db.collection("items")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        Log.d(TAG, document.id + " => " + document.data)
                        val item = document.toObject(Item::class.java)
                        item.id = document.id
                        itemsList.add(item)
                    }
                    parallelRequestsManager.decreaseRemainingRequests()
                    buildRecommender()
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                    Toast.makeText(applicationContext, "Erro ao obter informações! Verifique sua conexão com a internet.", Toast.LENGTH_LONG)
                }
        db.collection("ratings")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        ratings.add(document.toObject(Rating::class.java))
                    }
                    parallelRequestsManager.decreaseRemainingRequests()
                    buildRecommender()
                }.addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                    Toast.makeText(applicationContext, "Erro ao obter informações! Verifique sua conexão com a internet.", Toast.LENGTH_LONG)
                }
    }

    private fun buildRecommender() {
        if(parallelRequestsManager.isComplete!!){
            val recommender = RecommenderBuilder().buildKNNRecommender("ratings.txt", ratings, applicationContext)

            adapter.recommender = recommender
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
//        adapter.recommender = RecommenderBuilder().buildKNNRecommender("ratings.txt", applicationContext)
//        adapter.notifyDataSetChanged()
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
                startActivity(goToFeaturePreferences)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

}
