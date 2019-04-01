package flaskoski.rs.smartmuseum.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.firestore.FirebaseFirestore
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Rating
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    private var itemId: String? = null
    private val TAG = "ItemDetails"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        val extras = intent
        itemId = extras.getStringExtra("itemHash")

    }

    fun rate(v : View){
        var rating = 1.0F

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

        itemId?.let {
            val ratingDAO = RatingDAO()
            ratingDAO.add(Rating("Felipe", it, rating))
        }
    }
}
