package com.custom.wallpapers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperPreviewActivity extends Activity {

    static final String EXTRA_DRAWABLE_RES = "drawable_res";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_SUBTITLE1 = "subtitle1";
    static final String EXTRA_SUBTITLE2 = "subtitle2";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int drawableResId;
    private boolean centerCrop = true;
    private boolean applying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drawableResId = getIntent().getIntExtra(EXTRA_DRAWABLE_RES, 0);
        if (drawableResId == 0) {
            finish();
            return;
        }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String subtitle1 = getIntent().getStringExtra(EXTRA_SUBTITLE1);
        String subtitle2 = getIntent().getStringExtra(EXTRA_SUBTITLE2);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        preview.setOnClickListener(v -> {
            centerCrop = !centerCrop;
            preview.setScaleType(centerCrop ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        });

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setBackgroundColor(0x99000000);
        int pad = dp(16);
        bottomBar.setPadding(pad, dp(14), pad, pad);

        if (!TextUtils.isEmpty(title)) {
            TextView titleView = new TextView(this);
            titleView.setText(title);
            titleView.setTextColor(Color.WHITE);
            titleView.setTextSize(18f);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            bottomBar.addView(titleView);
        }

        addSubtitleIfPresent(bottomBar, subtitle1);
        addSubtitleIfPresent(bottomBar, subtitle2);

        Button applyButton = new Button(this);
        applyButton.setText(R.string.apply_wallpaper);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(10);
        bottomBar.addView(applyButton, btnLp);

        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(bottomBar, barLp);
        applyBottomInsets(bottomBar, pad);

        setContentView(root);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        executor.execute(() -> {
            Bitmap bmp = WallpaperRepository.decodeSampledBitmap(
                    getResources(), drawableResId, dm.widthPixels, dm.heightPixels, false);
            mainHandler.post(() -> {
                if (!isFinishing() && bmp != null) {
                    preview.setImageBitmap(bmp);
                }
            });
        });

        applyButton.setOnClickListener(v -> showTargetDialog(applyButton));
    }

    @SuppressWarnings("deprecation")
    private void applyBottomInsets(View bottomBar, int pad) {
        bottomBar.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset;
            int leftInset;
            int rightInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                bottomInset = bars.bottom;
                leftInset = bars.left;
                rightInset = bars.right;
            } else {
                bottomInset = insets.getSystemWindowInsetBottom();
                leftInset = insets.getSystemWindowInsetLeft();
                rightInset = insets.getSystemWindowInsetRight();
            }
            v.setPadding(pad + leftInset, dp(14), pad + rightInset, pad + bottomInset);
            return insets;
        });
    }

    private void addSubtitleIfPresent(LinearLayout container, String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFFD0D0D0);
        view.setTextSize(13f);
        view.setPadding(0, dp(2), 0, 0);
        container.addView(view);
    }

    private void showTargetDialog(Button applyButton) {
        if (applying) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
                getString(R.string.set_home_screen),
                getString(R.string.set_lock_screen),
                getString(R.string.set_both_screens)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.set_wallpaper_target)
                .setItems(options, (dialog, which) -> {
                    int flags;
                    if (which == 0) {
                        flags = WallpaperManager.FLAG_SYSTEM;
                    } else if (which == 1) {
                        flags = WallpaperManager.FLAG_LOCK;
                    } else {
                        flags = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                    }
                    applyWallpaper(flags, applyButton);
                })
                .show();
    }

    private void applyWallpaper(int flags, Button applyButton) {
        applying = true;
        applyButton.setEnabled(false);
        applyButton.setText(R.string.applying_wallpaper);

        executor.execute(() -> {
            boolean ok = false;
            try (InputStream in = getResources().openRawResource(drawableResId)) {
                WallpaperManager.getInstance(this).setStream(in, null, true, flags);
                ok = true;
            } catch (Exception ignored) {
                ok = false;
            }

            final boolean success = ok;
            mainHandler.post(() -> {
                applying = false;
                if (isFinishing()) {
                    return;
                }
                if (success) {
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    applyButton.setEnabled(true);
                    applyButton.setText(R.string.apply_wallpaper);
                    Toast.makeText(this, R.string.wallpaper_apply_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
