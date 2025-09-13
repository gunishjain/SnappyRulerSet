package com.gunishjain.myapplication.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for providing haptic feedback
 */
object HapticFeedbackUtil {
    
    /**
     * Perform a gentle vibration when snapping occurs
     */
    fun performSnapHapticFeedback(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
    
    /**
     * Perform a gentle vibration using the Vibrator service
     */
    fun performSnapVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 (API 26) and above
            // Create a gentle, short vibration effect
            val vibrationEffect = VibrationEffect.createOneShot(
                10, // 10 milliseconds duration
                VibrationEffect.DEFAULT_AMPLITUDE / 2 // Half the default amplitude for gentleness
            )
            vibrator.vibrate(vibrationEffect)
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            vibrator.vibrate(10) // 10 milliseconds duration
        }
    }
}