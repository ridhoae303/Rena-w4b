package com.rena.w4b;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UpdateSuccessDialog {
    private UpdateSuccessDialog() {
    }

    public static void show(final Activity activity, String versionName) {
        int size = Ui.dp(activity, 96);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout stage = new FrameLayout(activity);
        final View ring = new View(activity);
        ring.setBackground(Ui.roundStroke(Color.TRANSPARENT, Ui.dp(activity, 60), RenaTheme.accent(), Ui.dp(activity, 3)));
        ring.setAlpha(0f);
        stage.addView(ring, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        final ImageView icon = new ImageView(activity);
        Bitmap preview = IconSwitcher.preview(activity, IconSwitcher.current(activity), size);
        if (preview != null) {
            icon.setImageBitmap(preview);
        } else {
            icon.setImageResource(R.mipmap.ic_launcher);
        }
        icon.setScaleX(0f);
        icon.setScaleY(0f);
        icon.setRotation(-24f);
        stage.addView(icon, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        final TextView badge = Ui.text(activity, "\u2713", 18f, Color.WHITE, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.round(RenaTheme.accent(), Ui.dp(activity, 16)));
        badge.setScaleX(0f);
        badge.setScaleY(0f);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(Ui.dp(activity, 32), Ui.dp(activity, 32), Gravity.CENTER);
        badgeLp.leftMargin = Ui.dp(activity, 64);
        badgeLp.topMargin = Ui.dp(activity, 64);
        stage.addView(badge, badgeLp);
        content.addView(stage, new LinearLayout.LayoutParams(Ui.dp(activity, 140), Ui.dp(activity, 130)));

        final TextView title = Ui.text(activity, activity.getString(R.string.update_success_title), 20f, RenaTheme.text(), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.topMargin = Ui.dp(activity, 8);
        content.addView(title, titleLp);

        final TextView version = Ui.text(activity, activity.getString(R.string.update_success_version, versionName), 14f,
                RenaTheme.accentText(), Typeface.BOLD);
        version.setGravity(Gravity.CENTER);
        version.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 6), Ui.dp(activity, 14), Ui.dp(activity, 6));
        version.setBackground(Ui.round(RenaTheme.accentSoft(), Ui.dp(activity, 16)));
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(-2, -2);
        versionLp.topMargin = Ui.dp(activity, 12);
        content.addView(version, versionLp);

        final TextView message = Ui.text(activity, activity.getString(R.string.update_success_message), 13.5f, RenaTheme.muted(), Typeface.NORMAL);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams messageLp = new LinearLayout.LayoutParams(-2, -2);
        messageLp.topMargin = Ui.dp(activity, 12);
        content.addView(message, messageLp);
        title.setAlpha(0f);
        version.setAlpha(0f);
        message.setAlpha(0f);

        final RenaDialog dialog = new RenaDialog.Builder(activity)
                .setView(content)
                .setDim(0.5f)
                .setPositiveButton(R.string.update_success_close, null)
                .setCancelable(true)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                ObjectAnimator sx = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0f, 1f);
                ObjectAnimator sy = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0f, 1f);
                ObjectAnimator rot = ObjectAnimator.ofFloat(icon, View.ROTATION, -24f, 0f);
                sx.setInterpolator(new OvershootInterpolator(2.2f));
                sy.setInterpolator(new OvershootInterpolator(2.2f));
                AnimatorSet iconSet = new AnimatorSet();
                iconSet.playTogether(sx, sy, rot);
                iconSet.setDuration(520);
                iconSet.setStartDelay(120);
                iconSet.start();

                ObjectAnimator bx = ObjectAnimator.ofFloat(badge, View.SCALE_X, 0f, 1f);
                ObjectAnimator by = ObjectAnimator.ofFloat(badge, View.SCALE_Y, 0f, 1f);
                bx.setInterpolator(new OvershootInterpolator(3f));
                by.setInterpolator(new OvershootInterpolator(3f));
                AnimatorSet badgeSet = new AnimatorSet();
                badgeSet.playTogether(bx, by);
                badgeSet.setDuration(380);
                badgeSet.setStartDelay(560);
                badgeSet.start();

                ring.setAlpha(0.7f);
                ObjectAnimator rx = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.9f, 1.7f);
                ObjectAnimator ry = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.9f, 1.7f);
                ObjectAnimator ra = ObjectAnimator.ofFloat(ring, View.ALPHA, 0.7f, 0f);
                AnimatorSet ringSet = new AnimatorSet();
                ringSet.playTogether(rx, ry, ra);
                ringSet.setDuration(900);
                ringSet.setStartDelay(380);
                ringSet.start();

                View[] texts = new View[]{title, version, message};
                for (int i = 0; i < texts.length; i++) {
                    texts[i].setTranslationY(Ui.dp(activity, 12));
                    texts[i].animate().alpha(1f).translationY(0f).setStartDelay(520 + i * 110L).setDuration(300).start();
                }
            }
        });
        dialog.show();
    }
}
