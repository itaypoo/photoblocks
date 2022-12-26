package com.itaypoo.photoblockslib

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class DayTimeStamp(private val dayTimeString: String): java.io.Serializable {
    // get time string via - SimpleDateFormat("Y,M,d,u,k,m,s")
    val year: Int               // Year (ex. 2022)
    val dayOfMonth: Int         // Day of the month (1-31)
    val dayOfYear: Int          // Day of the year (1-256)
    val minuteOfHour: Int       // Minute of the hour (0-60)
    val minuteOfDay: Int        // Minute of the day (0-60*24)
    val secondOfMinute: Int     // Seconds of the minute (0-60)
    val secondOfHour: Int       // Seconds of the hour (1-3600)
    val secondOfDay: Int        // Seconds of the day (1-86,400)

    val dayOfWeek_number: Int    // Day of the week (1 = Monday, 7 = Sunday)
    val month_number: Int        // Month number (1-12)

    val dayOfWeek_name: String   // Day of the week name (ex. sunday)
    val month_name: String       // Month name (ex. aug.)

    val hourOfDay_24hour: Int    // Hour of the day in 24hour (1-24)
    val hourOfDay_AMPM: Int      // Hour of the day in AMPM (1-12)
    val AMPM: String             // AM or PM

    override fun toString(): String {
        return dayTimeString
    }

    fun secondsLong(): Long{
        var seconds: Long = 0
        // 1 second in 1 second
        seconds += secondOfHour
        // 3600 seconds in 1 hour
        seconds += hourOfDay_24hour * 3600
        // 8,400 seconds in one day
        seconds += dayOfYear * 86400
        // 31,556,926 seconds in one year
        seconds += year * 31556926

        return seconds
    }

    constructor(globalLocale: Boolean) : this(
        if(globalLocale){
            SimpleDateFormat("Y,M,d,D,u,k,m,s").format(Date.from(Instant.now()))
        }
        else{
            SimpleDateFormat("Y,M,d,D,u,k,m,s").format(Calendar.getInstance().time)
        }
    ) {
        // Y = Year (ex. 2022)
        // M = Month number (1-12)
        // d = Day of the month (1-31)
        // D = Day of the year (1-265)
        // u = Day of the week (1 = Monday, ..., 7 = Sunday)
        // k = Hour of day (1-24)
        // m = Minute in hour (0-60)
        // s = Second in minute (0-60)
    }

    init {
        val stringList = dayTimeString.split(",")

        year = stringList[0].toInt()
        month_number = stringList[1].toInt()
        dayOfMonth = stringList[2].toInt()
        dayOfYear = stringList[3].toInt()
        dayOfWeek_number = stringList[4].toInt()
        hourOfDay_24hour = stringList[5].toInt()
        minuteOfHour = stringList[6].toInt()
        secondOfMinute = stringList[7].toInt()

        minuteOfDay = ( hourOfDay_24hour * 60 ) + minuteOfHour
        secondOfHour = ( minuteOfDay * 60 ) + secondOfMinute

        secondOfDay = ( minuteOfDay * 60 ) + secondOfMinute

        val dotwNames = listOf<String>("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        dayOfWeek_name = dotwNames[dayOfWeek_number-1]

        val monthNames = listOf<String>("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        if(month_number == 5){
            month_name = "may" // no dot needed.
        }
        else{
            month_name = monthNames[month_number-1] + "."
        }

        if(hourOfDay_24hour > 12){
            hourOfDay_AMPM = hourOfDay_24hour - 12
            AMPM = "pm"
        }
        else{
            hourOfDay_AMPM = hourOfDay_24hour
            AMPM = "am"
        }
    }

}