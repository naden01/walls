package com.custom.wallpapers;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

final class WallpaperRepository {

    private static final String TAG = "WallpaperRepository";

    static final class WallpaperItem {
        final String id;
        final int drawableResId;
        final String title;
        final String subtitle1;
        final String subtitle2;

        WallpaperItem(String id, int drawableResId, String title, String subtitle1, String subtitle2) {
            this.id = id;
            this.drawableResId = drawableResId;
            this.title = title;
            this.subtitle1 = subtitle1;
            this.subtitle2 = subtitle2;
        }
    }

    static final class CategoryItem {
        final String id;
        final String title;
        final List<WallpaperItem> wallpapers;

        CategoryItem(String id, String title, List<WallpaperItem> wallpapers) {
            this.id = id;
            this.title = title;
            this.wallpapers = wallpapers;
        }
    }

    private static final int CACHE_KB = (int) Math.min(32768L, Runtime.getRuntime().maxMemory() / 1024L / 8L);
    private static final LruCache<Integer, Bitmap> THUMB_CACHE = new LruCache<Integer, Bitmap>(Math.max(4096, CACHE_KB)) {
        @Override
        protected int sizeOf(Integer key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private WallpaperRepository() {}

    static List<CategoryItem> loadCategories(Context context) {
        List<CategoryItem> categories = new ArrayList<>();
        Resources res = context.getResources();
        try (XmlResourceParser parser = res.getXml(R.xml.wallpapers)) {
            String currentCategoryId = null;
            String currentCategoryTitle = null;
            List<WallpaperItem> currentWallpapers = null;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("category".equals(tag)) {
                        currentCategoryId = parser.getAttributeValue(null, "id");
                        currentCategoryTitle = readStringAttr(res, parser, "title");
                        if (TextUtils.isEmpty(currentCategoryTitle)) {
                            currentCategoryTitle = TextUtils.isEmpty(currentCategoryId)
                                    ? context.getString(R.string.app_name)
                                    : currentCategoryId;
                        }
                        currentWallpapers = new ArrayList<>();
                    } else if ("static-wallpaper".equals(tag) && currentWallpapers != null) {
                        String id = parser.getAttributeValue(null, "id");
                        int srcResId = parser.getAttributeResourceValue(null, "src", 0);
                        String title = readStringAttr(res, parser, "title");
                        if (TextUtils.isEmpty(title)) {
                            title = id;
                        }
                        String subtitle1 = readStringAttr(res, parser, "subtitle1");
                        String subtitle2 = readStringAttr(res, parser, "subtitle2");
                        if (srcResId != 0) {
                            currentWallpapers.add(new WallpaperItem(id, srcResId, title, subtitle1, subtitle2));
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "category".equals(parser.getName())) {
                    if (currentWallpapers != null && !currentWallpapers.isEmpty()) {
                        categories.add(new CategoryItem(currentCategoryId, currentCategoryTitle, currentWallpapers));
                    }
                    currentCategoryId = null;
                    currentCategoryTitle = null;
                    currentWallpapers = null;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse res/xml/wallpapers.xml", e);
        }
        return categories;
    }

    private static String readStringAttr(Resources res, XmlResourceParser parser, String attrName) {
        int resId = parser.getAttributeResourceValue(null, attrName, 0);
        if (resId != 0) {
            try {
                return res.getString(resId);
            } catch (Resources.NotFoundException ignored) {
                return null;
            }
        }
        return parser.getAttributeValue(null, attrName);
    }

    static Bitmap getCachedThumbnail(int drawableResId) {
        return THUMB_CACHE.get(drawableResId);
    }

    static Bitmap decodeSampledBitmap(Resources res, int drawableResId, int reqWidth, int reqHeight, boolean cache) {
        if (cache) {
            Bitmap cached = THUMB_CACHE.get(drawableResId);
            if (cached != null) {
                return cached;
            }
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, drawableResId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        Bitmap decoded = BitmapFactory.decodeResource(res, drawableResId, options);
        if (cache && decoded != null) {
            THUMB_CACHE.put(drawableResId, decoded);
        }
        return decoded;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
