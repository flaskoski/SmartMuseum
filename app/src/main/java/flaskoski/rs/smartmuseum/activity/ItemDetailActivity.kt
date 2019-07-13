package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Itemizable
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    private var isRatingChanged = false
    private var currentItem : Itemizable? = null
    private val TAG = "ItemDetails"
    private var itemRating : Rating? = null
    lateinit var starViews : List<ImageView>
    private val ratingTexts = listOf(R.string.rating1, R.string.rating2, R.string.rating3, R.string.rating4, R.string.rating5)
    private var arrived: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        starViews = listOf<ImageView>(img_star1, img_star2, img_star3, img_star4, img_star5)

        val extras = intent
        currentItem = extras.getSerializableExtra("itemClicked") as Itemizable?
        arrived = extras.getBooleanExtra("arrived", false)
        val rating = extras.getFloatExtra(ApplicationProperties.TAG_ITEM_RATING, 0F)

        //<--ItemDetails
        lb_recommended_items.visibility = View.GONE
        recommended_items_list.visibility = View.GONE
        //-->

        if(!arrived) bt_next_item.visibility = View.GONE
        setStars(rating)
        currentItem?.let {
            supportActionBar?.title = it.title
            itemRating = Rating(ApplicationProperties.user!!.id, it.id, rating, it.recommedationRating, recommendationSystem = ApplicationProperties.recommendationSystem)
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
        if(rating > 0f) lb_avalie.setText(ratingTexts[rating.toInt()-1])
    }

    fun rate(v : View){
        //txt_rating.visibility = View.VISIBLE
        val index = starViews.indexOf(v)
        itemRating!!.rating = (index+1).toFloat()
        setStars(itemRating!!.rating)
        //Toast.makeText(applicationContext, ratingTexts[index], Toast.LENGTH_SHORT).show()


        ApplicationProperties.user?.id?.let {
            itemRating!!.date = ParseTime.getCurrentTime()
            RatingDAO().add(this.itemRating!!)

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
        if(arrived){
            val confirmationDialog = AlertDialog.Builder(this@ItemDetailActivity, R.style.Theme_AppCompat_Dialog_Alert)
            confirmationDialog.setTitle("Atenção")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Deseja ir ao próximo item da visita?")
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        goBack(true)
                    }.setNegativeButton(R.string.not_yet){ _, _ ->
                        goBack()
                    }
            confirmationDialog.show()
        }
        else{
           goBack()
        }
    }

    private fun goBack(goToNextItem : Boolean = false){
        val returnRatingIntent = Intent()
        if(isRatingChanged)
            returnRatingIntent.putExtra(ApplicationProperties.TAG_ITEM_RATING, itemRating)
        if(arrived) returnRatingIntent.putExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, goToNextItem)
        setResult(Activity.RESULT_OK, returnRatingIntent)
        finish()
    }

    fun goToNextItem(v: View){
        goBack(true)
    }
}
