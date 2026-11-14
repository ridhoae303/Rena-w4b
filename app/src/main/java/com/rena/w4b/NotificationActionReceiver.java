package com.rena.w4b;

import android.app.RemoteInput;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public final class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_OPEN =
            "com.rena.w4b.NOTIFICATION_OPEN";
    public static final String ACTION_REPLY =
            "com.rena.w4b.NOTIFICATION_REPLY";
    public static final String ACTION_READ =
            "com.rena.w4b.NOTIFICATION_READ";
    public static final String ACTION_IGNORE =
            "com.rena.w4b.NOTIFICATION_IGNORE";

    public static final String EXTRA_TAB_ID = "tab_id";
    public static final String EXTRA_CHAT = "chat";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_REPLY = "reply";
    private static final String EXTRA_REPLY_KEY = "remote_reply";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        int tabId = Math.max(
                1,
                Math.min(
                        10,
                        intent.getIntExtra(EXTRA_TAB_ID, 1)
                )
        );

        String action = intent.getAction();
        String notificationId =
                "rena_notification_" + tabId;

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (ACTION_IGNORE.equals(action) ||
                ACTION_REPLY.equals(action) ||
                ACTION_READ.equals(action)) {
            if (manager != null) {
                manager.cancel(notificationId.hashCode());
            }
        }

        if (ACTION_IGNORE.equals(action)) {
            return;
        }

        Intent open = new Intent(context, MainActivity.class);

        if (ACTION_REPLY.equals(action)) {
            try {
                Bundle results =
                        RemoteInput.getResultsFromIntent(intent);

                if (results != null) {
                    CharSequence reply =
                            results.getCharSequence(
                                    EXTRA_REPLY_KEY
                            );

                    if (reply != null) {
                        open.putExtra(
                                EXTRA_REPLY,
                                reply.toString()
                        );
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        open.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        if (action != null) {
            open.setAction(action);
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            open.putExtras(extras);
        }
        open.putExtra(EXTRA_TAB_ID, tabId);

        context.startActivity(open);
    }
}
