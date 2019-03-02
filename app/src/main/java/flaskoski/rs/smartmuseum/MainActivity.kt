package flaskoski.rs.smartmuseum

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.recommender.DatabaseIORequests
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val itemsList: ArrayList<Item> = ArrayList<Item>()

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

        val user1 = User("Alberto")
        val user2 = User("Beto")
        val user3 = User("Carlos")
        val user4 = User("Diego")

//        val databaseIoRequests : DatabaseIORequests
        if(savedInstanceState == null){
            itemsList.add(Item(id="1", title = "Item 1", avgRating = 4.3F))
            itemsList.add(Item(id="2", title = "Item 2", avgRating = 2.0F))
            itemsList.add(Item(id="3", title = "Item 3", avgRating = 3.0F))
            itemsList.add(Item(id="4", title = "Item 4", avgRating = 3.0F))
            itemsList.add(Item(id="5", title = "Item 5", avgRating = 3.0F))
            itemsList.add(Item(id="6", title = "Item 6", avgRating = 3.0F))

            val ratings = "${user1.id} ${itemsList.get(0).id} 4\n"+
                    "${user1.id} ${itemsList.get(0).id} 3\n"+
                    "${user1.id} ${itemsList.get(1).id} 2\n"+
                    "${user1.id} ${itemsList.get(2).id} 4\n"+
                    "${user1.id} ${itemsList.get(3).id} 4\n"+
                    "${user1.id} ${itemsList.get(4).id} 4\n"+
                    "${user1.id} ${itemsList.get(5).id} 5\n"+
                    "${user2.id} ${itemsList.get(5).id} 1\n"+
                    "${user2.id} ${itemsList.get(3).id} 3\n"+
                    "${user2.id} ${itemsList.get(1).id} 3\n"+
                    "${user3.id} ${itemsList.get(0).id} 4\n"+
                    "${user3.id} ${itemsList.get(2).id} 2\n"+
                    "${user3.id} ${itemsList.get(4).id} 3\n"+
                    "${user3.id} ${itemsList.get(5).id} 5\n"+
                    "${user4.id} ${itemsList.get(0).id} 2\n"+
                    "${user4.id} ${itemsList.get(1).id} 3\n"+
                    "${user4.id} ${itemsList.get(2).id} 3\n"+
                    "Felipe ${itemsList.get(2).id} 3\n"
            DatabaseIORequests(applicationContext, ratings)
        }
        else
            DatabaseIORequests(applicationContext)

        val recommender = RecommenderBuilder().buildKNNRecommender("ratings.txt", applicationContext)
        val numberOfColumns = 2
        itemsGridList.layoutManager = GridLayoutManager(this, numberOfColumns)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, recommender)
        itemsGridList.adapter = adapter



    }

    override fun onResume() {
        super.onResume()
        adapter.recommender = RecommenderBuilder().buildKNNRecommender("ratings.txt", applicationContext)
        adapter.notifyDataSetChanged()
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
