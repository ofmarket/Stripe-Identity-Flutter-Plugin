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

/**
 * A Flutter plugin for Stripe Identity verification.
 */
class IdentityPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    private var identityVerificationSheet: IdentityVerificationSheet? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "stripe_identity_plugin")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "startVerification" -> {
                val id = call.argument<String>("id")
                val key = call.argument<String>("key")
                val brandLogoUrl = call.argument<String>("brandLogoUrl")

                if (id != null && key != null) {
                    startVerification(id, key, brandLogoUrl, result)
                } else {
                    result.error("INVALID_ARGUMENTS", "Missing id or key", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun startVerification(id: String, key: String, brandLogoUrl: String?, result: Result) {
        if (identityVerificationSheet == null) {
            result.error("NO_SHEET", "IdentityVerificationSheet not initialized.", null)
            return
        }

        val activity = activity
        if (activity !is FragmentActivity) {
            result.error("NO_ACTIVITY", "Plugin requires a FragmentActivity.", null)
            return
        }

        // Handle brandLogoUrl, allowing it to be null safely
        val brandLogoUri: Uri = brandLogoUrl?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) } ?: Uri.EMPTY

        val configuration = IdentityVerificationSheet.Configuration(brandLogo = brandLogoUri)

        activity.runOnUiThread {
            identityVerificationSheet?.present(
                verificationSessionId = id,
                ephemeralKeySecret = key
            )
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        if (activity is FragmentActivity) {
            val fragmentActivity = activity as FragmentActivity

            identityVerificationSheet = IdentityVerificationSheet.create(
                fragmentActivity,
                IdentityVerificationSheet.Configuration(brandLogo = Uri.EMPTY) // Avoid passing `null`
            ) { verificationFlowResult ->
                handleVerificationResult(verificationFlowResult)
            }
        } else {
            Log.e("StripeIdentityPlugin", "Activity is not a FragmentActivity")
        }
    }

    private fun handleVerificationResult(verificationResult: IdentityVerificationSheet.VerificationFlowResult) {
        when (verificationResult) {
            is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                Log.d("StripeIdentityPlugin", "Verification completed")
                channel.invokeMethod("onVerificationComplete", "completed")
            }
            is IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                Log.d("StripeIdentityPlugin", "Verification canceled")
                channel.invokeMethod("onVerificationComplete", "canceled")
            }
            is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                Log.e("StripeIdentityPlugin", "Verification failed: ${verificationResult.throwable.localizedMessage}")
                channel.invokeMethod("onVerificationComplete", "failed: ${verificationResult.throwable.localizedMessage}")
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
        identityVerificationSheet = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
        identityVerificationSheet = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
