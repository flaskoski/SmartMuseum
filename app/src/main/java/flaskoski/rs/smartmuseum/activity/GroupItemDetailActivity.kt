package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.SubItemListAdapter
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import flaskoski.rs.smartmuseum.viewmodel.GroupItemActivityViewModel
import kotlinx.android.synthetic.main.activity_item_detail.*

class GroupItemDetailActivity  : AppCompatActivity(), SubItemListAdapter.OnShareSubItemClickListener{

    private val TAG = "ItemDetails"
    lateinit var starViews : List<ImageView>
    private val ratingTexts = listOf(R.string.rating1, R.string.rating2, R.string.rating3, R.string.rating4, R.string.rating5)

    private lateinit var recommendedSubItemsAdapter: SubItemListAdapter
    private lateinit var otherSubItemsAdapter: SubItemListAdapter

    private lateinit var vm: GroupItemActivityViewModel

    private val REQUEST_SUBITEM_PAGE: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        starViews = listOf<ImageView>(img_star1, img_star2, img_star3, img_star4, img_star5)

        val extras = intent
//        subItems = extras.getSerializableExtra("subItems")?.let { it as List<Itemizable> }
        val rating = extras.getFloatExtra(ApplicationProperties.TAG_ITEM_RATING_VALUE, 0F)

        //<--GroupItem
        vm = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(GroupItemActivityViewModel::class.java)
        vm.arrived = extras.getBooleanExtra(ApplicationProperties.TAG_ARRIVED, false)
        vm.subItemListChangedListener = {
            @Suppress("UNNECESSARY_SAFE_CALL")
            recommendedSubItemsAdapter?.notifyDataSetChanged()
            otherSubItemsAdapter?.notifyDataSetChanged()
        }
        list_recommended_items.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        list_other_items.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recommendedSubItemsAdapter = SubItemListAdapter(vm.recommendedSubItemList, this)
        otherSubItemsAdapter = SubItemListAdapter(vm.otherSubItemList, this)
        list_recommended_items.adapter = recommendedSubItemsAdapter
        list_other_items.adapter = otherSubItemsAdapter

        vm.currentItem = extras.getSerializableExtra("itemClicked") as GroupItem?
        if(vm.currentItem == null){
            Log.e(TAG, "Error when getting currentItem (GroupItem) extra!")
            Toast.makeText(applicationContext, "Erro ao carregar item!", Toast.LENGTH_LONG).show()
            return
        }

        if(!vm.arrived) bt_next_item.visibility = View.GONE
        setStars(rating)
        vm.currentItem?.let {
            supportActionBar?.title = it.title
            vm.itemRating = Rating(ApplicationProperties.user!!.id, it.id, rating, it.recommedationRating,
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
        vm.itemRating!!.rating = (index+1).toFloat()
        setStars(vm.itemRating!!.rating)
        //Toast.makeText(applicationContext, ratingTexts[index], Toast.LENGTH_SHORT).show()


        ApplicationProperties.user?.id?.let {
            vm.itemRating!!.date = ParseTime.getCurrentTime()
            RatingDAO().add(vm.itemRating!!)

        }
        vm.isRatingChanged = true
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
        if(vm.arrived){
            val confirmationDialog = AlertDialog.Builder(this@GroupItemDetailActivity, R.style.Theme_AppCompat_Dialog_Alert)
            confirmationDialog.setTitle("Atenção")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.question_next_item))
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
        if(vm.arrived && vm.itemRating!!.rating == 0F) {
            Snackbar.make(stars, getString(R.string.review_item_request), Snackbar.LENGTH_LONG).show()
            return
        }

        val returnRatingIntent = Intent()
        if(vm.visitedSubItems.isNotEmpty())
            returnRatingIntent.putExtra(ApplicationProperties.TAG_VISITED_SUBITEMS, vm.visitedSubItems)
        if(vm.isRatingChanged){
            ItemRepository.ratingList.remove(vm.itemRating)
            ItemRepository.ratingList.add(vm.itemRating!!)
            returnRatingIntent.putExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID, vm.itemRating?.item)
        }
        if(vm.arrived){
            returnRatingIntent.putExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, goToNextItem)
            returnRatingIntent.putExtra(ApplicationProperties.TAG_ARRIVED, true)
        }
        setResult(Activity.RESULT_OK, returnRatingIntent)
        finish()
    }

    fun goToNextItem(v: View){
        goBack(true)
    }

    //<--GroupItem
    override fun shareOnItemClicked(subItem: SubItem) {
        if(ApplicationProperties.user == null) {
            Toast.makeText(applicationContext,
                    "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG)
                    .show()
        }
//            var subItems : ArrayList<Itemizable>? = null
        val viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
//        val itemId = journeyManager.itemsList[p1].id
//        var itemRating : Float
//        journeyManager.ratingsList.find { it.user == ApplicationProperties.user?.id
//                && it.item == itemId }?.let {
//            itemRating = it.rating
//        }
        vm.currentSubItem = subItem
        viewItemDetails.putExtra("itemClicked", vm.currentSubItem)
        viewItemDetails.putExtra(ApplicationProperties.TAG_ITEM_RATING_VALUE, ItemRepository.ratingList.
                find {vm.currentSubItem?.id == it.item && ApplicationProperties.user?.id == it.user}?.rating)
        ActivityCompat.startActivityForResult(this, viewItemDetails, REQUEST_SUBITEM_PAGE, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_SUBITEM_PAGE-> {
                    vm.subItemVisitedResult(this, data)
                }
            }
        }
    }
    //-->
}
