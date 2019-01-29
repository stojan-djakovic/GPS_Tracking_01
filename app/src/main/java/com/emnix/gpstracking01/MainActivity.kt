package com.emnix.gpstracking01

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import kotlinx.android.synthetic.main.activity_main.*

import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.Response
import android.os.Build;
import android.os.Handler
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Math.abs

import java.text.SimpleDateFormat
import java.util.Calendar
import java.math.BigDecimal
import java.math.RoundingMode

/*import com.google.android.gms.location.sample.basiclocationsample.BuildConfig.APPLICATION_ID*/

class MainActivity : AppCompatActivity() {
    val PREFS_FILENAME = "com.emnix.gpstracking01.prefs"
    var prefs: SharedPreferences? = null


    var lastLocationLat: Double = 0.0
    var lastLocationLon: Double = 0.0

    var lastSendedLocationLat: Double = 0.0
    var lastSendedLocationLon: Double = 0.0


    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    private val TAG = "MainActivity GPS Location"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    val parseFormat = SimpleDateFormat("hh:mm:ss")

    val brand = Build.BRAND; val model = Build.MODEL ; val product = Build.PRODUCT
    val serial = Build.SERIAL
    val deviceID = "$brand - $model - $serial"

    var deviceName: String = ""
    var userName: String = ""
    var normalInterval: String = ""
    var fastInterval: String = ""
    var configGpsPriority: String = ""

/*
    fun writeStateDebug(text: String) {
        var calendar = Calendar.getInstance()

        var now = calendar.getTime()
        var nowFormated = parseFormat.format(now)
        txtState.append("$nowFormated: $text \n")
    }
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

    if (checkAndRequestPermissions()) {
        // carry on the normal flow, as the case of  permissions  granted.
        Log.d(TAG,"sms permission ok!")
    }
    /*
    // If Get In Into Other Activity
    var Intent1: Intent
    Intent1= getIntent()
    */
    if (intent.extras != null) {
        for (key in intent.extras!!.keySet()) {
            //val value = intent.extras!!.getString(key)
            //Log.d(TAG, "Key: $key Value: $value")
        }
    }

    val tmp1: String? = intent.getStringExtra("notification_obj")
    txtDebug.setText("")
    txtDebug.append("$tmp1 \n")

    if (!tmp1.isNullOrEmpty()) {
        Toast.makeText(this, tmp1, Toast.LENGTH_LONG).show()
    }

/*
    val queue = Volley.newRequestQueue(this)
    val url = "http://gps.emnix.com/post_location/"
    val params = JSONObject()
    params.put("name","test name")
    params.put("field1","value for filed1")
Log.d(TAG,"rootObj: $params")


    val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, params ,
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
*/

    /*
val sampleJson = "{k1=v1, k2=v2, k3=v3}"


    var jsonArray = JSONArray(sampleJson)
    for (jsonIndex in 0..(jsonArray.length() - 1)) {
        Log.d("JSON", jsonArray.getJSONObject(jsonIndex).getString("k1"))
    }
*/



    //val fcmToken = FirebaseInstanceId.getInstance().getToken(getString(R.string.SENDER_ID), "FCM")
    //Log.i(TAG, fcmToken)
    //txtDebug.append("fcmToken: $fcmToken \n");
    //Log.d("FCMToken", "token "+ FirebaseInstanceId.getInstance().instanceId.);


/*
FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( this,  OnSuccessListener<InstanceIdResult>() {
     @Override
     fun onSuccess(instanceIdResult: InstanceIdResult) {
           val newToken = instanceIdResult.getToken();
           Log.d("newToken", newToken);

     }
 });
*/
        txtDebug.append("deviceID: $deviceID \n");


        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
            userName        = prefs!!.getString("configUsername", "")
            deviceName      = prefs!!.getString("configDeviceName", "")
            normalInterval  = prefs!!.getString("configNormalInterval", "")
            fastInterval    = prefs!!.getString("configFastInterval", "")
            configGpsPriority = prefs!!.getString("configGpsPriority", "PRIORITY_HIGH_ACCURACY")

