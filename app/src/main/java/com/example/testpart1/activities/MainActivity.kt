package com.example.testpart1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import com.example.testpart1.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val mindfulExerciseButton = findViewById<ImageButton>(R.id.brain)
        mindfulExerciseButton.setOnClickListener{
            startActivity(Intent(this, BreathingHome::class.java))
        }

        val walkingExerciseButton = findViewById<ImageButton>(R.id.body)
        walkingExerciseButton.setOnClickListener{
            startActivity(Intent(this, ExerciseHome::class.java))
        }
    }

}


