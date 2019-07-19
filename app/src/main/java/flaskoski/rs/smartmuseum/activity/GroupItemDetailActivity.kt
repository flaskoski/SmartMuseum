package flaskoski.rs.smartmuseum.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var itemRating : Rating? = null
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
        val rating = extras.getFloatExtra("itemRating", 0F)

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
            itemRating = Rating(ApplicationProperties.user!!.id, it.id, rating, it.recommedationRating, recommendationSystem = ApplicationProperties.recommendationSystem)
            if(it.photoId.isNotBlank())
                ItemRepository.loadImage(applicationContext, imageView, it.photoId)
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
            RatingDAO().add(this!!.itemRating!!)

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
        if(vm.visitedSubItems.isNotEmpty())
            returnRatingIntent.putExtra(ApplicationProperties.TAG_VISITED_SUBITEMS, vm.visitedSubItems)
        if(vm.isRatingChanged)
            returnRatingIntent.putExtra(ApplicationProperties.TAG_ITEM_RATING, itemRating)
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
        viewItemDetails.putExtra(ApplicationProperties.TAG_ITEM_RATING, ItemRepository.ratingList.find {vm.currentSubItem?.id == it.item && ApplicationProperties.user?.id == it.user}?.rating)
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
