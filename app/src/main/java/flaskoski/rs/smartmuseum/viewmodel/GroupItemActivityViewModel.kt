package flaskoski.rs.smartmuseum.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import flaskoski.rs.smartmuseum.model.GroupItem
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.SubItem
import flaskoski.rs.smartmuseum.util.ApplicationProperties

class GroupItemActivityViewModel : ViewModel(){
    var isRatingChanged = false
    val recommendedSubItemList = ArrayList<SubItem>()
    val otherSubItemList = ArrayList<SubItem>()
    var currentSubItem : SubItem? = null
    val visitedSubItems = ArrayList<String>()
    var subItemListChangedListener : (() -> Unit)? = null

    var arrived: Boolean = false

    var currentItem: GroupItem? = null
    set(value){
        field = value
        field?.let{ item ->
            val subitems : List<SubItem> = ItemRepository.setRecommendationRatingOnSubItemsOf(item)
            for(subitem in subitems)
                if(subitem.isRecommended)
                    recommendedSubItemList.add(subitem)
                else otherSubItemList.add(subitem)
            subItemListChangedListener?.invoke()
        }

    }

    fun setCurrentSubItem(position : Int){
        currentSubItem = recommendedSubItemList[position]
    }

    fun subItemVisitedResult(activity: Activity, data: Intent?) {
        if(data != null) {
            val rating: Rating? = data.getSerializableExtra(ApplicationProperties.TAG_ITEM_RATING)?.let { it as Rating }
            if (rating != null) {
                ItemRepository.saveRating(rating)
                isRatingChanged = true
            }
            if(arrived)
                currentSubItem?.let {
                    it.isVisited = true
                    visitedSubItems.add(it.id)
                    subItemListChangedListener?.invoke()
                }
        }

    }

}
