package flaskoski.rs.smartmuseum.model

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.android.gms.maps.model.LatLng
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class User(val id: String, var age: Int? = null,
           var alreadyVisited: Boolean = false,
           var timeAvailable: Double, var location: LatLng? = null,
           context: Context? = null, androidId : String? = "",
           var termsAccepted : Boolean = false) {

    @SuppressLint("HardwareIds")
    val android_id : String = context?.let {
        Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID);
    }?: androidId?.let { it }?: ""

    companion object {
        const val FIELD_AGE = "userAge"
    }

    fun getAgeGroup() : Float? {
        age?.let { age ->
            return when(age){
                in 0..12 ->
                    age/12F
                in 13..17 ->
                    1+(age-12)/5f
                in 18..24 ->
                    2+(age-17)/7f
                in 25..59 ->
                    3+(age-24)/35f
                in 60..100->
                    4+(age-59)/41f
                else ->
                    5f
            }
        }
        return null
    }

}
