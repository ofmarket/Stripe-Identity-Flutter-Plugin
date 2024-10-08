import 'dart:developer';

import 'package:flutter/foundation.dart';
import 'package:stripe_identity_plugin/stripe_identity_plugin_platform_interface.dart';
import 'package:stripe_identity_plugin/utils/enum.dart';
import 'package:stripe_identity_plugin/utils/exception.dart';

class StripeIdentityPlugin {
  //* THE FUNCTION TO BEGIN VERIFICATION ON BOTH ANDROID AND IOS
  //* You should call your server endpoint before calling this method
  //* This method will receive the [verificationSessionId] and [ephemeralKeySecret]
  //* You should also pass your brand logo to this method.
  //! Recommended image size is [32 x 32 points]
  Future<(VerificationResult status, String? message)> startVerification({
    //* The verificationSessionId from your server endpoint
    required String id,
    //* The ephemeralKeySecret from your server endpoint
    required String key,
    //* Configure a square brand logo. Recommended image size is [32 x 32 points].
    String? brandLogoUrl,
  }) async {
    try {
      //* The verification begins at this point.
      if (kDebugMode) {
        log("Attempting to start verification", name: "StripeIdentityPlugin");
      }
      final result = await IdentityPlatform.instance.startVerification(
        id: id,
        key: key,
        brandLogoUrl: brandLogoUrl,
      );
      //* Returns a parsed verification result based on the result received from the platform.
      return _parseVerificationResult(result);
    } on StripeIdentityException catch (e) {
      if (kDebugMode) {
        log("Error while starting verification\nThe message is: ${e.message} and the code is: ${e.code}",
            name: "StripeIdentityPlugin");
      }
      //! It will most likely result to this exception when:
      //? On the iOS side, [result(FlutterError)] is returned
      //? On the Android side, [result.error()] is returned
      return (VerificationResult.failed, e.message);
    } catch (e) {
      if (kDebugMode) {
        log("Error while starting verification\nThe error message is: ${e.toString()}",
            name: "StripeIdentityPlugin");
      }
      return (VerificationResult.unknown, e.toString());
    }
  }

  (VerificationResult status, String message) _parseVerificationResult(
      String result) {
    switch (result) {
      case 'completed':
        //* The user has completed uploading their documents.
        //* Let them know that the verification is processing.
        //* This can also mean that the verification is successful or not.
        return (VerificationResult.completed, "Verification is completed.");
      case 'canceled':
        //* The user has canceled the verification OR
        //* The user did not complete uploading their documents.
        //* You should allow them to try again.
        return (VerificationResult.canceled, "Verification is canceled.");
      case 'failed':
        //* If the flow fails, you should display the localized error
        //* message to your user
        return (VerificationResult.failed, "Verification failed.");
      default:
        //* An unknown error occured. They can try again.
        return (VerificationResult.unknown, "Unknown error.");
    }
  }
}
