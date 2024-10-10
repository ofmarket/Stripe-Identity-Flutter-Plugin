class StripeIdentityException implements Exception {
  final String code;
  final String? message;

  StripeIdentityException(this.code, this.message);

  @override
  String toString() => 'StripeIdentityException($code, $message)';
}
