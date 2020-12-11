import 'dart:async';

import 'package:flutter/services.dart';

class FlutterDeviceAdmin {
  static const MethodChannel _channel = const MethodChannel('flutter_device_admin');
  static const EventChannel _eventsChannel = const EventChannel('flutter_device_admin/events');

  static Future<String> get platformVersion async {
    try {
      final String version = await _channel.invokeMethod('getPlatformVersion');
      return version;
    } on PlatformException catch (e) {
      print("Failed to Invoke: '${e.message}'.");
    }
    return null;
  }

  static Future<bool> get isEnabled async {
    try {
      final result = await _channel.invokeMethod<bool>("isEnabled");
      return result;
    } on PlatformException catch (e) {
      print("Failed to Invoke: '${e.message}'.");
    }
    return false;
  }

  static Future<bool> get enable async {
    try {
      final result = await _channel.invokeMethod<bool>("enable");
      return result;
    } on PlatformException catch (e) {
      print("Failed to Invoke: '${e.message}'.");
    }
    return false;
  }

  static Future<bool> get disable async {
    try {
      final result = await _channel.invokeMethod<bool>("disable");
      return result;
    } on PlatformException catch (e) {
      print("Failed to Invoke: '${e.message}'.");
    }
    return false;
  }

  static Future<bool> get lock async {
    try {
      final result = await _channel.invokeMethod<bool>("lock");
      return result;
    } on PlatformException catch (e) {
      print("Failed to Invoke: '${e.message}'.");
    }
    return false;
  }

  static StreamController<int> _onWrongPassword2Controller;

  static StreamController<int> get onWrongPassword2 {
    if (_onWrongPassword2Controller == null || _onWrongPassword2Controller.isClosed) {
      _onWrongPassword2Controller = StreamController<int>.broadcast();

      _eventsChannel.receiveBroadcastStream().listen((event) {
        int countErros = -1;
        if (event is int) countErros = event;
        _onWrongPassword2Controller.add(countErros);
      });
    }
    return _onWrongPassword2Controller;
  }
}
