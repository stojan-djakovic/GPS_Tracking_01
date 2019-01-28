package com.emnix.gpstracking01

import android.app.NotificationChannel
import android.app.NotificationManager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver

import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*
import org.json.JSONObject




class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage?.from}")
        val messageId = remoteMessage?.messageId
        Log.d(TAG, "messageId: $messageId")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            if (!remoteMessage.data.isNullOrEmpty()){
                Log.d(TAG, "Message data payload: " + remoteMessage.data)
                val data_str = remoteMessage.data.toString()
                Log.d(TAG, "Message data payload string: " + data_str)
                val json = JSONObject(data_str.toString())
                Log.d(TAG, "json: " + json)
                if (json.has("action")) {
                    val action: String? = json.getString("action")
                    Log.d(TAG, "action: " + action)

                    if (/* Check if data needs to be processed by long running job */ false) {
                        // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                        Log.d(TAG, "scheduleJob ")
                        scheduleJob()
                    } else {
                        // Handle message within 10 seconds
                        Log.d(TAG, "handleNow ")
                        handleNow(messageId, action)
                    }
                }
            } // !remoteMessage.data.isNullOrEmpty()
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            val mybody = it.body.toString()
            val mytitle = it.title.toString()

            Log.d(TAG, "My notification: $mytitle -> $mybody")
            /*
            val intent = Intent(this,MainActivity::class.java);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("notification_obj", "notification info")
            val uniqueInt = (System.currentTimeMillis() and 0xff).toInt()
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, uniqueInt, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            */
            /*
            var b = Bundle()
            b.putBoolean("isActive", true)
            intent.putExtras(b)

                 startActivity(Intent(this, Page2::class.java).apply {
                    putExtra("extra_1", value1)
                    putExtra("extra_2", value2)
                    putExtra("extra_3", value3)
                })
            */
            //if (!body.isNullOrEmpty()) sendNotification(body)
            sendNotification(mybody,mytitle)

        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        val PREFS_FILENAME = "com.emnix.gpstracking01.prefs"
        var prefs: SharedPreferences? = null

        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val editor = prefs!!.edit()
        editor.putString("fcm_token", token)
        editor.apply()


        this.getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", token).apply();

        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private fun scheduleJob() {
        // [START dispatch_job]
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val myJob = dispatcher.newJobBuilder()
            .setService(MyJobService::class.java)
            .setTag("my-job-tag")
            .build()
        dispatcher.schedule(myJob)
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow(messageId: String?, action: String?) {
        Log.d(TAG, "Short lived task . " + action)
        if (action == "getlocation") {
            lateinit var fusedLocationClient: FusedLocationProviderClient
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            fusedLocationClient.lastLocation.addOnSuccessListener {
                Log.d(TAG, "onSuccessListener")
                Log.d(TAG, "$it")
                val lat = it.latitude
                val lon = it.longitude
                Log.d(TAG, "lat: $lat | lon: $lon")

                val queue = Volley.newRequestQueue(this)
                val url = "http://gps.emnix.com/post_location/"
                val params = JSONObject()
                params.put("lat",lat)
                params.put("lon",lon)
                params.put("messageId",messageId)
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

            }
        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationToServer -> token: $token \n")
        val brand = Build.BRAND; val model = Build.MODEL ; val product = Build.PRODUCT
        val serial = Build.SERIAL
        val deviceID = "$brand - $model - $serial"


        val queue = Volley.newRequestQueue(this)
        val query_string = "deviceID=$deviceID&token=$token"
        val query = Uri.encode(query_string)

        val url = "http://gps.emnix.com/post_token/$query"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val myresponse = response.toString()
                Log.d(TAG,myresponse)
            },
            Response.ErrorListener { Log.d(TAG, "** ERROR** internet ne radi! \n") })

        queue.add(stringRequest)

    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String, messageTitle: String) {
        Log.d(TAG, "sendNotification1 \n")

        val notificationId = Random().nextInt(60000)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("notification_obj", "notification: " + messageBody)

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }


        notificationManager.notify(notificationId  /* ID of notification */, notificationBuilder.build())

        Log.d(TAG, "sendNotification2 \n")

    }

    companion object {

        private const val TAG = "MyFirebaseMsgService"
    }
}
