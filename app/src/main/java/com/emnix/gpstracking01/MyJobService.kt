package com.emnix.gpstracking01


import android.util.Log

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService


class MyJobService : JobService() {

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Performing long running task in scheduled job")

        // TODO(developer): add long running task here.
        return false //return false if job done otherwise return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "onStopJob")

        return false
        //Should this job be retried?"
    }


    companion object {

        private const val TAG = "MyJobService"
    }
}