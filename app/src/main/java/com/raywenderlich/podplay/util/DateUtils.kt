package com.raywenderlich.podplay.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {
    fun jsonDateToShortDate(jsonDate: String?): String {
//1
        if (jsonDate == null) {
            return "-"
        }
// 2
        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
// 3
        val date = inFormat.parse(jsonDate)
// 4
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT,
            Locale.getDefault())
// 6
        return outputFormat.format(date)
    }

    fun xmlDateToDate(date: String?): Date {
        val date = date ?: return Date()
        val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")
        return inFormat.parse(date)
    }
    fun dateToShortDate(date: Date): String {
        val outputFormat = DateFormat.getDateInstance(
            DateFormat.SHORT, Locale.getDefault())
        return outputFormat.format(date)
    }
}