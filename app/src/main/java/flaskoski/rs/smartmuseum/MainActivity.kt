package flaskoski.rs.smartmuseum

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
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


        itemsList.add(Item(title = "Item 1", rating = 4.3F))
        itemsList.add(Item(title = "Item 2", rating = 2.0F))
        itemsList.add(Item(title = "Item 3", rating = 3.0F))
        itemsList.add(Item(title = "Item 4", rating = 3.0F))
        itemsList.add(Item(title = "Item 5", rating = 3.0F))
        itemsList.add(Item(title = "Item 6", rating = 3.0F))

        val numberOfColumns = 2
        itemsGridList.setLayoutManager(GridLayoutManager(this, numberOfColumns))
        val adapter = ItemsGridListAdapter(itemsList, applicationContext)
        itemsGridList.setAdapter(adapter)
        //itemsGridList.spanSi
    }

}
