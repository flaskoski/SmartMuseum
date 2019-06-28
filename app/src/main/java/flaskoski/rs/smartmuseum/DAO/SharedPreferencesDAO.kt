package flaskoski.rs.smartmuseum.DAO

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*

class SharedPreferencesDAO(activity : Activity){
    val USER_ID = "userId"
    val USER_NAME = "userName"
    val START_TIME = "startTime"
    val TIME_AVAILABLE = "timeAvailable"
    private val ITEM_PREFIX = "item_"

    private var db: SharedPreferences

    init{
        db = activity.getPreferences(Context.MODE_PRIVATE)
    }

    fun saveUser(user : User){
        with(db.edit()){
            putString(USER_ID, user.id)
            putString(USER_NAME, user.name)
            putFloat(TIME_AVAILABLE, user.timeAvailable.toFloat())

            apply()
        }
    }

    fun getUser(): User? {
        var userId = db.getString(USER_ID, "")
        var userName = db.getString(USER_NAME, "")
        var userTimeAvailable = db.getFloat(TIME_AVAILABLE, -1f).toDouble()
        if(userId.isNotBlank() && userTimeAvailable > 0)
            return User(userId, userName, userTimeAvailable)
        return null
    }

    fun saveStartTime(startTime: Date) {
        with(db.edit()){
            putString(START_TIME, ParseTime.toString(startTime))
            apply()
        }

    }
    fun getStartTime(): Date? {
        val startTime = db.getString(START_TIME, "")
        if(startTime != "")
            return ParseTime.parse(startTime)
        return null
    }
    fun saveTimeAvailable(){

    }

    fun getAllRecommendedItemStatus() : Map<String, Pair<Int, Boolean>>{
        val allPairs = HashMap<String, Pair<Int, Boolean>>()
        db.all.filter { it.value is Boolean && it.key.contains(ITEM_PREFIX) }.forEach {
            val values = it.key.split("_")
            allPairs.put(values[1], Pair(values[2].toInt(), it.value as Boolean))
        }
        return allPairs
    }

    fun setAllRecommendedItems(recommendedItems : Set<Item>){
        db.all.filter { it.value is Boolean && it.key.contains(ITEM_PREFIX) }.keys.forEach {
            db.all.remove(it)
        }
        with(db.edit()){
            recommendedItems.forEach {
                putBoolean("${ITEM_PREFIX}${it.id}_${it.recommendedOrder}", it.isVisited)
            }
            apply()
        }
    }

    /***
     * Set item isVisited control variable. Do not use this function in case of change in the recommended route.
     * For that, use @function setAllRecommendedItems.
     */
    fun setRecommendedItem(item : Item){
        with(db.edit()){
            putBoolean("${ITEM_PREFIX}${item.id}_${item.recommendedOrder}", item.isVisited)
            apply()
        }
    }

    fun clear(){
        db.all.clear()
    }
}