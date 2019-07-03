package flaskoski.rs.smartmuseum.model

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.data.DataBufferObserver
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
//import javax.inject.Inject
//import javax.inject.Singleton

//@Singleton
object ItemRepository //@Inject constructor
{

    var isItemListLoaded = ObservableBoolean(false)
    var isRatingListLoaded = ObservableBoolean(false)
    var itemList = ArrayList<Item>()
    var subItemList  = ArrayList<SubItem>()
    var ratingList = HashSet<Rating>()
    var allElements = HashSet<Element>()
    val itemDAO = ItemDAO()

    init{
        itemDAO.getAllPoints { elements ->
//            recommendedRouteBuilder = RecommendedRouteBuilder(elements)
//            lastItem = recommendedRouteBuilder?.getAllEntrances()?.first()
//            @Suppress("UNCHECKED_CAST")
            allElements = elements as HashSet<Element>
            itemList.addAll(elements.filter { it is Item || it is GroupItem }.let {list -> list  as List<Item>})
            subItemList.addAll(elements.filter { it is SubItem}.let {list -> list as List<SubItem> } )
            isItemListLoaded.set(true)
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingList.addAll(it)
            isRatingListLoaded.set(true)
        }
    }
    fun saveRating(rating : Rating){
        ratingList.remove(rating)
        ratingList.add(rating)
    }
}