package com.personal.messageforwarder.diagnostics;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Restores the user-enabled foreground monitor after a completed boot or app update. */
public final class BootReceiver extends BroadcastReceiver {
    static boolean isRestoreAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
            || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !isRestoreAction(intent.getAction()) || !AutoForwarder.enabled(context)) return;

        String reason = Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ? "재부팅" : "앱 업데이트";
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
            || context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
            || context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            AutoForwarder.update(context, reason + " 후 자동발송 복구 실패: SMS 권한을 확인해 주세요.");
            return;
        }

        AutoForwarder.update(context, reason + " 후 자동발송 복구 시작");
        try {
            context.startForegroundService(new Intent(context, AutoService.class)
                .setAction(AutoService.ACTION_RESTORE)
                .putExtra(AutoService.EXTRA_RESTORE_REASON, reason));
        } catch (RuntimeException e) {
            AutoForwarder.update(context, reason + " 후 자동발송 시작 실패: " + e.getClass().getSimpleName());
        }
    }
}
