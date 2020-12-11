
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterDeviceAdmin {
  static const MethodChannel _channel =
      const MethodChannel('flutter_device_admin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
