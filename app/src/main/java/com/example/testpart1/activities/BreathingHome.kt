package com.example.testpart1.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.testpart1.R

class BreathingHome : ComponentActivity() {
    private var updatedCycleCount = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.breathing_home)

        val startButton = findViewById<Button>(R.id.start)
        startButton.setOnClickListener {
            val intent = Intent(this, BreathingActivity::class.java)
            intent.putExtra("userCycleCount", updatedCycleCount)
            startActivity(intent)
        }
    }

    fun increaseCycleCount(view: View) {
        updatedCycleCount++;
        displayUpdatedCount(updatedCycleCount)
    }

    fun decreaseCycleCount(view: View) {
        updatedCycleCount = if(updatedCycleCount > 1) updatedCycleCount - 1 else updatedCycleCount
        displayUpdatedCount(updatedCycleCount)
    }

    private fun displayUpdatedCount(number: Int) {
        val numberOfCycles = findViewById<TextView>(R.id.cycles)
        numberOfCycles.text = number.toString()
    }
}


