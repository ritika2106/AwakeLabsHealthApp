package com.example.testpart1.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.lifecycle.lifecycleScope
import android.Manifest
import android.view.View
import android.widget.Button
import androidx.health.services.client.awaitWithException
import com.example.testpart1.R
import kotlinx.coroutines.launch

class Exercise : AppCompatActivity() {

    private lateinit var healthClient: HealthServicesClient
    private lateinit var exerciseClient: ExerciseClient
    private lateinit var heartRateTextView: TextView
    private lateinit var startExerciseButton: Button
    private lateinit var stopExerciseButton: Button

    private val permissions = arrayOf(
        Manifest.permission.BODY_SENSORS
    )
    private val requestCodePermissions = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exercise_activity)

        healthClient = HealthServices.getClient(this)
        exerciseClient = healthClient.exerciseClient

        heartRateTextView = findViewById(R.id.heartObserve)
        startExerciseButton = findViewById(R.id.startexercise)
        stopExerciseButton = findViewById(R.id.stopexercise)

        startExerciseButton.setOnClickListener { requestPermissionsAndStartExercise() }
       stopExerciseButton.setOnClickListener { stopExercise() }
    }

    private fun requestPermissionsAndStartExercise() {
        if (checkPermissions()) {
            startExercise()
        } else {
            ActivityCompat.requestPermissions(this, permissions, requestCodePermissions)
        }
    }

    private fun checkPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startExercise()
            } else {
                // Handle permission denied
            }
        }
    }

    private fun startExercise() {
        lifecycleScope.launch {
            val exerciseConfig = ExerciseConfig(
                exerciseType = ExerciseType.WALKING,
                dataTypes = setOf(DataType.HEART_RATE_BPM),
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = true
            )
            try {
                exerciseClient.startExerciseAsync(exerciseConfig).awaitWithException()
                runOnUiThread {
                    startExerciseButton.visibility = View.GONE
                    stopExerciseButton.visibility = View.VISIBLE
                }
            }catch (_: Exception){

            }
        }

    }

    private fun stopExercise() {
        lifecycleScope.launch {
            try{
                exerciseClient.endExerciseAsync().awaitWithException()
                runOnUiThread{
                    startExerciseButton.visibility = View.VISIBLE
                    stopExerciseButton.visibility = View.GONE
                }
            } catch (_: Exception){

            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerExerciseUpdates()
    }

    override fun onStop() {
        super.onStop()
        // exerciseClient.clearUpdateCallbackAsync(ExerciseUpdateCallback)
    }

    private fun registerExerciseUpdates() {
        val exerciseUpdateCallback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val latestMetrics = update.latestMetrics
               // val locationUpdates = latestMetrics.getData(DataType.HEART_RATE_BPM)
               val paceUpdates = latestMetrics.getData(DataType.PACE)
                //Log.e("*******PACE*********", paceUpdates.last().value.toString())
                //Log.e("*******HEART RATE*******", locationUpdates.last().value.toString())
                if (paceUpdates.isNotEmpty()) { //&& paceUpdates.isNotEmpty()) {
                    val location = paceUpdates.last().value
                   // val pace = paceUpdates.last().value
                    updateLocationTextView(location)
                }
            }

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                // Handle data availability changes if needed
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
                // Handle lap summary if needed
            }

            override fun onRegistered() {

            }

            override fun onRegistrationFailed(throwable: Throwable) {
            }
        }
        exerciseClient.setUpdateCallback(exerciseUpdateCallback)
    }

    private fun updateLocationTextView(heartRate: Double) {


        val locationText = "Pace: $heartRate"
//        val paceText = "Pace: $pace"
        heartRateTextView.text = locationText
//        paceTextView.text = paceText
    }
}