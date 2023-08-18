package com.example.testpart1.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.room.Room
import com.example.testpart1.databinding.ExerciseActivityBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExerciseHome : ComponentActivity() {

    private lateinit var exerciseClient: ExerciseClient
    private lateinit var binding: ExerciseActivityBinding

    //for rooom db:
    private lateinit var cachedHeartRateDao: CachedHeartRateDao
    private lateinit var appRoomDb: AppRoomDb

    //list for caching
    private val heartRateDataList = mutableListOf<CachedHeartRate>()

    //making sure that the value of heart rate is always the latest
    private var latestHeartRate: Double = 0.0
    private val heartRateMutex = Mutex()

    private var exerciseUpdateCallback: ExerciseUpdateCallback? = null

    // Requested permissions
    private val permissions = arrayOf(
        Manifest.permission.BODY_SENSORS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ExerciseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Room database and ExerciseClient
        appRoomDb = Room.databaseBuilder(applicationContext, AppRoomDb::class.java, "heart_rate_db")
            .build()
        cachedHeartRateDao = appRoomDb.cachedHeartRateDao()

        exerciseClient = HealthServices.getClient(this).exerciseClient

        // Set up UI buttons and their click listeners
        binding.startexercise.setOnClickListener {
            requestPermissions.launch(permissions)
        }
        binding.stopexercise.setOnClickListener { stopExercise() }
        binding.close.setOnClickListener {
            closeView()
        }

        //caching data and storing data after set time delays
        cacheDataAfterSetTimeDuration()
        storeDataAfterSetTimeDuration()
    }

    //caching data in a list every 3 seconds
    private fun cacheDataAfterSetTimeDuration() {
        lifecycleScope.launch {
            while (true) {
                val latestHeartRate = getCurrentHeartRate()
                if (latestHeartRate != null) {
                    val heartRateData = CachedHeartRate(
                        heartRate = latestHeartRate,
                        timestamp = System.currentTimeMillis()
                    )
                    heartRateDataList.add(heartRateData)
                }
                delay(3000) // Collect data every 3 seconds
            }
        }

    }

    // storing cached data in the database every 60 seconds
    private fun storeDataAfterSetTimeDuration() {
        //only write if there is an existing connection with remote db ideally
        if(isConnected()) {
            // Start storing cached data in the database every 60 seconds
            lifecycleScope.launch {
                insertCachedDataIntoDb(heartRateDataList) // Insert cached data into the database
                delay(60000) // Store data every 60 seconds
            }
            heartRateDataList.clear()
        }
    }

    // insert cached data into the database
    private suspend fun insertCachedDataIntoDb(cachedDataList: List<CachedHeartRate>) {
        // Insert cached data into the database here using the cachedHeartRateDao
        cachedHeartRateDao.insertHeartRateList(cachedDataList)
    }

    //get latest heart rate value
    private suspend fun getCurrentHeartRate(): Double? {
        return heartRateMutex.withLock {
            latestHeartRate
        }
    }

    //update the latest heart rate value with Mutex since heartRate is getting sent and updated
    //using multiple coroutines
    private suspend fun updateLatestHeartRate(newHeartRate: Double) {
        heartRateMutex.withLock {
            latestHeartRate = newHeartRate
        }
    }

    //lose the current view and return to the main activity
    private fun closeView() {
        stopExercise()
        startActivity(Intent(this, MainActivity::class.java))
    }

    //handle permission request result
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.all { it.value }
        if (allPermissionsGranted) {
            startExercise()
        } else {
            //on permissions denied take back to the main activity
            //let the user know through a toast
            Toast.makeText(
                this,
                "Permission denied, please update permissions to continue",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this@ExerciseHome, MainActivity::class.java))
        }
    }


    //single exercise session begins
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

    //end the current session
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

    //register for exercise update callbacks when the activity starts
    override fun onStart() {
        super.onStart()
        registerExerciseUpdates()
    }

    //clearng exercise update callback when the activity stops
    override fun onStop() {
        super.onStop()
        exerciseUpdateCallback?.let {
            exerciseClient.clearUpdateCallbackAsync(it)
        }
    }

    //register exercise update callbacks
    private fun registerExerciseUpdates() {
        exerciseUpdateCallback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val latestMetrics = update.latestMetrics
                val heartRateUpdates = latestMetrics.getData(DataType.HEART_RATE_BPM)
                //Log.e("*******HEART RATE*******", locationUpdates.last().value.toString())
                if (heartRateUpdates.isNotEmpty()) {
                    val heartRate = heartRateUpdates.last().value
                    lifecycleScope.launch {
                        updateLatestHeartRate(heartRate)
                    }
                    updateLocationTextView(heartRate)
                }
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
            }

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
            }


            override fun onRegistered() {

            }

            override fun onRegistrationFailed(throwable: Throwable) {
            }
        }
        exerciseClient.setUpdateCallback(exerciseUpdateCallback as ExerciseUpdateCallback)
    }

    //update heartrate for user's visible text view
    private fun updateLocationTextView(heartRate: Double) {
        val heartRateText = "HeartRate: $heartRate BPM"
        binding.heartObserve.text = heartRateText
    }

    // Check if the device is online
    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}
