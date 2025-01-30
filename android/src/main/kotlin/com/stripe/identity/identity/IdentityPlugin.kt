package com.stripe.identity.identity


import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.stripe.android.identity.IdentityVerificationSheet
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.util.Log
import androidx.activity.ComponentActivity


/**
 * A Flutter plugin for Stripe Identity verification.
 */
class IdentityPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /**
     * The Flutter method channel used to communicate with the Flutter application.
     */
    private lateinit var channel: MethodChannel

    /**
     * The Android application context.
     */
    private lateinit var context: Context

    /**
     * The current activity.
     */
    private var activity: Activity? = null

    /**
     * The IdentityVerificationSheet instance.
     */
    private var identityVerificationSheet: IdentityVerificationSheet? = null

    /**
     * Called when the plugin is attached to the Flutter engine.
     *
     * @param flutterPluginBinding The Flutter plugin binding.
     */
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        // Create a method channel for communication with the Flutter application.
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "stripe_identity_plugin")
        // Set the method call handler for the channel.
        channel.setMethodCallHandler(this)
        // Get the application context.
        context = flutterPluginBinding.applicationContext
    }

    /**
     * Called when a method call is received from the Flutter application.
     *
     * @param call The method call received from Flutter.
     * @param result A closure to return the result of the method call to Flutter.
     */
    override fun onMethodCall(call: MethodCall, result: Result) {
        // Handle the "startVerification" method call.
        when (call.method) {
            "startVerification" -> {
                // Extract the verification session ID, ephemeral key secret, and brand logo URL from the method call arguments.
                val id = call.argument<String>("id")
                val key = call.argument<String>("key")
                val brandLogoUrl = call.argument<String>("brandLogoUrl")

                // Start the verification process if the required arguments are provided.
                if (id != null && key != null) {
                    startVerification(id, key, brandLogoUrl, result)
                } else {
                    // Return an error if the required arguments are missing.
                    result.error("INVALID_ARGUMENTS", "Missing id or key", null)
                }
            }
            // Handle any other method calls.
            else -> result.notImplemented()
        }
    }

    /**
     * Starts the Stripe Identity verification flow.
     *
     * @param id The verification session ID.
     * @param key The ephemeral key secret.
     * @param brandLogoUrl The URL of the brand logo to display.
     * @param result A closure to return the result of the verification flow to Flutter.
     */
private fun startVerification(id: String, key: String, brandLogoUrl: String?, result: Result) {
    if (identityVerificationSheet == null) {
        result.error("NO_SHEET", "IdentityVerificationSheet not initialized.", null)
        return
    }

    val activity = activity
    if (activity !is FragmentActivity) { // ✅ Ensure it's FragmentActivity
        result.error("NO_ACTIVITY", "Plugin requires a FragmentActivity.", null)
        return
    }

    activity.runOnUiThread {
        identityVerificationSheet?.present(
            verificationSessionId = id,
            ephemeralKeySecret = key
        )
    }
}


    /**
     * Handles the verification result from the IdentityVerificationSheet.
     *
     * @param verificationResult The verification result.
     * @param result A closure to return the result to Flutter.
     */
    private fun handleVerificationResult(
        verificationResult: IdentityVerificationSheet.VerificationFlowResult,
        result: Result
    ) {
        // Handle different verification results.
        when (verificationResult) {
            // Verification completed successfully.
            is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                result.success("completed")
            }
            // Verification canceled by the user.
            is IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                result.success("canceled")
            }
            // Verification failed.
            is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                // Return an error to Flutter with the error message.
                result.error(
                    "failed",
                    verificationResult.throwable.localizedMessage,
                    null
                )
            }
        }
    }

    /**
     * Called when the plugin is detached from the Flutter engine.
     *
     * @param binding The Flutter plugin binding.
     */
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // Remove the method call handler to avoid leaks.
        channel.setMethodCallHandler(null)
    }

    /**
     * Called when the plugin is attached to an activity.
     *
     * @param binding The activity plugin binding.
     */
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    if (activity is FragmentActivity) { // ✅ Use FragmentActivity
        val fragmentActivity = activity as FragmentActivity
        identityVerificationSheet = IdentityVerificationSheet.create(
            fragmentActivity,
            IdentityVerificationSheet.Configuration(brandLogo = null) // Add brandLogo to avoid error
        ) { verificationFlowResult ->
            // Handle the verification result
        }
    } else {
        Log.e("StripeIdentityPlugin", "Activity is not a FragmentActivity")
    }
}

    /**
     * Called when the activity is detached due to configuration changes.
     */
    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
        identityVerificationSheet = null
    }

    /**
     * Called when the activity is reattached after configuration changes.
     *
     * @param binding The activity plugin binding.
     */
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    /**
     * Called when the plugin is detached from an activity.
     */
    override fun onDetachedFromActivity() {
        activity = null
        identityVerificationSheet = null
    }
}