    val fcm_token    = prefs!!.getString("fcm_token", "")
    val fb    = this.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "")

    txtToken.text = fcm_token


    txtDebug.append("fcm_token: $fcm_token \n");
    txtDebug.append("fb: $fb \n");

    txtDebug.append("username: $userName \n");
        txtDebug.append("deviceName: $deviceName \n");
        txtDebug.append("NormalInterval: $normalInterval / $fastInterval\n");
        txtDebug.append("configGpsPriority: $configGpsPriority \n");



        btnSettings.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View): Unit {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                intent.putExtra("SHOW_WELCOME", true)
                startActivity(intent)
            }
        })

        progress.setVisibility(View.GONE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                progress.setVisibility(View.VISIBLE);

                Handler().postDelayed({
                        progress.setVisibility(View.GONE);
                }, 1000);

                lastLocation = p0.lastLocation

                lastLocation.latitude = BigDecimal(lastLocation.latitude).setScale(3, RoundingMode.DOWN).toDouble();
                lastLocation.longitude = BigDecimal(lastLocation.longitude).setScale(3, RoundingMode.DOWN).toDouble();

                lastLocation.latitude = String.format("%.3f", lastLocation.latitude).toDouble()
                lastLocation.longitude = String.format("%.3f", lastLocation.longitude).toDouble()

                latitude_text.text = lastLocation.latitude.toString()
                longitude_text.text = lastLocation.longitude.toString()

                lastLocationLat = lastLocation.latitude
                lastLocationLon = lastLocation.longitude


                var calendar = Calendar.getInstance()
                var now = calendar.getTime()
                var nowFormated = parseFormat.format(now)

                txtLastLocationUpdate.text = nowFormated

                sendLocationToURL()

            }
        }

        createLocationRequest()

    }


  fun checkAndRequestPermissions(): Boolean {
        val permissionSendMessage = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        val receiveSMS = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        val readSMS = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);

        var listPermissionsNeeded = arrayListOf<String>()

        if (receiveSMS != PackageManager.PERMISSION_GRANTED) {
              Log.d(TAG, "permission - receiveSMS not granted")
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        if (readSMS != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permission - readSMS not granted")
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);

        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permission - sendSMS not granted")
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }

        if (!listPermissionsNeeded.isNullOrEmpty()) {
              ActivityCompat.requestPermissions(this,
                  arrayOf("Manifest.permission.SEND_SMS","Manifest.permission.RECEIVE_SMS","Manifest.permission.READ_SMS"),
                  101);
              return false;
         }
         return true;
     }


    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_PHONE_STATE)) {
            } else { ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 2) } }

        try{
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val IMEI = tm.getImei()
            if (IMEI != null) {
                Toast.makeText(
                    this, "IMEI number: " + IMEI,
                    Toast.LENGTH_LONG
                ).show()
                this.getSharedPreferences("_", MODE_PRIVATE).edit().putString("imei", IMEI).apply();

            }
        }catch (ex:Exception){
            Log.e(TAG,"could not get imei: $ex.message")
        }


        if (!checkPermissions()) {
            requestPermissions()
        } else {
            //getLastLocation()
        }

    }



    /**
     *
     * Shows a [Snackbar].
     *
     * @param snackStrId The id for the string resource for the Snackbar text.
     * @param actionStrId The text of the action item.
     * @param listener The listener associated with the Snackbar action.
     */
    private fun showSnackbar(
        snackStrId: Int,
        actionStrId: Int = 0,
        listener: View.OnClickListener? = null
    ) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), getString(snackStrId),
            LENGTH_INDEFINITE)
        if (actionStrId != 0 && listener != null) {
            snackbar.setAction(getString(actionStrId), listener)
        }
        snackbar.show()
    }


    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions() =
        ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {
            // Provide an additional rationale to the user. This would happen if the user denied the
            // request previously, but didn't check the "Don't ask again" checkbox.
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale, android.R.string.ok, View.OnClickListener {
                // Request permission
                startLocationPermissionRequest()
            })

        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                grantResults.isEmpty() -> Log.i(TAG, "User interaction was cancelled.")

                // Permission granted.
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) -> startLocationUpdates()

                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                else -> {
                    showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", "com.emnix.gpstracking01", null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        })
                }
            }
        }
    }

    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        //2
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        if (normalInterval.isBlank()) {
            locationRequest.interval = 60000
        } else {
            locationRequest.interval = normalInterval.toLong()
        }
        if (fastInterval.isBlank()) {
            locationRequest.fastestInterval = 30000
        } else {
            locationRequest.fastestInterval = fastInterval.toLong()
        }

        if (configGpsPriority == "PRIORITY_HIGH_ACCURACY") locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY else LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        //locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        //locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MainActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    // 2
    override fun onPause() {
        super.onPause()
        //fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    // 3
    public override fun onResume() {
        super.onResume()
        //if (!locationUpdateState) {
            startLocationUpdates()
        //}

        if (intent.extras != null) {
            for (key in intent.extras!!.keySet()) {
                //val value = intent.extras!!.getString(key)
                //Log.d(TAG, "Resume Key: $key Value: $value")
            }
        }


        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        userName        = prefs!!.getString("configUsername", "")
        deviceName      = prefs!!.getString("configDeviceName", "")
        normalInterval  = prefs!!.getString("configNormalInterval", "")
        fastInterval    = prefs!!.getString("configFastInterval", "")
        configGpsPriority = prefs!!.getString("configGpsPriority", "PRIORITY_HIGH_ACCURACY")

        txtDebug.append("R-user: $userName \n");
        txtDebug.append("R-device: $deviceName \n");
        txtDebug.append("R-Interval: $normalInterval / $fastInterval\n");
        txtDebug.append("R-configGpsPriority: $configGpsPriority \n");

        val fcm_token    = prefs!!.getString("fcm_token", "")
        val fb    = this.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "")

        txtToken.text = fcm_token



    }

    public override fun onStop() {
        super.onStop()

    }

    fun btnClick(View: View) {
        val phoneNo:String = "+381649180279"
        val smsManager = SmsManager.getDefault() as SmsManager
        smsManager.sendTextMessage(phoneNo, null, "sms message", null, null)

        sendLocationToURL()
    }


    fun sendLocationToURL() {
        //val current = LocalDateTime.now()
        var calendar2 = Calendar.getInstance()
        val current_time = calendar2.getTime()
        var current_time_formated = parseFormat.format(current_time)
        val fb    = this.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "")


        val queue = Volley.newRequestQueue(this)
