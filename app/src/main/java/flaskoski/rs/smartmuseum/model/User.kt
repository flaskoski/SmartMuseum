package flaskoski.rs.smartmuseum.model

import com.google.android.gms.maps.model.LatLng

class User(val id: String, var age: Int, var timeAvailable: Double, var location : LatLng? = null) {
    companion object {
        const val FIELD_AGE = "userAge"
    }

    fun getAgeGroup() : Float {
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

}
