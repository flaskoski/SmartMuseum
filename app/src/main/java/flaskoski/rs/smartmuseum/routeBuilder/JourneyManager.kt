package flaskoski.rs.smartmuseum.routeBuilder
import android.util.Log
import flaskoski.rs.smartmuseum.model.Point
import java.lang.IllegalStateException
import java.util.*

class JourneyManager(){

    var museumGraph: MuseumGraph?  = null
    var closestItem: Point? = null
    var previousItem : Point? = null
    val TAG = "JourneyManager"


    var isPreferencesSet = false
    var isItemsAndRatingsLoaded: Boolean = false
    var isJourneyBegan: Boolean = false
    var timeAvailable: Double = 120.0

    fun build(points: List<Point>){
        museumGraph = MuseumGraph(points.toHashSet())
        previousItem = museumGraph?.entrances?.first()
    }

    private fun isBuilt() : Boolean{
        return museumGraph != null
    }

    fun getNextClosestItem(): Point? {
        //TODO: Has to catch the closest entrance to the user
        if(this.museumGraph?.entrances == null) {
            Log.e(TAG, "no entrances found!")
            throw Exception("No entrances found in the graph!")
        }

        previousItem = this.closestItem ?: museumGraph?.entrances?.first()
        previousItem?.isClosest = false
        closestItem = museumGraph?.getClosestItemTo(previousItem!!)
        closestItem?.isClosest = true
        return closestItem
    }

    fun getRecommendedRoute() {

    }


//    var state : JourneyStates? = null
//        set(newState : JourneyStates?){
//        val lastState = field
//        field = newState
//        when (field) {
//            PREFERENCES_NOT_SET -> onStateChangeCallback.onStatePreferencesNotSet(lastState)
//            BEGIN -> onStateChangeCallback.onStateBegin(lastState)
//            NEAR_ITEM -> onStateChangeCallback.onStateNearItem(lastState)
//            NEW_ITEM -> onStateChangeCallback.onStateNewItem(lastState)
//        }
//    }
//
//    interface OnStateChangeCallback{
//        fun onStatePreferencesNotSet(lastState: JourneyStates?)
//        fun onStateBegin(lastState: JourneyStates?)
//        fun onStateNearItem(lastState: JourneyStates?)
//        fun onStateNewItem(lastState: JourneyStates?)
//    }
}