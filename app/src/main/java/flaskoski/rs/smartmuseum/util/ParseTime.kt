package flaskoski.rs.smartmuseum.util

import java.util.*
import java.text.SimpleDateFormat


object ParseTime{
    val format = SimpleDateFormat("HH:mm")
    val timeZoneId = "GMT-3:00"
    init{
        format.timeZone = TimeZone.getTimeZone(timeZoneId)
    }
    fun parse(date : String) : Date {
        return format.parse(date)
    }
    fun toString(timeString : Date) : String{
        return format.format(timeString)
    }

    fun differenceInMinutesUntilNow(time : Date): Double {
        return (getCurrentTime().time - time.time)/1000.0/60.0
    }

    fun getCurrentTime(): Date {
        return Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).time
    }


}