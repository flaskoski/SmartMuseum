package flaskoski.rs.smartmuseum.DAO

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*

class SharedPreferencesDAO(activity : Activity){
    private val USER_ID = "userId"
    private val USER_AGE = "userAge"
    private val START_TIME = "startTime"
    private val TIME_AVAILABLE = "timeAvailable"
    private val ALREADY_VISITED: String = ""
    private val ANDROID_ID: String = "androidId"
    private val ITEM_PREFIX = "item_"
    private val TERMS_ACCEPTED = "termsAccepted"
    private val IS_QUESTIONNAIRE_ANSWERED = "isQuesAnswered"

    private var db: SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    var isSavedItemsSynchronized: Boolean = false

    fun saveUser(user : User){
        with(db.edit()){
            putString(USER_ID, user.id)
            putString(ANDROID_ID, user.android_id)
            putInt(USER_AGE, user.age)
            putBoolean(ALREADY_VISITED, user.alreadyVisited)
            putBoolean(TERMS_ACCEPTED, user.termsAccepted)
            putFloat(TIME_AVAILABLE, user.timeAvailable.toFloat())
            apply()
        }
    }

    fun getUser(): User? {
        val userId = db.getString(USER_ID, "")
        val androidID = db.getString(ANDROID_ID, "")
        val userAge = db.getInt(USER_AGE, -1)
        val alreadyVisited = db.getBoolean(ALREADY_VISITED, false)
        val termsAccepted = db.getBoolean(TERMS_ACCEPTED, false)
        val userTimeAvailable = db.getFloat(TIME_AVAILABLE, -1f).toDouble()
        if(userId!!.isNotBlank() && userTimeAvailable > 0)
            return User(userId, userAge, alreadyVisited, userTimeAvailable, androidId = androidID, termsAccepted = termsAccepted)
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
            return ParseTime.parse(startTime!!)
        return null
    }
    fun saveTimeAvailable(){

    }

    fun getAllRecommendedItemStatus() : HashSet<Itemizable>{
        val allRecommendedItems = HashSet<Itemizable>()
        db.all.filter { it.value is Boolean && it.key.contains(ITEM_PREFIX) }.forEach {
            val values = it.key.split("_")
            if(values[2].toInt() == 0)
                allRecommendedItems.add(SubItem(values[1], isVisited = it.value as Boolean))
            else
                allRecommendedItems.add(Item(values[1], recommendedOrder = values[2].toInt(), isVisited =  it.value as Boolean))
        }
        isSavedItemsSynchronized = true
        return allRecommendedItems
    }

    fun setAllRecommendedItems(recommendedItems: List<Item>, recommendedSubItems: List<SubItem>){
        val allRecommendedItems = ArrayList<Itemizable>()
        allRecommendedItems.addAll(recommendedItems)
        allRecommendedItems.addAll(recommendedSubItems)

        with(db.edit()){
            db.all.filter { it.value is Boolean && it.key.contains(ITEM_PREFIX) }.keys.forEach {
                remove(it)
            }
            allRecommendedItems.forEach {
                if(it is RoutableItem)
                    putBoolean("${ITEM_PREFIX}${it.id}_${it.recommendedOrder}", it.isVisited)
                else putBoolean("${ITEM_PREFIX}${it.id}_0", it.isVisited)
            }
            apply()
            isSavedItemsSynchronized = true
        }
    }

    /***
     * Set item isVisited control variable. Do not use this function in case of change in the recommended route.
     * For that, use @function setAllRecommendedItems.
     */
    fun setRecommendedItem(it : Itemizable){
        with(db.edit()){

            if(it is RoutableItem)
                putBoolean("${ITEM_PREFIX}${it.id}_${it.recommendedOrder}", it.isVisited)
            else putBoolean("${ITEM_PREFIX}${it.id}_0", it.isVisited)
            apply()
            isSavedItemsSynchronized = false
        }
    }

    fun clear(){
        with(db.edit()){
            clear()
            apply()
            isSavedItemsSynchronized = false
        }
    }
    fun resetJourney(){
        with(db.edit()){
            db.all.filter { it.value is Boolean && it.key.contains(ITEM_PREFIX) }.keys.forEach {
                remove(it)
            }
            remove(START_TIME)
            apply()
            isSavedItemsSynchronized = false
        }
    }

    fun setVisitedSubItems(visitedSubItems: List<String>) {
        with(db.edit()){
            visitedSubItems.forEach {
                putBoolean("${ITEM_PREFIX}${it}_0", true)
            }
            apply()
            isSavedItemsSynchronized = false
        }
    }

    fun removeItem(itemToBeRemoved: Item) {
        with(db.edit()){
            remove("${ITEM_PREFIX}${itemToBeRemoved.id}_${itemToBeRemoved.recommendedOrder}")
            apply()
            isSavedItemsSynchronized = false
        }
    }

    fun getIsQuestionnaireAnswered(): Boolean {
        return db.getBoolean(IS_QUESTIONNAIRE_ANSWERED, false)
    }

    fun setIsQuestionnaireAnswered(flag : Boolean){
        with(db.edit()) {
            putBoolean(IS_QUESTIONNAIRE_ANSWERED, flag)
            apply()
        }
    }
}