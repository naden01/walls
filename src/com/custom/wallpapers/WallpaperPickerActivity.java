package com.custom.wallpapers;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperPickerActivity extends Activity {

    private static final int REQUEST_PREVIEW = 1001;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int outerPad = dp(16);
        root.setPadding(outerPad, dp(12), outerPad, dp(24));
        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        applySystemBarInsets(scrollView, outerPad, dp(12), outerPad, dp(24));
        setContentView(scrollView);

        List<WallpaperRepository.CategoryItem> categories = WallpaperRepository.loadCategories(this);
        if (categories.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_wallpapers_found);
            empty.setTextSize(16f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, dp(48));
            root.addView(empty);
            return;
        }

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int columns = dm.widthPixels >= dp(600) ? 3 : 2;
        int gap = dp(12);
        int cellWidth = Math.max(dp(120), (dm.widthPixels - outerPad * 2 - gap * (columns - 1)) / columns);
        int cellHeight = (int) (cellWidth * 1.45f);

        for (WallpaperRepository.CategoryItem category : categories) {
            TextView header = new TextView(this);
            header.setText(category.title);
            header.setTextSize(18f);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setPadding(dp(4), dp(8), dp(4), dp(10));
            root.addView(header);

            List<WallpaperRepository.WallpaperItem> items = category.wallpapers;
            for (int i = 0; i < items.size(); i += columns) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = gap;
                root.addView(row, rowLp);

                for (int c = 0; c < columns; c++) {
                    int idx = i + c;
                    if (idx < items.size()) {
                        View card = createWallpaperCard(items.get(idx), cellWidth, cellHeight);
                        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, cellHeight, 1f);
                        if (c < columns - 1) {
                            cardLp.rightMargin = gap;
                        }
                        row.addView(card, cardLp);
                    } else {
                        View spacer = new View(this);
                        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, cellHeight, 1f);
                        if (c < columns - 1) {
                            spacerLp.rightMargin = gap;
                        }
                        row.addView(spacer, spacerLp);
                    }
                }
            }
        }
    }

    private View createWallpaperCard(WallpaperRepository.WallpaperItem item, int targetW, int targetH) {
        FrameLayout card = new FrameLayout(this);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setClipToOutline(true);
        card.setClickable(true);
        card.setFocusable(true);

        ImageView thumb = new ImageView(this);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(thumb, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (!TextUtils.isEmpty(item.title)) {
            TextView label = new TextView(this);
            label.setText(item.title);
            label.setTextColor(Color.WHITE);
            label.setTextSize(13f);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setMaxLines(1);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setBackgroundColor(0x66000000);
            label.setPadding(dp(10), dp(6), dp(10), dp(8));

            FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);
            card.addView(label, labelLp);
        }

        Bitmap cached = WallpaperRepository.getCachedThumbnail(item.drawableResId);
        if (cached != null) {
            thumb.setImageBitmap(cached);
        } else {
            thumb.setTag(item.drawableResId);
            executor.execute(() -> {
                Bitmap bmp = WallpaperRepository.decodeSampledBitmap(
                        getResources(), item.drawableResId, targetW, targetH, true);
                mainHandler.post(() -> {
                    Object tag = thumb.getTag();
                    if (bmp != null && tag instanceof Integer && ((Integer) tag) == item.drawableResId) {
                        thumb.setImageBitmap(bmp);
                    }
                });
            });
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, WallpaperPreviewActivity.class);
            intent.putExtra(WallpaperPreviewActivity.EXTRA_DRAWABLE_RES, item.drawableResId);
            intent.putExtra(WallpaperPreviewActivity.EXTRA_TITLE, item.title);
            intent.putExtra(WallpaperPreviewActivity.EXTRA_SUBTITLE1, item.subtitle1);
            intent.putExtra(WallpaperPreviewActivity.EXTRA_SUBTITLE2, item.subtitle2);
            startActivityForResult(intent, REQUEST_PREVIEW);
        });

        return card;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PREVIEW && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            if (Intent.ACTION_SET_WALLPAPER.equals(getIntent().getAction())) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View target, int baseLeft, int baseTop, int baseRight, int baseBottom) {
        target.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = baseLeft;
            int top = baseTop;
            int right = baseRight;
            int bottom = baseBottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left += bars.left;
                top += bars.top;
                right += bars.right;
                bottom += bars.bottom;
            } else {
                left += insets.getSystemWindowInsetLeft();
                top += insets.getSystemWindowInsetTop();
                right += insets.getSystemWindowInsetRight();
                bottom += insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
