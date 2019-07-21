package flaskoski.rs.smartmuseum.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.util.ApplicationProperties

class GroupItemActivityViewModel : ViewModel(){
    var isRatingChanged = false
    val recommendedSubItemList = ArrayList<SubItem>()
    val otherSubItemList = ArrayList<SubItem>()
    var currentSubItem : SubItem? = null
    val visitedSubItems = ArrayList<String>()
    var subItemListChangedListener : (() -> Unit)? = null

    var itemRating : Rating? = null

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

    fun subItemVisitedResult(activity: Activity, data: Intent?) {
        if(data != null) {
            val ratingChangedItemId: String? = data.getStringExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID)
            if (ratingChangedItemId != null) {
                isRatingChanged = true
            }
            if(arrived || currentItem!!.isVisited)
                currentSubItem?.let {
                    it.isVisited = true
                    visitedSubItems.add(it.id)
                    subItemListChangedListener?.invoke()
                }
        }

    }

}
