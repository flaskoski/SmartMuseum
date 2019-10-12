package flaskoski.rs.smartmuseum.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import flaskoski.rs.smartmuseum.activity.ItemDetailActivity
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
            val subitems : ArrayList<SubItem> = ItemRepository.setRecommendationRatingOnSubItemsOf(item) as ArrayList<SubItem>
            var i=0
            subitems.sortedWith(compareBy<Itemizable>{it.title}).forEach{
                subitems[i++] = it
            }
            recommendedSubItemList.clear()
            otherSubItemList.clear()
            for(subitem in subitems)
                if(subitem.isRecommended)
                    recommendedSubItemList.add(subitem)
                else otherSubItemList.add(subitem)
            subItemListChangedListener?.invoke()
        }

    }

    fun subItemVisitedResult(data: Intent?) {
        if(data != null) {
            val ratingChangedItemId: String? = data.getStringExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID)
            val isSubItemVisited: Boolean = data.getBooleanExtra(ItemDetailActivity.TAG_IS_SUBITEM_VISITED, true)
            if (ratingChangedItemId != null) {
                isRatingChanged = true
            }
            if((arrived || currentItem!!.isVisited) && isSubItemVisited)
                currentSubItem?.let {
                    it.isVisited = true
                    visitedSubItems.add(it.id)
                    subItemListChangedListener?.invoke()
                }
        }

    }

}
