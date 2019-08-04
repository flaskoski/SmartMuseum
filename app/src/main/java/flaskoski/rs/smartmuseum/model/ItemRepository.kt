package flaskoski.rs.smartmuseum.model

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.data.DataBufferObserver
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import kotlinx.android.synthetic.main.grid_item.view.*
import java.io.IOException
import java.io.InputStream

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
    val recommenderManager = RecommenderManager()

    init{
        itemDAO.getAllPoints { elements ->
//            recommendedRouteBuilder = RecommendedRouteBuilder(elements)
//            lastItem = recommendedRouteBuilder?.getAllEntrances()?.first()
//            @Suppress("UNCHECKED_CAST")
            allElements = elements as HashSet<Element>
            @Suppress("UNCHECKED_CAST")itemList.addAll(elements.filter { (it is Item || it is GroupItem)
                    && !(it as Itemizable).isClosed }.let {list -> list  as List<Item>})
            @Suppress("UNCHECKED_CAST")subItemList.addAll(elements.filter { it is SubItem && !it.isClosed}
                    .let { list -> list as List<SubItem> } )
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

    fun resetJourney() {
        allElements.filter { it is Itemizable}.forEach {
            (it as Itemizable).isVisited = false
            if(it is RoutableItem) it.recommendedOrder = Int.MAX_VALUE
        }

        ratingList.removeAll( ratingList.filter { it.user == ApplicationProperties.user?.id && it.type != Rating.TYPE_FEATURE} )
    }

    fun setRecommendationRatingOnSubItemsOf(groupItem : GroupItem) : List<SubItem>{
        val subItems = ItemRepository.subItemList.filter { subitem -> subitem.groupItem == groupItem.id }.let {it}
        subItems.forEach{subitem ->
            subitem.recommedationRating = recommenderManager.getPrediction(ApplicationProperties.user!!.id, subitem.id)?.let { it }?: 0f
        }
        return subItems
    }

    fun resetRecommendedOrder() {
        itemList.forEach { it.recommendedOrder = Int.MAX_VALUE }
    }

    fun loadImage(context: Context, imageView: ImageView, photoId: String) {
        val ims = getInputStreamImage(context, photoId)
        if(ims != null)
            imageView.setImageDrawable(Drawable.createFromStream(ims,null))
        else{
            val identifier = context.resources.getIdentifier(photoId, "drawable",context.packageName)
            if(identifier != 0)
                imageView.setImageResource(identifier)
        }
    }

    fun loadBackgroundPhoto(context: Context, view: View, photoId: String){
        val ims = getInputStreamImage(context, photoId)
        if(ims != null)
            view.background = Drawable.createFromStream(ims,null)
        else{
            val identifier = context.resources.getIdentifier(photoId, "drawable",context.packageName)
            if(identifier != 0)
                view.setBackgroundResource(identifier)
        }
    }
    private fun getInputStreamImage(context : Context, photoId : String) : InputStream? {
        val extensions = listOf("jpg", "jpeg")
        for(e in extensions)
            try { return context.assets.open("${photoId}.${e}")
            } catch (_: IOException) {}
        return null
    }

}