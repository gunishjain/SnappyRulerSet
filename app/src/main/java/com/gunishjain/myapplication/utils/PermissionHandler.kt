package com.gunishjain.myapplication.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Utility for handling storage permissions
 */
object PermissionHandler {
    
    /**
     * Check if storage permission is granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we don't need storage permission for MediaStore
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get required permissions for the current Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permission for MediaStore
            emptyArray()
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

/**
 * Composable for handling permission requests
 */
@Composable
fun rememberPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): (() -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(PermissionHandler.hasStoragePermission(context)) 
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermission = allGranted
        
        if (allGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    return {
        if (hasPermission) {
            onPermissionGranted()
        } else {
            val permissions = PermissionHandler.getRequiredPermissions()
            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions)
            } else {
                // No permissions needed for Android 13+
                onPermissionGranted()
            }
        }
    }
}
