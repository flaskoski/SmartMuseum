package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    private var isRatingChanged = false
    private var currentItem : Item? = null
    private val TAG = "ItemDetails"
    private lateinit var itemRating : Rating
    lateinit var starViews : List<ImageView>
    private val ratingTexts = listOf(R.string.rating1, R.string.rating2, R.string.rating3, R.string.rating4, R.string.rating5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        starViews = listOf<ImageView>(img_star1, img_star2, img_star3, img_star4, img_star5)

        val extras = intent
        currentItem = extras.getSerializableExtra("itemClicked") as Item?
        val arrived = extras.getBooleanExtra("arrived", false)
        val rating = extras.getFloatExtra("itemRating", 0F)

        if(!arrived) bt_next_item.visibility = View.GONE
        setStars(rating)
        currentItem?.let {
            itemRating = Rating(ApplicationProperties.user!!.id, it.id, rating)
            if(it.photoId.isNotBlank())
                imageView.setImageResource(this.resources.getIdentifier(it.photoId, "drawable", applicationContext.packageName))
            else imageView.visibility = View.GONE
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
        //txt_rating.visibility = View.VISIBLE
        val index = starViews.indexOf(v)
        itemRating.rating = (index+1).toFloat()
        setStars(itemRating.rating)
        Toast.makeText(applicationContext, ratingTexts[index], Toast.LENGTH_SHORT).show()
//        txt_rating.setText(ratingTexts[index])

        ApplicationProperties.user?.id?.let {
            RatingDAO().add(itemRating)

        }
        isRatingChanged = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if(!isRatingChanged){
            super.onBackPressed()
            finish()
        }

        val returnRatingIntent = Intent()
        returnRatingIntent.putExtra("itemRating", itemRating)
        setResult(Activity.RESULT_OK, returnRatingIntent)
        finish()
    }

    fun goToNextItem(v: View){
        val returnRatingIntent = Intent()
        if(isRatingChanged)
            returnRatingIntent.putExtra("itemRating", itemRating)
        returnRatingIntent.putExtra("nextItem", true)
        setResult(Activity.RESULT_OK, returnRatingIntent)
        finish()
    }
}
