package flaskoski.rs.smartmuseum.util

import java.util.*
import java.text.SimpleDateFormat
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

object ParseTime{
    private val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val timeZoneId = "GMT-3:00"
    init{
        format.timeZone = TimeZone.getTimeZone(timeZoneId)
    }
    fun parse(date : String) : Date {
        return format.parse(date)
    }
    fun toString(timeString : Date) : String{
        return format.format(timeString)
    }

    fun toHourString(dateString : Date) : String{
        return timeFormat.format(dateString)
    }

    fun differenceInMinutesUntilNow(time : Date): Double {
        return (getCurrentTime().time - time.time)/1000.0/60.0
    }

    fun getCurrentTime(): Date {
        return Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).time
    }

    fun hourStringToDate(string : String): Date? {
        return try {
            val dateString = dateFormat.format(getCurrentTime())
            format.parse("$dateString $string")
        }catch (_ : Exception){
            null
        }
    }
}