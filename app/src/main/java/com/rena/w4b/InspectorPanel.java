package com.rena.w4b;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public final class InspectorPanel extends FrameLayout {
    public interface Target {
        WebView webView();

        String hubKey();

        CookieManager cookieManager();

        String userAgent();
    }

    private static final int TAB_ELEMENTS = 0;
    private static final int TAB_CONSOLE = 1;
    private static final int TAB_NETWORK = 2;
    private static final int TAB_SOURCES = 3;
    private static final int TAB_STORAGE = 4;
    private static final int TAB_INFO = 5;

    private final Activity activity;
    private final Target target;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout body;
    private final ArrayList<TextView> tabViews = new ArrayList<TextView>();
    private final Runnable onClose;
    private int currentTab = TAB_CONSOLE;
    private Runnable refresher;
    private String sourceCache = "";
    private boolean sourceRendered;
    private String networkFilter = "";

    public InspectorPanel(Activity activity, Target target, Runnable onClose) {
        super(activity);
        this.activity = activity;
        this.target = target;
        this.onClose = onClose;
        setBackgroundColor(RenaTheme.bg());
        setClickable(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 6), Ui.dp(activity, 8), Ui.dp(activity, 2));
        TextView title = Ui.text(activity, activity.getString(R.string.inspect_title), 20f, RenaTheme.text(), Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = Ui.text(activity, "\u2715", 18f, RenaTheme.text(), Typeface.NORMAL);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.ripple(activity, Ui.round(0, Ui.dp(activity, 22)), 22));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        header.addView(close, new LinearLayout.LayoutParams(Ui.dp(activity, 44), Ui.dp(activity, 44)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 4), Ui.dp(activity, 10), Ui.dp(activity, 8));
        int[] labels = new int[]{R.string.inspect_elements, R.string.inspect_console, R.string.inspect_network,
                R.string.inspect_sources, R.string.inspect_storage, R.string.inspect_info};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView tab = Ui.text(activity, activity.getString(labels[i]), 13.5f, RenaTheme.muted(), Typeface.BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(Ui.dp(activity, 16), 0, Ui.dp(activity, 16), 0);
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(index);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.dp(activity, 36));
            lp.rightMargin = Ui.dp(activity, 6);
            tabs.addView(tab, lp);
            tabViews.add(tab);
        }
        scroll.addView(tabs, new FrameLayout.LayoutParams(-2, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -2));

        body = new FrameLayout(activity);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        addView(root, new FrameLayout.LayoutParams(-1, -1));
        ensureHooks();
        select(TAB_CONSOLE);
    }

    private void dismiss() {
        stopRefresh();
        if (onClose != null) {
            onClose.run();
        }
    }

    public void stopRefresh() {
        if (refresher != null) {
            handler.removeCallbacks(refresher);
            refresher = null;
        }
    }

    private void ensureHooks() {
        WebView view = target.webView();
        if (view == null) {
            return;
        }
        try {
            view.evaluateJavascript("(function(){window.__RENA_CFG=window.__RENA_CFG||{};})();" + PageScripts.load(activity, "inspector"), null);
        } catch (Throwable ignored) {
        }
    }

    private void styleTabs() {
        for (int i = 0; i < tabViews.size(); i++) {
            boolean selected = i == currentTab;
            TextView tab = tabViews.get(i);
            tab.setTextColor(selected ? RenaTheme.accentText() : RenaTheme.muted());
            tab.setBackground(Ui.ripple(activity, Ui.roundStroke(selected ? RenaTheme.accentSoft() : RenaTheme.surface(),
                    Ui.dp(activity, 18), selected ? RenaTheme.accent() : RenaTheme.outline(), Ui.dp(activity, 1)), 18));
        }
    }

    private void select(int index) {
        stopRefresh();
        currentTab = index;
        styleTabs();
        body.removeAllViews();
        switch (index) {
            case TAB_ELEMENTS:
                buildElements();
                break;
            case TAB_CONSOLE:
                buildConsole();
                break;
            case TAB_NETWORK:
                buildNetwork();
                break;
            case TAB_SOURCES:
                buildSources();
                break;
            case TAB_STORAGE:
                buildStorage();
                break;
            default:
                buildInfo();
                break;
        }
    }

    private InspectorHub hub() {
        return InspectorHub.forKey(target.hubKey());
    }

    private TextView mono(String text, int color) {
        TextView view = new TextView(activity);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(11.5f);
        view.setTextColor(color);
        view.setText(text);
        view.setTextIsSelectable(true);
        return view;
    }

    private void copy(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("Rena", text));
            Toast.makeText(activity, R.string.copied_text, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private void buildConsole() {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setStackFromBottom(false);
        list.setSelector(android.R.color.transparent);
        final ArrayList<InspectorHub.Entry> shown = new ArrayList<InspectorHub.Entry>();
        final BaseAdapter adapter = new BaseAdapter() {
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
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = convertView instanceof TextView ? (TextView) convertView : new TextView(activity);
                InspectorHub.Entry entry = shown.get(position);
                view.setTypeface(Typeface.MONOSPACE);
                view.setTextSize(11.5f);
                view.setPadding(Ui.dp(activity, 12), Ui.dp(activity, 6), Ui.dp(activity, 12), Ui.dp(activity, 6));
                int color = RenaTheme.text();
                int fill = 0;
                String prefix = "";
                if ("error".equals(entry.level)) {
                    color = RenaTheme.danger();
                    fill = RenaTheme.dangerSoft();
                    prefix = "\u2716 ";
                } else if ("warn".equals(entry.level) || "warning".equals(entry.level)) {
                    color = RenaTheme.isDark() ? Color.rgb(255, 214, 102) : Color.rgb(150, 100, 0);
                    fill = Color.argb(30, 255, 200, 0);
                    prefix = "\u26A0 ";
                } else if ("input".equals(entry.level)) {
                    color = RenaTheme.accentText();
                    prefix = "\u203A ";
                } else if ("result".equals(entry.level)) {
                    color = RenaTheme.info();
                    prefix = "\u2039 ";
                } else if ("debug".equals(entry.level) || "tip".equals(entry.level)) {
                    color = RenaTheme.muted();
                }
                view.setTextColor(color);
                view.setBackgroundColor(fill);
                view.setText(prefix + entry.text);
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                copy(shown.get(position).text);
                return true;
            }
        });
        column.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 8), Ui.dp(activity, 10), Ui.dp(activity, 10));
        inputRow.setBackgroundColor(RenaTheme.sheet());
        final EditText input = new EditText(activity);
        input.setHint(R.string.inspect_console_hint);
        input.setHintTextColor(RenaTheme.faint());
        input.setTextColor(RenaTheme.text());
        input.setTypeface(Typeface.MONOSPACE);
        input.setTextSize(13f);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackground(Ui.roundStroke(RenaTheme.surface2(), Ui.dp(activity, 22), RenaTheme.outline(), Ui.dp(activity, 1)));
        input.setPadding(Ui.dp(activity, 16), 0, Ui.dp(activity, 16), 0);
        inputRow.addView(input, new LinearLayout.LayoutParams(0, Ui.dp(activity, 44), 1f));
        TextView run = Ui.text(activity, activity.getString(R.string.inspect_run), 14f, Color.WHITE, Typeface.BOLD);
        run.setGravity(Gravity.CENTER);
        run.setBackground(Ui.ripple(activity, Ui.round(RenaTheme.accent(), Ui.dp(activity, 22)), 22));
        LinearLayout.LayoutParams runLp = new LinearLayout.LayoutParams(Ui.dp(activity, 68), Ui.dp(activity, 44));
        runLp.leftMargin = Ui.dp(activity, 8);
        inputRow.addView(run, runLp);
        TextView clear = Ui.text(activity, "\u2205", 18f, RenaTheme.muted(), Typeface.NORMAL);
        clear.setGravity(Gravity.CENTER);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hub().clearConsole();
            }
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(Ui.dp(activity, 40), Ui.dp(activity, 44));
        inputRow.addView(clear, clearLp);
        column.addView(inputRow, new LinearLayout.LayoutParams(-1, -2));

        final Runnable execute = new Runnable() {
            @Override
            public void run() {
                final String code = input.getText().toString().trim();
                if (code.length() == 0) {
                    return;
                }
                input.setText("");
                hub().addConsole("input", code);
                WebView view = target.webView();
                if (view == null) {
                    return;
                }
                ensureHooks();
                String script = "(function(){try{return window.__renaEval?window.__renaEval(" + PageScripts.quote(code)
                        + "):'inspector not ready';}catch(e){return 'Uncaught '+e;}})()";
                view.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        hub().addConsole("result", decode(value));
                    }
                });
            }
        };
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                execute.run();
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                execute.run();
                return true;
            }
        });
        body.addView(column, new FrameLayout.LayoutParams(-1, -1));

        final int[] lastCount = new int[]{-1};
        refresher = new Runnable() {
            @Override
            public void run() {
                ArrayList<InspectorHub.Entry> entries = hub().console();
                if (entries.size() != lastCount[0]) {
                    boolean atEnd = list.getLastVisiblePosition() >= shown.size() - 2;
                    lastCount[0] = entries.size();
                    shown.clear();
                    shown.addAll(entries);
                    adapter.notifyDataSetChanged();
                    if (atEnd && !shown.isEmpty()) {
                        list.setSelection(shown.size() - 1);
                    }
                }
                handler.postDelayed(this, 500L);
            }
        };
        refresher.run();
    }

    private String decode(String value) {
        if (value == null || "null".equals(value)) {
            return "undefined";
        }
        try {
            Object parsed = new JSONTokener(value).nextValue();
            return parsed == null ? "undefined" : String.valueOf(parsed);
        } catch (Throwable ignored) {
            return value;
        }
    }

    private void buildNetwork() {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(activity, 12), 0, Ui.dp(activity, 12), Ui.dp(activity, 8));
        EditText filter = new EditText(activity);
        filter.setHint(R.string.inspect_filter_hint);
        filter.setHintTextColor(RenaTheme.faint());
        filter.setTextColor(RenaTheme.text());
        filter.setTextSize(13f);
        filter.setSingleLine(true);
        filter.setText(networkFilter);
        filter.setBackground(Ui.roundStroke(RenaTheme.surface(), Ui.dp(activity, 20), RenaTheme.outline(), Ui.dp(activity, 1)));
        filter.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 14), 0);
        bar.addView(filter, new LinearLayout.LayoutParams(0, Ui.dp(activity, 40), 1f));
        TextView clear = Ui.text(activity, activity.getString(R.string.clear_text), 13f, RenaTheme.accentText(), Typeface.BOLD);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 6), 0);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hub().clearNetwork();
            }
        });
        bar.addView(clear, new LinearLayout.LayoutParams(-2, Ui.dp(activity, 40)));
        column.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        final ArrayList<InspectorHub.Entry> shown = new ArrayList<InspectorHub.Entry>();
        final BaseAdapter adapter = new BaseAdapter() {
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
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row;
                if (convertView instanceof LinearLayout) {
                    row = (LinearLayout) convertView;
                    row.removeAllViews();
                } else {
                    row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.VERTICAL);
                }
                row.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 8), Ui.dp(activity, 14), Ui.dp(activity, 8));
                InspectorHub.Entry entry = shown.get(position);
                String status = entry.status < 0 ? "\u2026" : (entry.status == 0 ? "ERR" : String.valueOf(entry.status));
                int color = entry.status >= 400 || entry.status == 0 ? RenaTheme.danger()
                        : entry.status >= 300 ? RenaTheme.info() : entry.status < 0 ? RenaTheme.muted() : RenaTheme.accentText();
                TextView top = Ui.text(activity, entry.method + "  " + status + (entry.ms >= 0 ? "  " + entry.ms + " ms" : "")
                        + (entry.size >= 0 ? "  " + DownloadFormat.size(entry.size) : ""), 11.5f, color, Typeface.BOLD);
                top.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                row.addView(top, new LinearLayout.LayoutParams(-2, -2));
                TextView url = mono(entry.url, RenaTheme.text());
                url.setTextIsSelectable(false);
                url.setMaxLines(2);
                url.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                row.addView(url, new LinearLayout.LayoutParams(-1, -2));
                if (!TextUtils.isEmpty(entry.type)) {
                    TextView type = mono(entry.type, RenaTheme.faint());
                    type.setTextSize(10.5f);
                    row.addView(type, new LinearLayout.LayoutParams(-2, -2));
                }
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showRequest(shown.get(position));
            }
        });
        column.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        body.addView(column, new FrameLayout.LayoutParams(-1, -1));

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                networkFilter = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final int[] signature = new int[]{-1};
        refresher = new Runnable() {
            @Override
            public void run() {
                ArrayList<InspectorHub.Entry> entries = hub().network();
                String needle = networkFilter.toLowerCase(Locale.US);
                ArrayList<InspectorHub.Entry> next = new ArrayList<InspectorHub.Entry>();
                int sig = needle.hashCode();
                for (int i = entries.size() - 1; i >= 0; i--) {
                    InspectorHub.Entry entry = entries.get(i);
                    if (needle.length() == 0 || entry.url.toLowerCase(Locale.US).contains(needle) ||
                            entry.method.toLowerCase(Locale.US).contains(needle) || String.valueOf(entry.status).contains(needle)) {
                        next.add(entry);
                        sig = sig * 31 + entry.status + (int) entry.ms;
                    }
                }
                if (sig != signature[0] || next.size() != shown.size()) {
                    signature[0] = sig;
                    shown.clear();
                    shown.addAll(next);
                    adapter.notifyDataSetChanged();
                }
                handler.postDelayed(this, 700L);
            }
        };
        refresher.run();
    }

    private void showRequest(InspectorHub.Entry entry) {
        StringBuilder text = new StringBuilder();
        text.append(entry.method).append(' ').append(entry.url).append("\n\n");
        text.append("Status: ").append(entry.status < 0 ? "pending" : String.valueOf(entry.status)).append('\n');
        if (entry.ms >= 0) {
            text.append("Time: ").append(entry.ms).append(" ms\n");
        }
        if (entry.size >= 0) {
            text.append("Size: ").append(DownloadFormat.size(entry.size)).append('\n');
        }
        if (!TextUtils.isEmpty(entry.type)) {
            text.append("Type: ").append(entry.type).append('\n');
        }
        if (!TextUtils.isEmpty(entry.via)) {
            text.append("Via: ").append(entry.via).append('\n');
        }
        if (!TextUtils.isEmpty(entry.headers)) {
            text.append("\nRequest headers\n").append(entry.headers);
        }
        final String content = text.toString();
        ScrollView scroll = new ScrollView(activity);
        TextView view = mono(content, RenaTheme.text());
        scroll.addView(view);
        new RenaDialog.Builder(activity)
                .setTitle(R.string.inspect_request)
                .setView(scroll)
                .setNeutralButton(R.string.copy_text, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        copy(content);
                    }
                })
                .setPositiveButton(R.string.okay_text, null)
                .show();
    }

    private static final class Node {
        String text;
        boolean element;
        String name = "";
        String attrs = "";
        String comment;
        ArrayList<Node> children = new ArrayList<Node>();
        int more;
        boolean expanded;
    }

    private Node parseNode(JSONObject object) {
        Node node = new Node();
        String type = object.optString("t");
        if ("text".equals(type)) {
            node.text = object.optString("v");
        } else if ("comment".equals(type)) {
            node.comment = object.optString("v");
        } else {
            node.element = true;
            node.name = object.optString("n");
            node.attrs = object.optString("a");
            node.more = object.optInt("more", 0);
            JSONArray children = object.optJSONArray("c");
            if (children != null) {
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.optJSONObject(i);
                    if (child != null) {
                        node.children.add(parseNode(child));
                    }
                }
            }
        }
        return node;
    }

    private void flatten(Node node, int depth, ArrayList<Object[]> out) {
        out.add(new Object[]{node, depth});
        if (node.element && node.expanded) {
            for (Node child : node.children) {
                flatten(child, depth + 1, out);
            }
        }
    }

    private void buildElements() {
        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        final ArrayList<Object[]> rows = new ArrayList<Object[]>();
        final Node[] rootHolder = new Node[1];
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return rows.size();
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = convertView instanceof TextView ? (TextView) convertView : new TextView(activity);
                Node node = (Node) rows.get(position)[0];
                int depth = (Integer) rows.get(position)[1];
                view.setTypeface(Typeface.MONOSPACE);
                view.setTextSize(11.5f);
                view.setPadding(Ui.dp(activity, 10 + Math.min(depth, 14) * 10), Ui.dp(activity, 5), Ui.dp(activity, 10), Ui.dp(activity, 5));
                if (node.element) {
                    boolean has = !node.children.isEmpty() || node.more > 0;
                    String arrow = has ? (node.expanded ? "\u25BE " : "\u25B8 ") : "  ";
                    String attrs = node.attrs.length() > 0 ? " " + node.attrs : "";
                    String label = arrow + "<" + node.name + attrs + ">" + (has && !node.expanded ? "\u2026</" + node.name + ">" : "");
                    SpannableString spannable = new SpannableString(label);
                    view.setTextColor(RenaTheme.isDark() ? Color.rgb(125, 200, 255) : Color.rgb(20, 90, 160));
                    view.setText(spannable);
                } else if (node.comment != null) {
                    view.setTextColor(RenaTheme.faint());
                    view.setText("  <!-- " + node.comment + " -->");
                } else {
                    view.setTextColor(RenaTheme.text());
                    view.setText("  " + node.text);
                }
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Node node = (Node) rows.get(position)[0];
                if (node.element) {
                    node.expanded = !node.expanded;
                    rows.clear();
                    if (rootHolder[0] != null) {
                        flatten(rootHolder[0], 0, rows);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
        list.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Node node = (Node) rows.get(position)[0];
                copy(node.element ? "<" + node.name + (node.attrs.length() > 0 ? " " + node.attrs : "") + ">" :
                        (node.text == null ? node.comment : node.text));
                return true;
            }
        });
        body.addView(list, new FrameLayout.LayoutParams(-1, -1));
        final TextView status = Ui.text(activity, activity.getString(R.string.loading_text), 14f, RenaTheme.muted(), Typeface.NORMAL);
        status.setGravity(Gravity.CENTER);
        body.addView(status, new FrameLayout.LayoutParams(-1, -1));
        WebView view = target.webView();
        if (view == null) {
            return;
        }
        view.evaluateJavascript("(function(){try{return window.__renaDom?window.__renaDom(7):'null';}catch(e){return 'null';}})()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try {
                    Object parsed = new JSONTokener(value).nextValue();
                    if (parsed instanceof String && !"null".equals(parsed)) {
                        Node root = parseNode(new JSONObject((String) parsed));
                        root.expanded = true;
                        for (Node child : root.children) {
                            if (child.element && ("head".equals(child.name) || "body".equals(child.name))) {
                                child.expanded = true;
                            }
                        }
                        rootHolder[0] = root;
                        rows.clear();
                        flatten(root, 0, rows);
                        adapter.notifyDataSetChanged();
                        status.setVisibility(View.GONE);
                        return;
                    }
                } catch (Throwable ignored) {
                }
                status.setText(R.string.inspect_unavailable);
            }
        });
    }

    private void buildSources() {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(activity, 12), 0, Ui.dp(activity, 12), Ui.dp(activity, 8));
        final EditText search = new EditText(activity);
        search.setHint(R.string.inspect_search_hint);
        search.setHintTextColor(RenaTheme.faint());
        search.setTextColor(RenaTheme.text());
        search.setTextSize(13f);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackground(Ui.roundStroke(RenaTheme.surface(), Ui.dp(activity, 20), RenaTheme.outline(), Ui.dp(activity, 1)));
        search.setPadding(Ui.dp(activity, 14), 0, Ui.dp(activity, 14), 0);
        bar.addView(search, new LinearLayout.LayoutParams(0, Ui.dp(activity, 40), 1f));
        final TextView mode = Ui.text(activity, activity.getString(sourceRendered ? R.string.inspect_rendered : R.string.inspect_raw), 13f, RenaTheme.accentText(), Typeface.BOLD);
        mode.setGravity(Gravity.CENTER);
        mode.setPadding(Ui.dp(activity, 12), 0, Ui.dp(activity, 6), 0);
        bar.addView(mode, new LinearLayout.LayoutParams(-2, Ui.dp(activity, 40)));
        TextView copy = Ui.text(activity, activity.getString(R.string.copy_text), 13f, RenaTheme.accentText(), Typeface.BOLD);
        copy.setGravity(Gravity.CENTER);
        copy.setPadding(Ui.dp(activity, 6), 0, Ui.dp(activity, 4), 0);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy(sourceCache);
            }
        });
        bar.addView(copy, new LinearLayout.LayoutParams(-2, Ui.dp(activity, 40)));
        column.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        final ArrayList<String> lines = new ArrayList<String>();
        final String[] needle = new String[]{""};
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return lines.size();
            }

            @Override
            public Object getItem(int position) {
                return lines.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = convertView instanceof TextView ? (TextView) convertView : new TextView(activity);
                view.setTypeface(Typeface.MONOSPACE);
                view.setTextSize(11f);
                view.setTextColor(RenaTheme.text());
                view.setPadding(Ui.dp(activity, 8), Ui.dp(activity, 1), Ui.dp(activity, 8), Ui.dp(activity, 1));
                String line = lines.get(position);
                String number = String.format(Locale.US, "%4d  ", position + 1);
                SpannableString spannable = new SpannableString(number + line);
                if (needle[0].length() > 0) {
                    String lower = line.toLowerCase(Locale.US);
                    int index = lower.indexOf(needle[0]);
                    while (index >= 0) {
                        spannable.setSpan(new BackgroundColorSpan(Color.argb(150, 255, 200, 0)), number.length() + index,
                                number.length() + index + needle[0].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        index = lower.indexOf(needle[0], index + needle[0].length());
                    }
                }
                view.setText(spannable);
                return view;
            }
        };
        list.setAdapter(adapter);
        column.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        final TextView status = Ui.text(activity, activity.getString(R.string.loading_text), 14f, RenaTheme.muted(), Typeface.NORMAL);
        status.setGravity(Gravity.CENTER);
        FrameLayout holder = new FrameLayout(activity);
        holder.addView(column, new FrameLayout.LayoutParams(-1, -1));
        holder.addView(status, new FrameLayout.LayoutParams(-1, -1));
        body.addView(holder, new FrameLayout.LayoutParams(-1, -1));

        final Runnable load = new Runnable() {
            @Override
            public void run() {
                status.setVisibility(View.VISIBLE);
                status.setText(R.string.loading_text);
                lines.clear();
                adapter.notifyDataSetChanged();
                if (sourceRendered) {
                    WebView view = target.webView();
                    if (view == null) {
                        return;
                    }
                    view.evaluateJavascript("(function(){try{return '<!DOCTYPE html>\\n'+document.documentElement.outerHTML;}catch(e){return '';}})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            String text = decode(value);
                            display(text, lines, adapter, status);
                        }
                    });
                } else {
                    fetchRaw(new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String text) {
                            display(text, lines, adapter, status);
                        }
                    });
                }
            }
        };
        mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceRendered = !sourceRendered;
                mode.setText(sourceRendered ? R.string.inspect_rendered : R.string.inspect_raw);
                load.run();
            }
        });
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                needle[0] = search.getText().toString().toLowerCase(Locale.US);
                adapter.notifyDataSetChanged();
                if (needle[0].length() > 0) {
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).toLowerCase(Locale.US).contains(needle[0])) {
                            list.setSelection(i);
                            break;
                        }
                    }
                }
                return true;
            }
        });
        load.run();
    }

    private void display(String text, ArrayList<String> lines, BaseAdapter adapter, TextView status) {
        sourceCache = text == null ? "" : text;
        lines.clear();
        if (sourceCache.length() == 0) {
            status.setText(R.string.inspect_unavailable);
            adapter.notifyDataSetChanged();
            return;
        }
        String clipped = sourceCache.length() > 600000 ? sourceCache.substring(0, 600000) : sourceCache;
        for (String line : clipped.split("\n", -1)) {
            lines.add(line.length() > 1500 ? line.substring(0, 1500) + "\u2026" : line);
        }
        status.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void fetchRaw(final ValueCallback<String> callback) {
        final WebView view = target.webView();
        final String url = view == null ? null : view.getUrl();
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            callback.onReceiveValue("");
            return;
        }
        final CookieManager cookies = target.cookieManager();
        final String agent = target.userAgent();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = "";
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setRequestProperty("User-Agent", agent);
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*");
                    String cookie = cookies == null ? null : cookies.getCookie(url);
                    if (!TextUtils.isEmpty(cookie)) {
                        connection.setRequestProperty("Cookie", cookie);
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null && builder.length() < 900000) {
                        builder.append(line).append('\n');
                    }
                    reader.close();
                    result = builder.toString();
                } catch (Throwable ignored) {
                }
                final String text = result;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onReceiveValue(text);
                    }
                });
            }
        }).start();
    }

    private void buildStorage() {
        final ScrollView scroll = new ScrollView(activity);
        final LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 4), Ui.dp(activity, 14), Ui.dp(activity, 24));
        scroll.addView(column, new FrameLayout.LayoutParams(-1, -2));
        body.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        WebView view = target.webView();
        if (view == null) {
            return;
        }
        String url = view.getUrl();
        CookieManager cookies = target.cookieManager();
        String cookieString = url == null || cookies == null ? "" : cookies.getCookie(url);
        addSection(column, activity.getString(R.string.inspect_cookies) + " (HTTP)");
        if (TextUtils.isEmpty(cookieString)) {
            column.addView(emptyLine());
        } else {
            for (String pair : cookieString.split(";\\s*")) {
                int eq = pair.indexOf('=');
                addKeyValue(column, eq > 0 ? pair.substring(0, eq) : pair, eq > 0 ? pair.substring(eq + 1) : "");
            }
        }
        view.evaluateJavascript("(function(){try{return window.__renaStorage?window.__renaStorage():'{}';}catch(e){return '{}';}})()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try {
                    Object parsed = new JSONTokener(value).nextValue();
                    JSONObject object = new JSONObject(String.valueOf(parsed));
                    addSection(column, "localStorage");
                    JSONObject local = object.optJSONObject("local");
                    fill(column, local);
                    addSection(column, "sessionStorage");
                    JSONObject session = object.optJSONObject("session");
                    fill(column, session);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private void fill(LinearLayout column, JSONObject object) {
        if (object == null || object.length() == 0) {
            column.addView(emptyLine());
            return;
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            addKeyValue(column, key, object.optString(key));
        }
    }

    private View emptyLine() {
        TextView view = Ui.text(activity, activity.getString(R.string.inspect_empty), 12.5f, RenaTheme.faint(), Typeface.ITALIC);
        view.setPadding(Ui.dp(activity, 6), Ui.dp(activity, 4), 0, Ui.dp(activity, 6));
        return view;
    }

    private void addSection(LinearLayout column, String title) {
        TextView view = Ui.sectionTitle(activity, title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = Ui.dp(activity, 16);
        lp.bottomMargin = Ui.dp(activity, 6);
        column.addView(view, lp);
    }

    private void addKeyValue(LinearLayout column, final String key, final String value) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(Ui.dp(activity, 12), Ui.dp(activity, 8), Ui.dp(activity, 12), Ui.dp(activity, 8));
        row.setBackground(Ui.pressable(activity, RenaTheme.surface(), 12));
        TextView k = Ui.text(activity, key, 12.5f, RenaTheme.accentText(), Typeface.BOLD);
        k.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        row.addView(k, new LinearLayout.LayoutParams(-2, -2));
        TextView v = mono(value.length() > 400 ? value.substring(0, 400) + "\u2026" : value, RenaTheme.text());
        v.setTextIsSelectable(false);
        row.addView(v, new LinearLayout.LayoutParams(-1, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copy(key + "=" + value);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Ui.dp(activity, 6);
        column.addView(row, lp);
    }

    private void buildInfo() {
        final ScrollView scroll = new ScrollView(activity);
        final LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 4), Ui.dp(activity, 14), Ui.dp(activity, 24));
        scroll.addView(column, new FrameLayout.LayoutParams(-1, -2));
        body.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        WebView view = target.webView();
        if (view == null) {
            return;
        }
        view.evaluateJavascript("(function(){try{return window.__renaInfo?window.__renaInfo():'{}';}catch(e){return '{}';}})()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try {
                    Object parsed = new JSONTokener(value).nextValue();
                    JSONObject object = new JSONObject(String.valueOf(parsed));
                    Iterator<String> keys = object.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String v = object.optString(key);
                        if ("load".equals(key) || "dom".equals(key)) {
                            v = v + " ms";
                        }
                        addKeyValue(column, key, v);
                    }
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                    addKeyValue(column, "inspected", time);
                    addKeyValue(column, "webview", DeviceProfiles.fullVersion(activity));
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
