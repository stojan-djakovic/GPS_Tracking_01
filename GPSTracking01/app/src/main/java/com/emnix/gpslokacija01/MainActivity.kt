package com.emnix.gpslokacija01

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import kotlinx.android.synthetic.main.content_main.*

import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.Response
import android.os.Build;

import org.json.JSONArray
import org.json.JSONObject
/*import java.time.LocalDateTime */
import java.util.Calendar

/*import com.google.android.gms.location.sample.basiclocationsample.BuildConfig.APPLICATION_ID*/

class MainActivity : AppCompatActivity() {
    var lastLocationLat: Double = 0.0
    var lastLocationLon: Double = 0.0

    var lastSendedLocationLat: Double = 0.0
    var lastSendedLocationLon: Double = 0.0


    private lateinit var lastLocation: Location

    // 1
    private lateinit var locationCallback: LocationCallback
    // 2
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        // 3
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    private val TAG = "MainActivity"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView


    private lateinit var txtDevice: TextView
    private lateinit var txtUser: TextView

    private var txtDebug: TextView? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Instead of findView(R.id.toolbar) as Toolbar
        //mToolbar = findViewById(R.id.toolbar) as Toolbar

        latitudeText = findViewById(R.id.latitude_text)
        longitudeText = findViewById(R.id.longitude_text)

        txtDevice = findViewById(R.id.txtDevice)
        txtUser   = findViewById(R.id.txtUser)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                latitudeText.text = lastLocation.latitude.toString()
                longitudeText.text = lastLocation.longitude.toString()

                lastLocationLat = lastLocation.latitude
                lastLocationLon = lastLocation.longitude

                sendLocationToURL()

            }
        }

        createLocationRequest()

        txtDebug = findViewById(R.id.txtDebug)
        //getUsers()
        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val model = Build.MODEL
        val product = Build.PRODUCT
        //val serial = Build.getSerial()
        val serial = Build.SERIAL

        //val deviceName = "$manufacturer | $brand | $model | $product "
        val deviceName = "$brand - $serial "
        txtDevice.text = deviceName
        txtUser.text = "stojan.djakovic@gmail.com"


    }

    override fun onStart() {
        super.onStart()

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            getLastLocation()
        }
    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     *
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    latitudeText.text = resources
                        .getString(R.string.latitude_label, task.result.latitude)
                    longitudeText.text = resources
                        .getString(R.string.longitude_label, task.result.longitude)


                    Log.i(TAG, " Location: " + task); //may return **null** because, I can't guarantee location has been changed immmediately

                    lastLocationLat = task.result.latitude
                    lastLocationLon = task.result.longitude

                } else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    showSnackbar(R.string.no_location_detected)
                }
            }
    }


    fun calculate(View: View) {

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            // 3
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                Log.i("STOLE", " currentLatLng: " + currentLatLng);

                latitudeText.text = location.latitude.toString()
                longitudeText.text = location.longitude.toString()
            }

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
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) -> getLastLocation()

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
                                data = Uri.fromParts("package", "com.emnix.gpslokacija01", null)
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
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 3
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }


    fun sendDataToURL(View: View) {

        val queue = Volley.newRequestQueue(this)
        val url = "https://pastebin.com/XDYWMUta"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                // Display the first 500 characters of the response string.
                txtDebug?.text = "Response is: ${response.substring(0, 500)}"
            },
            Response.ErrorListener { txtDebug?.text = "That didn't work!" })

// Add the request to the RequestQueue.
        queue.add(stringRequest)

        txtDebug?.text = "testt"


    }


    // function for network call
    fun getUsers(View: View) {
        val queue = Volley.newRequestQueue(this)
        val url: String = "https://api.github.com/search/users?q=eyehunt"

        // Instantiate the RequestQueue.
        // Request a string response from the provided URL.
        val stringReq = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->

                var strResp = response.toString()
                val jsonObj: JSONObject = JSONObject(strResp)
                val jsonArray: JSONArray = jsonObj.getJSONArray("items")
                var str_user: String = ""
                for (i in 0 until jsonArray.length()) {
                    var jsonInner: JSONObject = jsonArray.getJSONObject(i)
                    str_user = str_user + "\n" + jsonInner.get("login")
                }
                txtDebug?.text = "response : $str_user "
            },
            Response.ErrorListener { txtDebug?.text = "That didn't work!" })
        queue.add(stringReq)

    }

    fun btnClick(View: View) {
        sendLocationToURL()
    }


    fun sendLocationToURL() {
        //val current = LocalDateTime.now()
        val c = Calendar.getInstance()
        val current = c.getTime()

        val queue = Volley.newRequestQueue(this)
        val device = txtDevice.text
        val user   = txtUser.text

        if ((lastLocationLat != lastSendedLocationLat) || (lastLocationLon != lastSendedLocationLon)) {
            val query = Uri.encode("device=$device&user=$user&lat=$lastLocationLat&lon=$lastLocationLon")

            val url = "http://gps.emnix.com/post/$query"

            txtDebug?.text = "Saljem na server ... $current \n"
            txtDebug?.append ("Lat: $lastLocationLat / Last: $lastSendedLocationLat \nLon: $lastLocationLon / Last: $lastSendedLocationLon \n")
            txtDebug?.append ("************************* \n")

            //txtDebug?.append("\n * $url * \n")

            // Request a string response from the provided URL.
            val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    val myresponse = response.toString()
                    txtDebug?.append(myresponse)
                    txtDebug?.append("\n")

                    if (myresponse.contains("ok:")) {
                        lastSendedLocationLat = lastLocationLat
                        lastSendedLocationLon = lastLocationLon
                        txtDebug?.append("uspesno sacuvano!")
                    } else {
                        txtDebug?.append("nije sacuvano, greska kod upisa")

                    }


                },
                Response.ErrorListener { txtDebug?.append("That didn't work!") })

            queue.add(stringRequest)

        } else {
            txtDebug?.text = "Nema promena! $current \n"
            txtDebug?.append ("Lat: $lastLocationLat / Last: $lastSendedLocationLat \nLon: $lastLocationLon / Last: $lastSendedLocationLon \n")

        }


    }// fun sendLocationToURL


}
