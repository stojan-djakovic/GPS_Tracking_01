package com.emnix.gpstracking01

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build.*
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.util.regex.Pattern


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d(TAG, "SMS Received!")

        val txt = getTextFromSms(intent?.extras)
        Log.d(TAG, "message=" + txt)

        val code = validateTrackerCode(txt)
        Log.d(TAG, "code=" + code)

        code?.let { handleUrl(context, it) }
    }

    private fun handleUrl(context: Context?, code: String) {
        if (!code.isBlank()) {
            Log.d(TAG,"handle: $code")
                lateinit var fusedLocationClient: FusedLocationProviderClient
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)

                fusedLocationClient.lastLocation.addOnSuccessListener {
                    Log.d(TAG, "onSuccessListener")
                    Log.d(TAG, "$it")
                    val lat = it.latitude
                    val lon = it.longitude
                    Log.d(TAG, "lat: $lat | lon: $lon")

                    val queue = Volley.newRequestQueue(context!!)
                    val url = "http://gps.emnix.com/post_sms_response/"
                    val params = JSONObject()
                    val imei    = context!!.getSharedPreferences("_", MODE_PRIVATE).getString("imei", "")

                    params.put("lat",lat)
                    params.put("lon",lon)
                    params.put("code",code)
                    params.put("imei",imei)

                    Log.d(TAG,"rootObj: $params")

                    val jsonObjectRequest = JsonObjectRequest(
                        Request.Method.POST, url, params ,
                        Response.Listener { response ->
                            Log.d(TAG, "Response: " + response.toString())
                            val r_status = response.getString("status")
                            val r_message = response.getString("message")
                            Log.d(TAG,"status: $r_status | message: $r_message")
                        },
                        Response.ErrorListener { error ->
                            Log.d(TAG, "Response error : " + error.toString())

                        }
                    )

                    queue.add(jsonObjectRequest)

                    /*
                    send sms with data
                     */
                    val phoneNo:String = "+381649180279"
                    val smsManager = SmsManager.getDefault() as SmsManager
                    smsManager.sendTextMessage(phoneNo, null, "sms message", null, null)

                }


        } else {
            Log.d(TAG,"can not handle empty code $code")

        }
    }

    private fun getTextFromSms(extras: Bundle?): String {
        val pdus = extras?.get("pdus") as Array<*>
        val format = extras?.getString("format")
        var txt = ""
        for (pdu in pdus) {
            val smsmsg = getSmsMsg(pdu as ByteArray?, format)
            val submsg = smsmsg?.displayMessageBody
            submsg?.let { txt = "$txt$it" }
        }
        return txt
    }

    private fun getSmsMsg(pdu: ByteArray?, format: String?): SmsMessage? {
        when {
            VERSION.SDK_INT >= VERSION_CODES.M -> return SmsMessage.createFromPdu(pdu, format)
            else -> return SmsMessage.createFromPdu(pdu)
        }
    }

    private fun validateTrackerCode(msg: String) : String? {
        // the pattern we want to search for
        val p = Pattern.compile("<t>(\\S+)</t>")
        val m = p.matcher(msg)
        if (m.find()) {
            // get the matching group
            val codeGroup = m.group(1)
            // print the group
            Log.d(TAG,"Found: $codeGroup \n")
            return codeGroup
        }
        return ""
    }

    companion object {
        private val TAG = SmsReceiver::class.java.simpleName
    }
}