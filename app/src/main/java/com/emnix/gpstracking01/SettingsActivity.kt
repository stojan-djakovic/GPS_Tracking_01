package com.emnix.gpstracking01

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings.*

class SettingsActivity: AppCompatActivity() {

    val PREFS_FILENAME = "com.emnix.gpstracking01.prefs"

    var prefs: SharedPreferences? = null

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.settings)

    radioGroup.setOnCheckedChangeListener{_, checkedId ->
            //val checkedRadioID = radioGroup.checkedRadioButtonId
            //Toast.makeText(this," On checked change : $checkedId", Toast.LENGTH_SHORT).show()
    }


    if (intent.getBooleanExtra("SHOW_WELCOME", false)) {
        Toast.makeText(this, "settings opened", Toast.LENGTH_LONG).show()
    }

    prefs = this.getSharedPreferences(PREFS_FILENAME, 0)


    var configUsername = prefs!!.getString("configUsername", "")
    var configDeviceName = prefs!!.getString("configDeviceName", "")
    var configNormalInterval = prefs!!.getString("configNormalInterval", "60000")
    var configFastInterval = prefs!!.getString("configFastInterval", "30000")
    var configGpsPriority = prefs!!.getString("configGpsPriority", "PRIORITY_HIGH_ACCURACY")

    if (configGpsPriority == "PRIORITY_HIGH_ACCURACY") radioButtonHigh.setChecked(true)

        if (configGpsPriority == "PRIORITY_BALANCED_POWER_ACCURACY")radioButtonBalanced.setChecked(true)

    txtConfigUsername.setText(configUsername)
    txtConfigDeviceName.setText(configDeviceName)
    txtConfigNormalInterval.setText(configNormalInterval)
    txtConfigFastInterval.setText(configFastInterval)


    val brand = Build.BRAND; val model = Build.MODEL ; val product = Build.PRODUCT
    val serial = Build.SERIAL
    val deviceID = "$brand - $model - $serial "

    txtDeviceID.setText(deviceID)
}

    fun saveSettings(View: View) {
        val checkedRadioID = radioGroup.checkedRadioButtonId

        var gpsPriority: String = ""

        when (checkedRadioID) {
            R.id.radioButtonHigh -> gpsPriority = "PRIORITY_HIGH_ACCURACY"
            R.id.radioButtonBalanced -> gpsPriority = "PRIORITY_BALANCED_POWER_ACCURACY"
            else -> gpsPriority = "PRIORITY_HIGH_ACCURACY"
        }
        //Toast.makeText(this, "checkedRadioID: $checkedRadioID ($gpsPriority)", Toast.LENGTH_LONG).show()


        val editor = prefs!!.edit()
        editor.putString("configUsername", txtConfigUsername.text.toString())
        editor.putString("configDeviceName", txtConfigDeviceName.text.toString())
        editor.putString("configNormalInterval", txtConfigNormalInterval.text.toString())
        editor.putString("configFastInterval", txtConfigFastInterval.text.toString())
        editor.putString("configGpsPriority", gpsPriority)

        editor.apply()

        Toast.makeText(this, "settings saved !", Toast.LENGTH_LONG).show()

    }



}