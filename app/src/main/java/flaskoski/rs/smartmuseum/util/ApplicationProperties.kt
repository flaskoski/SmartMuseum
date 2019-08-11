package flaskoski.rs.smartmuseum.util

import android.os.Build
import android.view.View
import flaskoski.rs.smartmuseum.model.User
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import flaskoski.rs.smartmuseum.DAO.ConfigurationsDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Configurations


object ApplicationProperties {
    //Custom application constants
    var isArrivedIsSet: Boolean = false
    const val WESTERN_POINT : Double = -46.624661
    const val EASTERN_POINT : Double = -46.620273
    const val NORTHERN_POINT : Double = -23.649290
    const val SOUTHERN_POINT : Double = -23.653593

    //technical constants
    const val TAG_RATING_CHANGED_ITEM_ID = "ratingChangedItemId"
    const val TAG_ITEM_RATING_VALUE = "itemRatingValue"
    const val TAG_GO_NEXT_ITEM = "nextItem"
    const val TAG_VISITED_SUBITEMS = "visitedSubItems"
    const val TAG_ARRIVED: String = "arrived"
    const val USER_LOCATION_ITEM_ID: String = "userLocation"
    const val SYSTEM_USER_BASED = "user_based"

    var updateConfigurations : Configurations? = null
    var user : User? = null
    val recommendationSystem: String = SYSTEM_USER_BASED

    fun userNotDefinedYet(): Boolean {
        return user == null
    }

    fun bringToFront(view: View, z_value: Float = 20f){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.translationZ = z_value
            view.bringToFront()
            view.invalidate()
        }
        else {
            view.bringToFront()
            view.parent.requestLayout()
            //sheet_next_items.parent.invalidate()
        }
    }

    fun resetConfigurations() {
        user = null
    }
    private var currentVersion : String? = null
    private var currentVersionCode: Long = 0
    fun getCurrentVersion(context : Context) : String? {
        currentVersion?.let { it }
                ?: try {
                    currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
                catch (e: PackageManager.NameNotFoundException) { e.printStackTrace() }
        return currentVersion
    }

    fun getCurrentVersionCode(context: Context): Long {
        if(currentVersionCode.toInt() == 0)
            try{currentVersionCode =  PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)) }
            catch (e: Exception) { e.printStackTrace() }
        return currentVersionCode
    }

    fun checkForUpdates(currentVersion: Long, callback: (isThereUpdates : Boolean?) -> Unit){
        if(updateConfigurations == null)
            ConfigurationsDAO().getUpdates(){
                if(it == null) callback(null)
                updateConfigurations = it
                callback(updateConfigurations?.latestVersion?:-1 > currentVersion)
            }
        else callback(updateConfigurations!!.latestVersion > currentVersion)
    }

    fun checkIfForceUpdateIsOn(): Boolean? {
        return updateConfigurations?.forceUpdate
    }


}