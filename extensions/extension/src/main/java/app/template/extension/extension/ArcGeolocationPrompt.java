package app.template.extension.extension;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.GeolocationPermissions;

public final class ArcGeolocationPrompt {
    private static final int REQUEST_CODE = 7914;
    private static volatile Activity activity;
    private static GeolocationPermissions.Callback callback;
    private static String origin;

    private ArcGeolocationPrompt() {}

    public static void setActivity(Activity value) {
        if (value != null && !value.isFinishing()) activity = value;
    }

    public static void showPrompt(String requestedOrigin, GeolocationPermissions.Callback requestedCallback) {
        Activity current = activity;
        if (requestedCallback == null) return;
        if (current == null || current.isFinishing()) {
            requestedCallback.invoke(requestedOrigin, false, false);
            return;
        }
        current.runOnUiThread(() -> new AlertDialog.Builder(current)
            .setTitle("위치 정보 요청")
            .setMessage(requestedOrigin + " 사이트에서 위치 정보에 접근하려고 합니다.")
            .setPositiveButton("허용", (dialog, which) -> allow(current, requestedOrigin, requestedCallback))
            .setNegativeButton("차단", (dialog, which) -> requestedCallback.invoke(requestedOrigin, false, false))
            .setOnCancelListener(dialog -> requestedCallback.invoke(requestedOrigin, false, false))
            .show());
    }

    private static void allow(Activity current, String requestedOrigin, GeolocationPermissions.Callback requestedCallback) {
        if (hasPermission(current)) {
            requestedCallback.invoke(requestedOrigin, true, false);
            return;
        }
        if (Build.VERSION.SDK_INT < 23) {
            requestedCallback.invoke(requestedOrigin, false, false);
            return;
        }
        origin = requestedOrigin;
        callback = requestedCallback;
        current.requestPermissions(new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_CODE);
    }

    private static boolean hasPermission(Activity current) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return current.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || current.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQUEST_CODE) return;
        GeolocationPermissions.Callback pending = callback;
        String pendingOrigin = origin;
        callback = null;
        origin = null;
        if (pending == null || pendingOrigin == null) return;
        Activity current = activity;
        boolean granted = current != null && hasPermission(current);
        if (!granted && results != null) {
            for (int result : results) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        pending.invoke(pendingOrigin, granted, false);
    }
}
