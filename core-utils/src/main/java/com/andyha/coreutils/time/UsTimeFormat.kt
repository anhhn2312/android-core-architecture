package com.andyha.coreutils.time


class UsTimeFormat constructor(private val is24hFormat: Boolean? = null) : TimeFormat() {

    override val date: String
        get() = "dd"
    override val weekDay: String
        get() = "EEEE"
    override val dayMonthYear: String
        get() = "MM/dd/yyyy"
    override val hourMinute: String
        get() = if (this.is24hFormat == true) "HH:mm" else "hh:mm aa"
}