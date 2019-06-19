//package flaskoski.rs.smartmuseum.activity
//
//import android.app.Dialog
//import android.app.TimePickerDialog
//import android.os.Bundle
//import android.support.v4.app.DialogFragment
//import android.text.format.DateFormat
//import android.view.View
//import android.widget.EditText
//import android.widget.TimePicker
//
//class TimeAvailableFragment() : DialogFragment(), TimePickerDialog.OnTimeSetListener {
//
//    var timeView : EditText? = null
//    var timeAvailable : Double? = null
//
//    fun setTimeView(v : View) : TimeAvailableFragment{
//        timeView = v as EditText
//        return this
//    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        // Create a new instance of TimePickerDialog and return it
//        return TimePickerDialog(activity, this, 1, 0, DateFormat.is24HourFormat(activity))
//    }
//
//    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
//        timeView?.setText("${hourOfDay}h${minute}min")
//        timeAvailable = hourOfDay*60.0 + minute
//    }
//}