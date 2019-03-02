package flaskoski.rs.smartmuseum

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.recommender.DatabaseIORequests
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    private lateinit var databaseIORequests: DatabaseIORequests

    private var itemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        val extras = Intent()
        itemId = extras.getStringExtra("itemHash")

        databaseIORequests = DatabaseIORequests(applicationContext)
    }

    fun rate(v : View){
        var rating : Float = 1.0F

        txt_rating.visibility = View.VISIBLE
        img_star2.setImageResource(android.R.drawable.btn_star_big_off)
        img_star3.setImageResource(android.R.drawable.btn_star_big_off)
        img_star4.setImageResource(android.R.drawable.btn_star_big_off)
        img_star5.setImageResource(android.R.drawable.btn_star_big_off)
        img_star1.setImageResource(android.R.drawable.btn_star_big_on)

        if(!v.equals(img_star1)){
            img_star2.setImageResource(android.R.drawable.btn_star_big_on)
            if(!v.equals(img_star2)){
                img_star3.setImageResource(android.R.drawable.btn_star_big_on)
                if(!v.equals(img_star3)){
                    img_star4.setImageResource(android.R.drawable.btn_star_big_on)
                    if(!v.equals(img_star4)){
                        img_star5.setImageResource(android.R.drawable.btn_star_big_on)
                        txt_rating.text = "Adorei!"
                        rating = 5F
                    }else{
                        txt_rating.text = "Gostei!"
                        rating = 4F
                    }
                }else{
                    txt_rating.text = "Regular"
                    rating = 3F
                }
            }else {
                txt_rating.text = "Não Gostei!"
                rating = 2F
            }
        }else txt_rating.text = "Muito Ruim!"

        itemId?.let { databaseIORequests.write("Felipe", it, rating) }
    }
}
