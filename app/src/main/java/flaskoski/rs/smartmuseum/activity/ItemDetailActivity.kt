package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.app.Application
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    private var itemRating: Float = 0F
    private var isRatingChanged = false
    private var currentItem : Item? = null
    private val TAG = "ItemDetails"
    lateinit var starViews : List<ImageView>
    val ratingTexts = listOf(R.string.rating1, R.string.rating2, R.string.rating3, R.string.rating4, R.string.rating5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        starViews = listOf<ImageView>(img_star1, img_star2, img_star3, img_star4, img_star5)

        val extras = intent
        currentItem = extras.getSerializableExtra("itemClicked") as Item?
        itemRating = extras.getFloatExtra("itemRating", 0F)
        setStars(itemRating)
        currentItem?.let {
            imageView.setImageResource(this.resources.getIdentifier(it.photoId, "drawable", applicationContext.packageName))
            item_description.text = it.description
        }
    }
    private fun setStars(rating: Float) {
        var count = 0
        starViews.forEach{
            if(count++ < rating)
                it.setImageResource(android.R.drawable.btn_star_big_on)
            else
                it.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }

    fun rate(v : View){
        var rating : Float = 1.0F

        txt_rating.visibility = View.VISIBLE
        val index = starViews.indexOf(v)
        rating = (index+1).toFloat()
        setStars(rating)
        txt_rating.setText(ratingTexts[index])


//        if(!v.equals(img_star1)){
//            img_star2.setImageResource(android.R.drawable.btn_star_big_on)
//            if(!v.equals(img_star2)){
//                img_star3.setImageResource(android.R.drawable.btn_star_big_on)
//                if(!v.equals(img_star3)){
//                    img_star4.setImageResource(android.R.drawable.btn_star_big_on)
//                    if(!v.equals(img_star4)){
//                        img_star5.setImageResource(android.R.drawable.btn_star_big_on)
//                        txt_rating.text = "Adorei!"
//                        rating = 5F
//                    }else{
//                        txt_rating.text = "Gostei!"
//                        rating = 4F
//                    }
//                }else{
//                    txt_rating.text = "Regular"
//                    rating = 3F
//                }
//            }else {
//                txt_rating.text = "Não Gostei!"
//                rating = 2F
//            }
//        }else txt_rating.text = "Muito Ruim!"

        ApplicationProperties.user?.id?.let {
            RatingDAO().add(Rating(it, currentItem!!.id, rating))
        }
        isRatingChanged = true
    }

    override fun onBackPressed() {
        if(!isRatingChanged) super.onBackPressed()

        setResult(Activity.RESULT_OK)
        finish()
    }
}
