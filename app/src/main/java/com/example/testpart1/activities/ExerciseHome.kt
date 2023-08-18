package com.example.testpart1.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.awaitWithException
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.lifecycle.lifecycleScope
import com.example.testpart1.databinding.ExerciseActivityBinding
import kotlinx.coroutines.launch

class ExerciseHome : ComponentActivity() {

    private lateinit var exerciseClient: ExerciseClient
    private lateinit var binding: ExerciseActivityBinding
    private var exerciseUpdateCallback: ExerciseUpdateCallback? = null

    private val permissions = arrayOf(
        Manifest.permission.BODY_SENSORS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExerciseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseClient = HealthServices.getClient(this).exerciseClient

        binding.startexercise.setOnClickListener {
            requestPermissions.launch(permissions)
        }
        binding.stopexercise.setOnClickListener { stopExercise() }
        binding.close.setOnClickListener {
            closeView()
        }
    }

    private fun closeView() {
        stopExercise()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.all { it.value }
        if (allPermissionsGranted) {
            startExercise()
        } else {
            // Handle permission denied
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
                    binding.startexercise.visibility = View.GONE
                    binding.close.visibility = View.VISIBLE
                    binding.heartObserve.visibility = View.VISIBLE
                    binding.stopexercise.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    private fun stopExercise() {
        lifecycleScope.launch {
            try {
                exerciseClient.endExerciseAsync().awaitWithException()
                runOnUiThread {
                    binding.startexercise.visibility = View.VISIBLE
                    binding.stopexercise.visibility = View.GONE
                }
                val rootLayout = binding.rootLayout
                rootLayout.postDelayed({
                    startActivity(Intent(this@ExerciseHome, MainActivity::class.java))
                }, 5000)

            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerExerciseUpdates()
    }

    override fun onStop() {
        super.onStop()
        exerciseUpdateCallback?.let {
            exerciseClient.clearUpdateCallbackAsync(it)
        }
    }

    private fun registerExerciseUpdates() {
        exerciseUpdateCallback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val latestMetrics = update.latestMetrics
                val heartRateUpdates = latestMetrics.getData(DataType.HEART_RATE_BPM)
                //Log.e("*******HEART RATE*******", locationUpdates.last().value.toString())
                if (heartRateUpdates.isNotEmpty()) {
                    val heartRate = heartRateUpdates.last().value
                    updateLocationTextView(heartRate)
                }
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
            }

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                // Handle data availability changes if needed
            }



            override fun onRegistered() {

            }

            override fun onRegistrationFailed(throwable: Throwable) {
            }
        }
        exerciseClient.setUpdateCallback(exerciseUpdateCallback as ExerciseUpdateCallback)
    }

    private fun updateLocationTextView(heartRate: Double) {
        val heartRateText = "HeartRate: $heartRate BPM"
        binding.heartObserve.text = heartRateText
    }
}
