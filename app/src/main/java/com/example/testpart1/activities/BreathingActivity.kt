package com.example.testpart1.activities

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import com.example.testpart1.R
import com.example.testpart1.databinding.BreathingActivityBinding
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class BreathingActivity : ComponentActivity() {

    private lateinit var binding: BreathingActivityBinding
    private lateinit var valueAnimator: ValueAnimator
    private var retrievedCycleCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //user input received after being passed through intent
        //user input : number of cycles user expects
        retrievedCycleCount = intent.getIntExtra("userCycleCount", 1)

        binding = BreathingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.stop.setOnClickListener {
            startActivity(Intent(this, BreathingHome::class.java))
        }

        breathingViewPrep()
    }

    //starts initial countdown to get the user ready for breathing cycles
    private fun breathingViewPrep() {
        startCountdownAnimation(3000, binding.countdown, 3, 0) {
            binding.countdown.visibility = View.INVISIBLE
            startBreathingAnimation()
            totalBreathingCycle()
        }
    }

    /*
    Each stage has its own animation count, text (inhale, exhale, hold) and
    textview for displaying the animation
    Each stage has a set duration for background animation
    */
    private fun totalBreathingCycle() {
        binding.breathingAction.visibility = View.VISIBLE
        binding.breathingCircle.visibility = View.VISIBLE
        binding.inhaleCount.visibility = View.VISIBLE

        val stages = listOf(
            Stage(getString(R.string.inhale), "4"),
            Stage(getString(R.string.hold), "4"),
            Stage(getString(R.string.exhale), "4"),
            Stage(getString(R.string.hold), "4")
        )

        val countdownTextViews = arrayOf(
            binding.inhaleCount, // For inhale
            binding.holdCount,
            binding.exhaleCount,
            binding.hold2Count
        )

        val stageDuration = 4000L

        // Initialize index for current stage
        var stageIndex = 0

        fun animateStage() {
            if (stageIndex >= stages.size) {
                // Animation completed for one cycle
                if (retrievedCycleCount > 1) {
                    retrievedCycleCount--
                    stageIndex = 0 // Reset stage index for the next cycle
                    animateStage() // Start the animation sequence for the next cycle
                } else {
                    onCompleteCycles()
                }
                return
            }


            val stage = stages[stageIndex]

            // Update text and visibility for current stage
            binding.breathingAction.text = stage.text
            countdownTextViews[stageIndex].text = stage.count
            countdownTextViews[stageIndex].visibility = View.VISIBLE

            // Start countdown animation for current stage
            startCountdownAnimation(stageDuration, countdownTextViews[stageIndex], 4, 0) {
                countdownTextViews[stageIndex].visibility = View.INVISIBLE
                stageIndex++ // Move to the next stage
                animateStage() // recursive call for each stage animation
            }
        }

        // Start the animation sequence
        animateStage()
    }

    data class Stage(val text: String, val count: String)

    //final success message and automatic redirection to main screen after
    //successfull completion
    private fun onCompleteCycles() {
        binding.breathingCircle.visibility = View.INVISIBLE
        binding.breathingAction.text = getString(R.string.relax)
        binding.stop.visibility = View.INVISIBLE
        val party = Party(
            speed = 0f,
            maxSpeed = 10f,
            damping = 0.9f,
            spread = 600,
            colors = listOf(0xFDD835, 0xB71C1C, 0xEC407A, 0xFF9E80),
            emitter = Emitter(duration = 1000, TimeUnit.MILLISECONDS).max(600),
            position = Position.Relative(0.5, 1.0)
        )

        binding.konfettiView.start(party)

        //redirection after 5 seconds to main activity
        val rootLayout = findViewById<View>(R.id.rootLayout)
        rootLayout.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
        }, 5000)

    }

    //separate function of using valueanimator for countdown animation
    private fun startCountdownAnimation(
        duration: Long,
        targetTextView: TextView,
        startValue: Int,
        endValue: Int,
        onFinish: () -> Unit
    ) {
        valueAnimator = ValueAnimator.ofInt(startValue, endValue)
        valueAnimator.duration = duration
        valueAnimator.addUpdateListener { animation ->
            val currentValue = animation.animatedValue as Int
            targetTextView.text = currentValue.toString()
        }

        valueAnimator.start()

        valueAnimator.doOnEnd {
            onFinish.invoke()
        }
    }

    //backgrounf animation
    private fun startBreathingAnimation() {
        val animator = ValueAnimator.ofFloat(0.6f, 0.8f)
        animator.duration = 4000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            binding.breathingCircle.scaleX = scale
            binding.breathingCircle.scaleY = scale
        }
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the ValueAnimator to prevent memory leaks
        //checking if it has been initialized to avoid any crashes
        //if the activity is destroyed before 'valueAnimator' gets initialized
        if (::valueAnimator.isInitialized  && valueAnimator.isRunning) {
            valueAnimator.cancel()
        }
    }
}
