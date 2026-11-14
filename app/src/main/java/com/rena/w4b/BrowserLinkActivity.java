package com.rena.w4b;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class BrowserLinkActivity extends Activity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        Intent out = new Intent(this, MainActivity.class);
        if (incoming != null) {
            out.setAction(incoming.getAction());
            out.setData(incoming.getData());
            out.setType(incoming.getType());
            if (incoming.getExtras() != null) {
                out.putExtras(incoming.getExtras());
            }
        }
        out.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(out);
        } catch (Throwable ignored) {
        }
        finish();
    }
}
