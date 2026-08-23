package com.rena.w4b;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class TeamDeveloperActivity extends Activity {
    private LinearLayout contributorsContainer;
    private TextView contributorsState;
    private boolean reduceAnimations;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        reduceAnimations =
                RenaSettingsStore.getBoolean(
                        this,
                        "reduce_animation",
                        false
                );

        configureWindow();
        buildUi();
        loadContributors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureWindow();
        reduceAnimations =
                RenaSettingsStore.getBoolean(
                        this,
                        "reduce_animation",
                        false
                );
    }

    private void configureWindow() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.BLACK);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 27, 33));
        root.setPadding(dp(14), dp(8), dp(14), dp(10));

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            root.setOnApplyWindowInsetsListener(
                    new View.OnApplyWindowInsetsListener() {
                        @Override
                        public android.view.WindowInsets onApplyWindowInsets(
                                View v,
                                android.view.WindowInsets insets
                        ) {
                            v.setPadding(
                                    dp(14),
                                    dp(8) + insets.getSystemWindowInsetTop(),
                                    dp(14),
                                    dp(10) + insets.getSystemWindowInsetBottom()
                            );
                            return insets;
                        }
                    }
            );
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton back = new ImageButton(this);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setImageDrawable(createBackArrowDrawable());
        back.setContentDescription(
                NativeConfig.teamBackContentDescription()
        );
        back.setColorFilter(Color.WHITE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithAnimation();
            }
        });

        header.addView(
                back,
                new LinearLayout.LayoutParams(dp(48), dp(48))
        );

        TextView title = text(
                NativeConfig.teamDeveloperText(),
                21,
                Color.WHITE
        );
        title.setTypeface(loadFont("font", Typeface.BOLD));
        title.setGravity(Gravity.CENTER_VERTICAL);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1f
                )
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        TextView officialTitle = sectionTitle(
                NativeConfig.officialTeamText()
        );
        root.addView(
                officialTitle,
                sectionTitleLp(
                        dp(44),
                        dp(10),
                        dp(10),
                        dp(10),
                        dp(8)
                )
        );

        addOfficialMember(
                root,
                NativeConfig.officialTeamName1(),
                NativeConfig.officialTeamUsername1(),
                NativeConfig.officialTeamRole1(),
                true
        );

        addOfficialMember(
                root,
                NativeConfig.officialTeamName2(),
                NativeConfig.officialTeamUsername2(),
                NativeConfig.officialTeamRole2(),
                false
        );

        addOfficialMember(
                root,
                NativeConfig.officialTeamName3(),
                NativeConfig.officialTeamUsername3(),
                NativeConfig.officialTeamRole3(),
                false
        );

        TextView contributorTitle =
                sectionTitle(NativeConfig.contributorText());
        root.addView(
                contributorTitle,
                sectionTitleLp(
                        dp(44),
                        dp(12),
                        dp(12),
                        dp(10),
                        dp(8)
                )
        );

        contributorsState =
                text(
                        NativeConfig.teamLoadingText(),
                        13,
                        Color.argb(170, 255, 255, 255)
                );
        contributorsState.setPadding(
                dp(8),
                0,
                dp(8),
                0
        );
        // Keep the loading state compact while the live GitHub request is running.
        contributorsState.setVisibility(View.VISIBLE);
        contributorsState.setGravity(Gravity.CENTER_VERTICAL);

        root.addView(
                contributorsState,
                exact(dp(34))
        );

        contributorsContainer =
                new LinearLayout(this);
        contributorsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                contributorsContainer,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(true);
        scroll.addView(
                root,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(scroll);
    }

    private TextView sectionTitle(String value) {
        TextView title = text(
                value,
                14,
                Color.argb(185, 255, 255, 255)
        );
        title.setTypeface(loadFont("font", Typeface.BOLD));
        title.setPadding(dp(8), dp(8), 0, 0);
        return title;
    }

    private LinearLayout.LayoutParams sectionTitleLp(
            int height,
            int left,
            int top,
            int right,
            int bottom
    ) {
        LinearLayout.LayoutParams lp = exact(height);
        lp.leftMargin = left;
        lp.topMargin = top;
        lp.rightMargin = right;
        lp.bottomMargin = bottom;
        return lp;
    }

    private TextView text(
            String value,
            float size,
            int color
    ) {
        TextView view = new TextView(this);
        view.setText(
                TextUtils.isEmpty(value)
                        ? ""
                        : value
        );
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams exact(int height) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
    }

    private void addOfficialMember(
            final LinearLayout parent,
            final String name,
            final String username,
            final String role,
            final boolean developerFont
    ) {
        View card = createMemberCard(
                name,
                username,
                role,
                developerFont,
                null
        );

        LinearLayout.LayoutParams cardLp = exact(dp(86));
        cardLp.leftMargin = dp(4);
        cardLp.rightMargin = dp(4);
        cardLp.topMargin = dp(5);
        cardLp.bottomMargin = dp(5);

        parent.addView(
                card,
                cardLp
        );
    }

    private View createMemberCard(
            final String name,
            final String username,
            final String role,
            final boolean developerFont,
            final String avatarUrl
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), 0, dp(8), 0);
        card.setBackground(
                rounded(
                        Color.argb(30, 255, 255, 255),
                        dp(18)
                )
        );

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            card.setElevation(dp(1.5f));
        }

        ImageView avatar = avatarView();

        card.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(50),
                        dp(50)
                )
        );

        LinearLayout details =
                new LinearLayout(this);
        details.setOrientation(
                LinearLayout.VERTICAL
        );
        details.setGravity(Gravity.CENTER_VERTICAL);
        details.setPadding(dp(12), 0, 0, 0);

        TextView nameView =
                text(
                        name,
                        16,
                        Color.WHITE
                );
        nameView.setTypeface(
                loadFont(
                        developerFont
                                ? "ridhoae303"
                                : "font",
                        Typeface.BOLD
                )
        );

        TextView usernameView =
                text(
                        username,
                        11,
                        Color.argb(175, 0, 220, 172)
                );
        usernameView.setTypeface(
                loadFont(
                        "font",
                        Typeface.NORMAL
                )
        );

        TextView roleView =
                text(
                        role,
                        11,
                        Color.argb(160, 255, 255, 255)
                );
        roleView.setTypeface(
                loadFont(
                        "font",
                        Typeface.NORMAL
                )
        );

        details.addView(
                nameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(24)
                )
        );
        details.addView(
                usernameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(19)
                )
        );
        details.addView(
                roleView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(19)
                )
        );

        card.addView(
                details,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        ImageButton github =
                new ImageButton(this);
        github.setBackground(
                rounded(
                        Color.rgb(34, 38, 42),
                        dp(14)
                )
        );
        github.setPadding(
                dp(9),
                dp(9),
                dp(9),
                dp(9)
        );
        github.setContentDescription(
                NativeConfig.teamGithubContentDescription()
        );

        int githubId = getResources().getIdentifier(
                "github",
                "drawable",
                getPackageName()
        );

        if (githubId != 0) {
            try {
                github.setImageResource(githubId);
            } catch (Throwable ignored) {
                github.setImageResource(
                        android.R.drawable.ic_menu_info_details
                );
            }
        } else {
            github.setImageResource(
                    android.R.drawable.ic_menu_info_details
            );
        }

        github.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );
        // Preserve the supplied GitHub icon colors; tinting it white makes
        // the black mark disappear into the white circle.
        final String profileUrl =
                NativeConfig.githubProfileUrl(username);

        github.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(profileUrl);
            }
        });

        card.addView(
                github,
                new LinearLayout.LayoutParams(
                        dp(46),
                        dp(46)
                )
        );

        loadAvatar(
                avatar,
                username,
                avatarUrl
        );

        return card;
    }

    private ImageView avatarView() {
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable background =
                new GradientDrawable();
        background.setShape(
                GradientDrawable.OVAL
        );
        background.setColor(
                Color.rgb(42, 49, 54)
        );

        avatar.setBackground(background);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            avatar.setClipToOutline(true);
        }

        return avatar;
    }

    private void loadContributors() {
        if (contributorsContainer != null) {
            contributorsContainer.removeAllViews();
        }

        if (!hasInternetConnection()) {
            showOfflineState();
            return;
        }

        if (contributorsState != null) {
            contributorsState.setText(NativeConfig.teamLoadingText());
            contributorsState.setVisibility(View.VISIBLE);
        }

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final java.util.ArrayList<Contributor> result =
                                    fetchFreshContributors();

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isFinishing() ||
                                                    (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                                                return;
                                            }

                                            if (result.isEmpty()) {
                                                showContributorToast(
                                                        NativeConfig.teamNoContributorsText()
                                                );
                                                hideContributorState();
                                                return;
                                            }

                                            showContributors(result);
                                        }
                                    }
                            );
                        } catch (final Exception error) {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isFinishing() ||
                                                    (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                                                return;
                                            }

                                            showContributorFailure(
                                                    NativeConfig.teamRequestFailedText()
                                            );
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private java.util.ArrayList<Contributor> fetchFreshContributors()
            throws Exception {
        java.util.ArrayList<Contributor> result =
                new java.util.ArrayList<Contributor>();

        String nextUrl = NativeConfig.contributorsApiUrl();

        while (!TextUtils.isEmpty(nextUrl)) {
            HttpResult response = requestText(nextUrl);
            if (response.statusCode < 200 ||
                    response.statusCode >= 300) {
                throw new java.io.IOException(
                        "GitHub HTTP " + response.statusCode
                );
            }

            JSONArray array = new JSONArray(response.body);

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) {
                    continue;
                }

                String login = item.optString("login", "");
                if (TextUtils.isEmpty(login)) {
                    continue;
                }

                String avatarUrl = item.optString("avatar_url", "");
                int contributions = item.optInt("contributions", 0);

                result.add(
                        new Contributor(
                                login,
                                avatarUrl,
                                contributions
                        )
                );
            }

            nextUrl = extractNextLink(response.linkHeader);
        }

        return result;
    }

    private void showOfflineState() {
        if (contributorsContainer != null) {
            contributorsContainer.removeAllViews();
        }

        if (contributorsState != null) {
            contributorsState.setText(
                    NativeConfig.noInternetConnectionText()
            );
            contributorsState.setVisibility(View.VISIBLE);
        }
    }

    private void hideContributorState() {
        if (contributorsState != null) {
            contributorsState.setText("");
            contributorsState.setVisibility(View.GONE);
        }
    }

    private void showContributorFailure(String message) {
        if (contributorsContainer != null) {
            contributorsContainer.removeAllViews();
        }

        boolean offline = !hasInternetConnection();
        if (offline) {
            showOfflineState();
            return;
        }

        hideContributorState();
        showContributorToast(message);
    }

    private void showContributorToast(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private boolean hasInternetConnection() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            if (android.os.Build.VERSION.SDK_INT < 23) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }

            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showContributors(
            java.util.ArrayList<Contributor> result
    ) {
        if (contributorsContainer == null) {
            return;
        }

        contributorsContainer.removeAllViews();

        if (result == null || result.isEmpty()) {
            showContributorToast(
                    NativeConfig.teamNoContributorsText()
            );
            return;
        }

        contributorsState.setText("");
        contributorsState.setVisibility(View.GONE);

        for (Contributor contributor : result) {
            LinearLayout.LayoutParams cardLp = exact(dp(86));
            cardLp.leftMargin = dp(4);
            cardLp.rightMargin = dp(4);
            cardLp.topMargin = dp(5);
            cardLp.bottomMargin = dp(5);

            contributorsContainer.addView(
                    createContributorCard(contributor),
                    cardLp
            );
        }
    }

    private View createContributorCard(
            Contributor contributor
    ) {
        String username = contributor.username;
        String displayName = contributor.username;
        String role = "Contributor";
        boolean developerFont = false;

        if (username.equalsIgnoreCase(
                NativeConfig.officialTeamUsername1()
        )) {
            displayName = NativeConfig.officialTeamName1();
            role = NativeConfig.officialTeamRole1();
            developerFont = true;
        } else if (username.equalsIgnoreCase(
                NativeConfig.officialTeamUsername2()
        )) {
            displayName = NativeConfig.officialTeamName2();
            role = NativeConfig.officialTeamRole2();
        } else if (username.equalsIgnoreCase(
                NativeConfig.officialTeamUsername3()
        )) {
            displayName = NativeConfig.officialTeamName3();
            role = NativeConfig.officialTeamRole3();
        }

        View card = createMemberCard(
                displayName,
                username,
                role,
                developerFont,
                contributor.avatarUrl
        );

        return card;
    }

    private void loadAvatar(
            final ImageView target,
            final String username,
            final String avatarUrl
    ) {
        if (target == null || TextUtils.isEmpty(username)) {
            return;
        }

        target.setImageDrawable(null);

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = null;

                        try {
                            File file =
                                    ContributorCache.avatarFile(
                                            TeamDeveloperActivity.this,
                                            username
                                    );

                            if (file.exists()) {
                                bitmap =
                                        BitmapFactory.decodeFile(
                                                file.getAbsolutePath()
                                        );
                            }
                        } catch (Throwable ignored) {
                        }

                        final Bitmap cachedBitmap = bitmap;

                        if (cachedBitmap != null) {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                target.setImageBitmap(cachedBitmap);
                                            }
                                        }
                                    }
                            );
                        }

                        String sourceUrl =
                                TextUtils.isEmpty(avatarUrl)
                                        ? NativeConfig.githubAvatarUrl(username)
                                        : avatarUrl;

                        BitmapResult fetched = fetchBitmap(sourceUrl);
                        final Bitmap freshBitmap = fetched.bitmap;

                        if (freshBitmap != null) {
                            try {
                                File file = ContributorCache.avatarFile(
                                        TeamDeveloperActivity.this,
                                        username
                                );
                                File parent = file.getParentFile();
                                if (parent != null && !parent.exists()) {
                                    parent.mkdirs();
                                }
                                FileOutputStream output = new FileOutputStream(file);
                                freshBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                                output.close();
                            } catch (Throwable ignored) {
                            }

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                target.setImageBitmap(freshBitmap);
                                            }
                                        }
                                    }
                            );
                        }

                    }
                }
        );
    }

    private HttpResult requestText(String urlText) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            connection =
                    (HttpURLConnection)
                            new URL(urlText)
                                    .openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(9000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
            );
            connection.setRequestProperty(
                    "X-GitHub-Api-Version",
                    "2026-03-10"
            );
            connection.setRequestProperty(
                    "User-Agent",
                    NativeConfig.developerName()
            );
            connection.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
            );

            int statusCode = connection.getResponseCode();
            input = statusCode >= 200 && statusCode < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (input == null) {
                throw new java.io.IOException(
                        "GitHub HTTP " + statusCode + " without response body"
                );
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            return new HttpResult(
                    statusCode,
                    output.toString("UTF-8"),
                    connection.getHeaderField("Link")
            );
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractNextLink(String linkHeader) {
        if (TextUtils.isEmpty(linkHeader)) {
            return null;
        }

        String[] links = linkHeader.split(",");
        for (String link : links) {
            String part = link.trim();
            int open = part.indexOf('<');
            int close = part.indexOf('>');
            if (open < 0 || close <= open) {
                continue;
            }

            String relation = part.substring(close + 1).trim();
            if (relation.contains("rel=\"next\"")) {
                return part.substring(open + 1, close);
            }
        }

        return null;
    }

    private BitmapResult fetchBitmap(String urlText) {
        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            connection =
                    (HttpURLConnection)
                            new URL(urlText)
                                    .openConnection();

            connection.setConnectTimeout(6000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(true);
            connection.setRequestProperty(
                    "User-Agent",
                    NativeConfig.developerName()
            );

            if (connection.getResponseCode() < 200 ||
                    connection.getResponseCode() >= 300) {
                return new BitmapResult(null);
            }

            input = connection.getInputStream();

            return new BitmapResult(
                    android.graphics.BitmapFactory
                            .decodeStream(input)
            );
        } catch (Throwable ignored) {
            return new BitmapResult(null);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Throwable ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Typeface loadFont(
            String name,
            int style
    ) {
        try {
            int id =
                    getResources().getIdentifier(
                            name,
                            "font",
                            getPackageName()
                    );

            if (id != 0 &&
                    android.os.Build.VERSION.SDK_INT >= 26) {
                return Typeface.create(
                        getResources().getFont(id),
                        style
                );
            }
        } catch (Throwable ignored) {
        }

        return Typeface.create(
                "sans-serif",
                style
        );
    }

    private GradientDrawable rounded(
            int color,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void openUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        try {
            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    )
            );
        } catch (Throwable ignored) {
        }
    }

    private Drawable createBackArrowDrawable() {
        android.graphics.drawable.ShapeDrawable drawable =
                new android.graphics.drawable.ShapeDrawable(
                        new android.graphics.drawable.shapes.PathShape(
                                new android.graphics.Path() {{
                                    moveTo(dp(20), dp(4));
                                    lineTo(dp(8), dp(14));
                                    lineTo(dp(20), dp(24));
                                    moveTo(dp(8), dp(14));
                                    lineTo(dp(31), dp(14));
                                }},
                                dp(32),
                                dp(28)
                        )
                );

        drawable.getPaint().setColor(Color.WHITE);
        drawable.getPaint().setStyle(
                android.graphics.Paint.Style.STROKE
        );
        drawable.getPaint().setStrokeWidth(dp(2.2f));
        drawable.getPaint().setStrokeCap(
                android.graphics.Paint.Cap.SQUARE
        );
        drawable.getPaint().setStrokeJoin(
                android.graphics.Paint.Join.MITER
        );
        drawable.setIntrinsicWidth(dp(32));
        drawable.setIntrinsicHeight(dp(28));

        return drawable;
    }

    private void finishWithAnimation() {
        finish();

        if (!reduceAnimations) {
            overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        }
    }

    static final class Contributor {
        final String username;
        final String avatarUrl;
        final int contributions;

        Contributor(
                String username,
                String avatarUrl,
                int contributions
        ) {
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.contributions = contributions;
        }
    }

    private static final class HttpResult {
        final int statusCode;
        final String body;
        final String linkHeader;

        HttpResult(
                int statusCode,
                String body,
                String linkHeader
        ) {
            this.statusCode = statusCode;
            this.body = body;
            this.linkHeader = linkHeader;
        }
    }

    private static final class BitmapResult {
        final android.graphics.Bitmap bitmap;

        BitmapResult(android.graphics.Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    private int dp(float value) {
        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f
        );
    }
}
