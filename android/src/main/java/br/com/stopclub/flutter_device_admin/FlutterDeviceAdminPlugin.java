package br.com.stopclub.flutter_device_admin;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class FlutterDeviceAdminPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {
    private final Object initializationLock = new Object();

    private Activity activity;
    private ComponentName compName;
    private static DevicePolicyManager deviceManager;

    // Channel
    private MethodChannel channel;
    private MethodChannel.Result enabledMethodChannelResult;

    // Events
    private EventChannel eventChannel;
    private static EventChannel.EventSink _events;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("deprecations")
    public static void registerWith(PluginRegistry.Registrar registrar) {
        final FlutterDeviceAdminPlugin plugin = new FlutterDeviceAdminPlugin();
        plugin.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context context, BinaryMessenger messenger) {
        synchronized (initializationLock) {
            if (compName != null)
                return;

            compName = new ComponentName(context, StopClubAdminReceiver.class);
            deviceManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

            // Channel
            channel = new MethodChannel(messenger, "flutter_device_admin");
            channel.setMethodCallHandler(this);

            // Wrong access events
            eventChannel = new EventChannel(messenger, "flutter_device_admin/events");
            eventChannel.setStreamHandler(this);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        activity = null;
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
            eventChannel = null;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "isEnabled":
                boolean isEnabled = deviceManager.isAdminActive(compName);
                result.success(isEnabled);
                break;
            case "enable":
                if (compName != null) {
                    enabledMethodChannelResult = result;
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Precisamos de autorização para saber quando tentativas de acesso incorretas forem feitas.");
                    activity.startActivityForResult(intent, 1);
                }
                break;
            case "disable":
                if (compName != null && deviceManager.isAdminActive(compName)) {
                    try {
                        deviceManager.removeActiveAdmin(compName);
                        result.success(true);
                    } catch (Exception ignored) {
                        result.success(false);
                    }
                } else {
                    result.success(true);
                }
                break;
            case "lock":
                try {
                    deviceManager.lockNow();
                    result.success(true);
                } catch (Exception ignore) {
                    result.success(false);
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        _events = events;
    }

    @Override
    public void onCancel(Object arguments) {
        if (_events != null)
            _events.endOfStream();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1) return false;
        if (enabledMethodChannelResult != null)
            enabledMethodChannelResult.success(resultCode == FlutterActivity.RESULT_OK);
        return true;
    }

    static public class StopClubAdminReceiver extends DeviceAdminReceiver {
        @Override
        public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
            super.onEnabled(context, intent);
        }

        @Override
        public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
            super.onDisabled(context, intent);
        }

        @Override
        public void onPasswordFailed(@NonNull Context context, @NonNull Intent intent, @NonNull UserHandle user) {
            super.onPasswordFailed(context, intent, user);
            notifyPasswordFaield();
        }

        @Override
        public void onPasswordFailed(@NonNull Context context, @NonNull Intent intent) {
            super.onPasswordFailed(context, intent);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                notifyPasswordFaield();
        }

        void notifyPasswordFaield() {
            if (_events != null) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        _events.success(deviceManager.getCurrentFailedPasswordAttempts());
                    }
                };
                mainHandler.post(runnable);
            }
        }
    }
}
