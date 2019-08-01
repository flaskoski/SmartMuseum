package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Itemizable
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity  : AppCompatActivity() {

    companion object {
        const val TAG_IS_SUBITEM_VISITED = "isSubItemVisited"
    }

    private var isRatingChanged = false
    private var currentItem : Itemizable? = null
    private val TAG = "ItemDetails"
    private var itemRating : Rating? = null
    lateinit var starViews : List<ImageView>
    private val ratingTexts = listOf(R.string.rating1, R.string.rating2, R.string.rating3, R.string.rating4, R.string.rating5)
    private var arrived: Boolean = false

    private var isSubitem: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        starViews = listOf<ImageView>(img_star1, img_star2, img_star3, img_star4, img_star5)

        val extras = intent
        currentItem = extras.getSerializableExtra("itemClicked") as Itemizable?
        arrived = extras.getBooleanExtra("arrived", false)
        isSubitem = extras.getBooleanExtra(GroupItemDetailActivity.EXTRA_IS_SUBITEM, false)
        val rating = extras.getFloatExtra(ApplicationProperties.TAG_ITEM_RATING_VALUE, 0F)

        //<--ItemDetails
        lb_recommended_items.visibility = View.GONE
        list_recommended_items.visibility = View.GONE
        lb_other_items.visibility = View.GONE
        list_other_items.visibility = View.GONE
        separator_lists.visibility = View.GONE
        //-->

        if(!arrived || isSubitem) bt_next_item.visibility = View.GONE
        setStars(rating)
        currentItem?.let {
            supportActionBar?.title = it.title
            itemRating = Rating(ApplicationProperties.user!!.id, it.id, rating, it.recommedationRating,
                    ApplicationProperties.recommendationSystem,
                    ApplicationProperties.getCurrentVersion(applicationContext)!!,
                    ApplicationProperties.user!!.location?.latitude,
                    ApplicationProperties.user!!.location?.longitude)
            if(it.photoId.isNotBlank())
                ItemRepository.loadImage(applicationContext, imageView, it.photoId)
            else imageView.visibility = View.GONE
            item_description.text = Html.fromHtml( it.description)
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
        lb_avalie.visibility = View.VISIBLE
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
        if(arrived && !isSubitem){
            val confirmationDialog = AlertDialog.Builder(this@ItemDetailActivity, R.style.Theme_AppCompat_Dialog_Alert)
            confirmationDialog.setTitle("Atenção")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.question_next_item))
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        checkIfCanGoBackAndGo(true)
                    }.setNegativeButton(R.string.not_yet){ _, _ ->
                        checkIfCanGoBackAndGo()
                    }
            confirmationDialog.show()
        }
        else{
           checkIfCanGoBackAndGo()
        }
    }

    private fun checkIfCanGoBackAndGo(goToNextItem : Boolean = false){
        if(arrived && itemRating!!.rating == 0F) {
            if(isSubitem)
                AlertDialog.Builder(this@ItemDetailActivity, R.style.Theme_AppCompat_Dialog_Alert).setTitle("Atenção")
                        .setPositiveButton(R.string.yes){_,_->
                            Snackbar.make(stars, getString(R.string.review_item_request), Snackbar.LENGTH_LONG).show()}
                        .setNegativeButton(R.string.no){_,_->
                               goBack()
                        }.setMessage("Você já visitou essa atração?".trimMargin())
                        .show()
            else if(goToNextItem)
                Snackbar.make(stars, getString(R.string.review_item_request), Snackbar.LENGTH_LONG).show()
        }else
            goBack(goToNextItem)
    }

    private fun goBack(goToNextItem : Boolean = false) {
        val returnRatingIntent = Intent()
        if(isRatingChanged){
            ItemRepository.saveRating(itemRating!!)
            returnRatingIntent.putExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID, itemRating?.item)
        }
        if(arrived){
            if(isSubitem){ if(itemRating?.rating == 0F)
                returnRatingIntent.putExtra(TAG_IS_SUBITEM_VISITED, false)}
            else returnRatingIntent.putExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, goToNextItem)
        }
        setResult(Activity.RESULT_OK, returnRatingIntent)
        finish()
    }

    fun goToNextItem(v: View){
        checkIfCanGoBackAndGo(true)
    }
}
