package com.rena.w4b;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class DeletedMessagesActivity extends Activity {
    private final ArrayList<JSONObject> items = new ArrayList<JSONObject>();
    private BaseAdapter adapter;
    private TextView empty;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RenaTheme.apply(this);
        super.onCreate(savedInstanceState);
        buildUi();
        RenaTheme.applyWindow(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private int dp(float value) {
        return Ui.dp(this, value);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(RenaTheme.bg());
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(6), dp(8), dp(6));
        TextView back = Ui.text(this, NativeConfig.downloadBackSymbol(), 24f, RenaTheme.text(), Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.ripple(this, Ui.round(0, dp(24)), 24));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(Ui.text(this, getString(R.string.deleted_messages_title), 21f, RenaTheme.text(), Typeface.BOLD),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clear = Ui.text(this, getString(R.string.clear_text), 14f, RenaTheme.accentText(), Typeface.BOLD);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(dp(16), 0, dp(16), 0);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RenaDialog.Builder(DeletedMessagesActivity.this)
                        .setTitle(R.string.deleted_clear_title)
                        .setMessage(R.string.deleted_clear_message)
                        .setNegativeButton(R.string.cancel_text, null)
                        .setPositiveButton(R.string.clear_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AntiDeleteStore.clearAll(DeletedMessagesActivity.this);
                                reload();
                            }
                        })
                        .show();
            }
        });
        header.addView(clear, new LinearLayout.LayoutParams(-2, dp(40)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(this, getString(R.string.deleted_note), 12.5f, RenaTheme.muted(), Typeface.NORMAL);
        note.setLineSpacing(0f, 1.2f);
        note.setPadding(dp(20), 0, dp(20), dp(10));
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout body = new FrameLayout(this);
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setClipToPadding(false);
        list.setPadding(0, dp(2), 0, dp(24));
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return card(items.get(position));
            }
        };
        list.setAdapter(adapter);
        body.addView(list, new FrameLayout.LayoutParams(-1, -1));
        empty = Ui.text(this, getString(R.string.deleted_empty), 15f, RenaTheme.muted(), Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        body.addView(empty, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private View card(final JSONObject record) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(Ui.pressable(this, RenaTheme.surface(), 18));
        wrap.addView(card, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        String chat = record.optString("chat", "");
        TextView name = Ui.text(this, TextUtils.isEmpty(chat) ? getString(R.string.deleted_unknown_chat) : chat, 14.5f,
                RenaTheme.accentText(), Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        long deletedAt = record.optLong("deletedAt", 0L);
        TextView time = Ui.text(this, DateUtils.getRelativeTimeSpanString(deletedAt, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString(), 11.5f, RenaTheme.muted(), Typeface.NORMAL);
        top.addView(time, new LinearLayout.LayoutParams(-2, -2));
        card.addView(top, new LinearLayout.LayoutParams(-1, -2));

        String thumb = record.optString("thumb", "");
        if (thumb.startsWith("data:image")) {
            try {
                int comma = thumb.indexOf(',');
                byte[] bytes = Base64.decode(thumb.substring(comma + 1), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    ImageView image = new ImageView(this);
                    image.setImageBitmap(bitmap);
                    image.setAdjustViewBounds(true);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setBackground(Ui.round(RenaTheme.surface2(), dp(12)));
                    image.setClipToOutline(true);
                    image.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, android.graphics.Outline outline) {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(12));
                        }
                    });
                    LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(dp(160), dp(120));
                    imageLp.topMargin = dp(10);
                    card.addView(image, imageLp);
                }
            } catch (Throwable ignored) {
            }
        }
        final String text = record.optString("text", "");
        String body = !TextUtils.isEmpty(text) ? text : getString(R.string.antidelete_media) + ": " + record.optString("kind", "");
        TextView message = Ui.text(this, body, 14.5f, RenaTheme.text(), Typeface.NORMAL);
        message.setLineSpacing(0f, 1.2f);
        message.setTextIsSelectable(false);
        LinearLayout.LayoutParams messageLp = new LinearLayout.LayoutParams(-1, -2);
        messageLp.topMargin = dp(8);
        card.addView(message, messageLp);
        String pre = record.optString("pre", "");
        if (!TextUtils.isEmpty(pre)) {
            TextView meta = Ui.text(this, pre.trim(), 11.5f, RenaTheme.faint(), Typeface.NORMAL);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
            metaLp.topMargin = dp(6);
            card.addView(meta, metaLp);
        }
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                try {
                    ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(ClipData.newPlainText("Rena", text));
                    Toast.makeText(DeletedMessagesActivity.this, R.string.copied_text, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                }
            }
        });
        return wrap;
    }

    private void reload() {
        items.clear();
        items.addAll(AntiDeleteStore.deletedAcrossProfiles(this));
        adapter.notifyDataSetChanged();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
