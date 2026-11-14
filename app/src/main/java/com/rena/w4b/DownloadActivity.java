package com.rena.w4b;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DownloadActivity extends Activity {
    private static final String[] FILTERS = new String[]{"", RenaPaths.IMAGES, RenaPaths.VIDEOS, RenaPaths.AUDIO,
            RenaPaths.DOCUMENT, RenaPaths.PACKAGES, RenaPaths.OTHER};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<RenaDownloadManager.DownloadItem> shown = new ArrayList<RenaDownloadManager.DownloadItem>();
    private final ArrayList<TextView> chips = new ArrayList<TextView>();
    private ListView list;
    private Adapter adapter;
    private TextView empty;
    private String filter = "";
    private String signature = "";
    private File pendingInstall;
    private boolean running;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            refresh(false);
            handler.postDelayed(this, 550L);
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RenaTheme.apply(this);
        super.onCreate(savedInstanceState);
        RenaDownloadManager.initialize(this);
        buildUi();
        RenaTheme.applyWindow(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        refresh(true);
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 550L);
        if (pendingInstall != null && canInstall()) {
            File file = pendingInstall;
            pendingInstall = null;
            install(file);
        }
    }

    @Override
    protected void onPause() {
        running = false;
        handler.removeCallbacks(tick);
        super.onPause();
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

        TextView title = Ui.text(this, getString(R.string.downloads_title), 22f, RenaTheme.text(), Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView clear = Ui.text(this, getString(R.string.clear_text), 14f, RenaTheme.accentText(), Typeface.BOLD);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(dp(16), 0, dp(16), 0);
        clear.setBackground(Ui.ripple(this, Ui.round(0, dp(20)), 20));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearOptions();
            }
        });
        header.addView(clear, new LinearLayout.LayoutParams(-2, dp(40)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(dp(14), dp(4), dp(14), dp(10));
        for (int i = 0; i < FILTERS.length; i++) {
            final String value = FILTERS[i];
            String label = value.length() == 0 ? getString(R.string.download_filter_all) : RenaPaths.label(this, value);
            TextView chip = Ui.text(this, label, 13.5f, RenaTheme.muted(), Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), 0, dp(16), 0);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filter = value;
                    styleChips();
                    refresh(true);
                }
            });
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-2, dp(36));
            chipLp.rightMargin = dp(8);
            chipRow.addView(chip, chipLp);
            chips.add(chip);
        }
        chipScroll.addView(chipRow, new FrameLayout.LayoutParams(-2, -2));
        root.addView(chipScroll, new LinearLayout.LayoutParams(-1, -2));
        styleChips();

        FrameLayout body = new FrameLayout(this);
        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(android.R.color.transparent);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setClipToPadding(false);
        list.setPadding(0, dp(2), 0, dp(24));
        adapter = new Adapter();
        list.setAdapter(adapter);
        body.addView(list, new FrameLayout.LayoutParams(-1, -1));

        empty = Ui.text(this, getString(R.string.download_empty_text), 15f, RenaTheme.muted(), Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        body.addView(empty, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void styleChips() {
        for (int i = 0; i < chips.size(); i++) {
            TextView chip = chips.get(i);
            boolean selected = FILTERS[i].equals(filter);
            chip.setTextColor(selected ? RenaTheme.accentText() : RenaTheme.muted());
            chip.setBackground(Ui.ripple(this, Ui.roundStroke(selected ? RenaTheme.accentSoft() : RenaTheme.surface(), dp(18),
                    selected ? RenaTheme.accent() : RenaTheme.outline(), dp(1)), 18));
        }
    }

    private void showClearOptions() {
        String[] labels = new String[]{getString(R.string.download_clear_finished), getString(R.string.download_delete_all_files)};
        new RenaDialog.Builder(this)
                .setTitle(R.string.clear_text)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            RenaDownloadManager.clearFinishedRecords();
                            refresh(true);
                        } else {
                            confirmDeleteAll();
                        }
                    }
                })
                .show();
    }

    private void confirmDeleteAll() {
        new RenaDialog.Builder(this)
                .setTitle(R.string.clear_downloads_confirm_title)
                .setMessage(R.string.clear_downloads_confirm_message)
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.clear_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RenaDownloadManager.clearAll(true);
                        Toast.makeText(DownloadActivity.this, R.string.downloads_cleared_text, Toast.LENGTH_SHORT).show();
                        refresh(true);
                    }
                })
                .show();
    }

    private void refresh(boolean force) {
        ArrayList<RenaDownloadManager.DownloadItem> all = RenaDownloadManager.getItems();
        ArrayList<RenaDownloadManager.DownloadItem> next = new ArrayList<RenaDownloadManager.DownloadItem>();
        StringBuilder sig = new StringBuilder();
        for (RenaDownloadManager.DownloadItem item : all) {
            if (filter.length() > 0) {
                boolean match = filter.equals(item.folder) || RenaPaths.AUDIO.equals(filter) && RenaPaths.VOICE.equals(item.folder);
                if (!match) {
                    continue;
                }
            }
            next.add(item);
            sig.append(item.id).append(item.status).append(item.downloaded).append(item.speed / 1024).append(item.fileName).append(';');
        }
        String newSignature = sig.toString();
        if (!force && newSignature.equals(signature)) {
            return;
        }
        signature = newSignature;
        shown.clear();
        shown.addAll(next);
        adapter.notifyDataSetChanged();
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String statusText(RenaDownloadManager.DownloadItem item) {
        switch (item.status) {
            case RenaDownloadManager.QUEUED:
                return getString(R.string.download_queued_text);
            case RenaDownloadManager.RUNNING: {
                StringBuilder text = new StringBuilder(getString(R.string.download_running_text));
                text.append(" \u00B7 ").append(DownloadFormat.size(item.downloaded));
                if (item.total > 0) {
                    text.append(" / ").append(DownloadFormat.size(item.total));
                }
                if (item.speed > 0) {
                    text.append(" \u00B7 ").append(DownloadFormat.size(item.speed)).append("/s");
                    if (item.total > item.downloaded) {
                        long seconds = (item.total - item.downloaded) / Math.max(1L, item.speed);
                        text.append(" \u00B7 ").append(DownloadFormat.duration(seconds));
                    }
                }
                return text.toString();
            }
            case RenaDownloadManager.PAUSED: {
                StringBuilder text = new StringBuilder(getString(R.string.download_paused_text));
                text.append(" \u00B7 ").append(DownloadFormat.size(item.downloaded));
                if (item.total > 0) {
                    text.append(" / ").append(DownloadFormat.size(item.total));
                }
                return text.toString();
            }
            case RenaDownloadManager.COMPLETED: {
                long when = item.completedAt > 0 ? item.completedAt : item.createdAt;
                CharSequence relative = DateUtils.getRelativeTimeSpanString(when, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                return DownloadFormat.size(item.downloaded) + " \u00B7 " + relative;
            }
            case RenaDownloadManager.CANCELLED:
                return getString(R.string.download_cancelled_text);
            default:
                return getString(R.string.download_failed_prefix) + " \u00B7 " + errorText(item.error);
        }
    }

    private String errorText(String code) {
        if (code == null) {
            return getString(R.string.download_error_network);
        }
        if (code.startsWith("http-")) {
            return getString(R.string.download_error_http, code.substring(5));
        }
        if ("write".equals(code) || "mkdir".equals(code) || "rename".equals(code)) {
            return getString(R.string.download_error_storage);
        }
        if ("missing".equals(code)) {
            return getString(R.string.download_error_missing);
        }
        if ("blob-start-timeout".equals(code) || "incomplete".equals(code) || "interrupted".equals(code)) {
            return getString(R.string.download_error_source);
        }
        return getString(R.string.download_error_network);
    }

    private final class Holder {
        FrameLayout wrap;
        LinearLayout card;
        ImageView thumb;
        TextView name;
        TextView status;
        TextView extra;
        ProgressLine progress;
        LinearLayout actions;
        TextView[] buttons = new TextView[4];
        String boundKey = "";
    }

    private final class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return shown.get(position).id.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                holder = createHolder();
                convertView = holder.wrap;
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            bind(holder, shown.get(position));
            return convertView;
        }
    }

    private Holder createHolder() {
        final Holder holder = new Holder();
        holder.wrap = new FrameLayout(this);
        holder.wrap.setPadding(dp(14), dp(6), dp(14), dp(6));
        holder.card = new LinearLayout(this);
        holder.card.setOrientation(LinearLayout.VERTICAL);
        holder.card.setPadding(dp(14), dp(14), dp(14), dp(12));
        holder.card.setBackground(Ui.pressable(this, RenaTheme.surface(), 20));
        holder.wrap.addView(holder.card, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        holder.thumb = new ImageView(this);
        holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.thumb.setClipToOutline(true);
        final int radius = dp(14);
        holder.thumb.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        top.addView(holder.thumb, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        infoLp.leftMargin = dp(14);
        top.addView(info, infoLp);

        holder.name = Ui.text(this, "", 15f, RenaTheme.text(), Typeface.BOLD);
        holder.name.setMaxLines(2);
        holder.name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        info.addView(holder.name, new LinearLayout.LayoutParams(-2, -2));

        holder.status = Ui.text(this, "", 12.5f, RenaTheme.muted(), Typeface.NORMAL);
        holder.status.setSingleLine(true);
        holder.status.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-2, -2);
        statusLp.topMargin = dp(5);
        info.addView(holder.status, statusLp);

        holder.extra = Ui.text(this, "", 11.5f, RenaTheme.faint(), Typeface.NORMAL);
        holder.extra.setSingleLine(true);
        holder.extra.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams extraLp = new LinearLayout.LayoutParams(-2, -2);
        extraLp.topMargin = dp(3);
        info.addView(holder.extra, extraLp);
        holder.card.addView(top, new LinearLayout.LayoutParams(-1, -2));

        holder.progress = new ProgressLine(this);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, dp(5));
        progressLp.topMargin = dp(12);
        holder.card.addView(holder.progress, progressLp);

        holder.actions = new LinearLayout(this);
        holder.actions.setOrientation(LinearLayout.HORIZONTAL);
        holder.actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        for (int i = 0; i < holder.buttons.length; i++) {
            TextView button = Ui.text(this, "", 13f, RenaTheme.accentText(), Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(14), 0, dp(14), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(36));
            lp.leftMargin = dp(8);
            holder.actions.addView(button, lp);
            holder.buttons[i] = button;
        }
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2);
        actionsLp.topMargin = dp(10);
        holder.card.addView(holder.actions, actionsLp);
        return holder;
    }

    private void styleButton(TextView button, String label, int color, boolean danger) {
        button.setText(label);
        button.setTextColor(color);
        button.setVisibility(View.VISIBLE);
        int fill = danger ? RenaTheme.dangerSoft() : RenaTheme.accentSoft();
        button.setBackground(Ui.ripple(this, Ui.round(fill, dp(18)), 18));
    }

    private void bind(final Holder holder, final RenaDownloadManager.DownloadItem item) {
        holder.name.setText(item.fileName);
        holder.status.setText(statusText(item));
        int statusColor = item.status == RenaDownloadManager.FAILED ? RenaTheme.danger()
                : item.status == RenaDownloadManager.RUNNING ? RenaTheme.accentText() : RenaTheme.muted();
        holder.status.setTextColor(statusColor);

        String key = DownloadThumbs.keyFor(item);
        String extra = DownloadThumbs.cachedExtra(key);
        holder.extra.setText(extra != null && extra.length() > 0 ? extra : RenaPaths.displayPath(item.folder));
        holder.boundKey = key;
        Bitmap cached = item.status == RenaDownloadManager.COMPLETED ? DownloadThumbs.cached(key) : null;
        if (cached != null) {
            holder.thumb.setImageBitmap(cached);
        } else {
            holder.thumb.setImageDrawable(DownloadThumbs.badge(this, item.fileName, item.folder));
            if (DownloadThumbs.wantsPreview(item)) {
                final String boundKey = key;
                DownloadThumbs.load(this, item, dp(58) * 2, new DownloadThumbs.Callback() {
                    @Override
                    public void onLoaded(String loadedKey, Bitmap bitmap, String loadedExtra) {
                        if ((bitmap != null || loadedExtra != null) && boundKey.equals(holder.boundKey)) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }

        boolean active = item.status == RenaDownloadManager.RUNNING || item.status == RenaDownloadManager.QUEUED ||
                item.status == RenaDownloadManager.PAUSED;
        holder.progress.setVisibility(active ? View.VISIBLE : View.GONE);
        if (active) {
            holder.progress.setProgress(item.total > 0 ? item.percent() / 100f : -1f,
                    item.status == RenaDownloadManager.PAUSED);
        }

        for (TextView button : holder.buttons) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
        }
        int slot = 0;
        final String id = item.id;
        if (item.status == RenaDownloadManager.RUNNING || item.status == RenaDownloadManager.QUEUED) {
            styleButton(holder.buttons[slot], getString(R.string.download_pause_text), RenaTheme.accentText(), false);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RenaDownloadManager.pause(id);
                    refresh(true);
                }
            });
            styleButton(holder.buttons[slot], getString(R.string.download_cancel_text), RenaTheme.danger(), true);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RenaDownloadManager.cancel(id);
                    refresh(true);
                }
            });
        } else if (item.status == RenaDownloadManager.PAUSED) {
            styleButton(holder.buttons[slot], getString(R.string.download_resume_text), RenaTheme.accentText(), false);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RenaDownloadManager.resume(id);
                    refresh(true);
                }
            });
            styleButton(holder.buttons[slot], getString(R.string.download_cancel_text), RenaTheme.danger(), true);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RenaDownloadManager.cancel(id);
                    refresh(true);
                }
            });
        } else if (item.status == RenaDownloadManager.COMPLETED) {
            final File file = new File(item.path);
            String ext = DownloadNames.extensionOf(item.fileName);
            if ("apk".equals(ext)) {
                styleButton(holder.buttons[slot], getString(R.string.download_install_text), RenaTheme.accentText(), false);
                holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        install(file);
                    }
                });
            }
            styleButton(holder.buttons[slot], getString(R.string.download_open_text), RenaTheme.accentText(), false);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFile(file);
                }
            });
            styleButton(holder.buttons[slot], getString(R.string.download_share_text), RenaTheme.accentText(), false);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareFile(file);
                }
            });
            styleButton(holder.buttons[slot], getString(R.string.download_delete_text), RenaTheme.danger(), true);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete(item);
                }
            });
        } else {
            if (!item.blob) {
                styleButton(holder.buttons[slot], getString(R.string.download_retry_text), RenaTheme.accentText(), false);
                holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RenaDownloadManager.retry(id);
                        refresh(true);
                    }
                });
            }
            styleButton(holder.buttons[slot], getString(R.string.download_delete_text), RenaTheme.danger(), true);
            holder.buttons[slot++].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RenaDownloadManager.delete(id, false);
                    refresh(true);
                }
            });
        }
        holder.actions.setVisibility(slot > 0 ? View.VISIBLE : View.GONE);

        holder.card.setOnClickListener(item.status == RenaDownloadManager.COMPLETED ? new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile(new File(item.path));
            }
        } : null);
        holder.card.setClickable(item.status == RenaDownloadManager.COMPLETED);
    }

    private void confirmDelete(final RenaDownloadManager.DownloadItem item) {
        new RenaDialog.Builder(this)
                .setTitle(R.string.download_delete_confirm_title)
                .setMessage(getString(R.string.download_delete_confirm_message, item.fileName))
                .setNegativeButton(R.string.cancel_text, null)
                .setPositiveButton(R.string.download_delete_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RenaDownloadManager.delete(item.id, true);
                        refresh(true);
                    }
                })
                .show();
    }

    private void openFile(File file) {
        if (file == null || !file.isFile()) {
            Toast.makeText(this, R.string.download_error_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        if (file.getName().toLowerCase(java.util.Locale.US).endsWith(".apk")) {
            install(file);
            return;
        }
        try {
            Uri uri = UpdateFileProvider.buildUri(this, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, DownloadNames.mimeForName(file.getName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, file.getName()));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.download_open_failed_text, Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, R.string.download_open_failed_text, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        if (file == null || !file.isFile()) {
            Toast.makeText(this, R.string.download_error_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = UpdateFileProvider.buildUri(this, file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(DownloadNames.mimeForName(file.getName()));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, file.getName()));
        } catch (Throwable error) {
            Toast.makeText(this, R.string.download_open_failed_text, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canInstall() {
        return Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls();
    }

    private void install(final File file) {
        if (file == null || !file.isFile()) {
            Toast.makeText(this, R.string.download_error_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canInstall()) {
            pendingInstall = file;
            new RenaDialog.Builder(this)
                    .setTitle(R.string.download_install_permission_title)
                    .setMessage(R.string.download_install_permission_message)
                    .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pendingInstall = null;
                        }
                    })
                    .setPositiveButton(R.string.storage_settings_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Throwable error) {
                                pendingInstall = null;
                                Toast.makeText(DownloadActivity.this, R.string.download_install_failed_text, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(UpdateFileProvider.buildUri(this, file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable error) {
            Toast.makeText(this, R.string.download_install_failed_text, Toast.LENGTH_SHORT).show();
        }
    }

    private static final class ProgressLine extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;
        private boolean paused;
        private long started = System.currentTimeMillis();

        ProgressLine(Context context) {
            super(context);
        }

        void setProgress(float progress, boolean pausedState) {
            value = progress;
            paused = pausedState;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float height = getHeight();
            float radius = height / 2f;
            track.setColor(RenaTheme.fg(30));
            canvas.drawRoundRect(new RectF(0, 0, getWidth(), height), radius, radius, track);
            fill.setColor(paused ? RenaTheme.muted() : RenaTheme.accent());
            if (value >= 0f) {
                float width = Math.max(height, getWidth() * Math.min(1f, value));
                canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius, fill);
            } else {
                long elapsed = System.currentTimeMillis() - started;
                float phase = (elapsed % 1400L) / 1400f;
                float segment = getWidth() * 0.32f;
                float left = (getWidth() + segment) * phase - segment;
                canvas.drawRoundRect(new RectF(Math.max(0, left), 0, Math.min(getWidth(), left + segment), height), radius, radius, fill);
                postInvalidateDelayed(32);
            }
        }
    }
}