if (userName != "") {
    if (abs(lastLocationLat) - abs(lastSendedLocationLat) > 0.001 || abs(lastLocationLon) - abs(lastSendedLocationLon) > 0.001){
    //if ((lastLocationLat != lastSendedLocationLat) || (lastLocationLon != lastSendedLocationLon)) {
        val query_string = "deviceID=$deviceID&user=$userName&lat=$lastLocationLat&lon=$lastLocationLon&ni=$normalInterval&fi=$fastInterval&deviceName=$deviceName&priority=$configGpsPriority&token=$fb"
        val query = Uri.encode(query_string)

        val url = "http://gps.emnix.com/post/$query"

        txtDebug?.append("Saljem ... $current_time_formated | Lat: $lastLocationLat / Lon: $lastLocationLon \n")

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                val myresponse = response.toString()

                if (myresponse.contains("ok:")) {
                    lastSendedLocationLat = lastLocationLat
                    lastSendedLocationLon = lastLocationLon
                    txtDebug?.append("uspesno sacuvano! $myresponse \n")

                    txtLastLat.text = lastLocationLat.toString()
                    txtLastLon.text = lastLocationLon.toString()

                    var calendar = Calendar.getInstance()
                    var now = calendar.getTime()
                    var nowFormated = parseFormat.format(now)

                    txtLastSendTime.text = nowFormated.toString()

                } else {
                    txtDebug?.append("nije sacuvano, greska kod upisa. \n $myresponse \n")

                }

            },
            Response.ErrorListener { txtDebug?.append("** ERROR** internet ne radi! \n") })

        queue.add(stringRequest)

    } else {
        txtDebug?.append("Nema promena! $current_time_formated | $lastLocationLat $lastLocationLon \n")
        //txtDebug?.append ("Lat: $lastLocationLat / Lon: $lastLocationLon  \nLat: $lastSendedLocationLat / Lon: $lastSendedLocationLon \n")
    }
}// if username !=''
    }// fun sendLocationToURL


}
