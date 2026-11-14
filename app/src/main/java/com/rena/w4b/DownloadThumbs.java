package com.rena.w4b;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DownloadThumbs {
    public interface Callback {
        void onLoaded(String key, Bitmap bitmap, String extra);
    }

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final LruCache<String, String> EXTRA = new LruCache<String, String>(200);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DownloadThumbs() {
    }

    public static String keyFor(RenaDownloadManager.DownloadItem item) {
        File file = new File(item.path);
        return item.path + "|" + file.lastModified() + "|" + file.length();
    }

    public static Bitmap cached(String key) {
        return CACHE.get(key);
    }

    public static String cachedExtra(String key) {
        return EXTRA.get(key);
    }

    public static boolean wantsPreview(RenaDownloadManager.DownloadItem item) {
        if (item.status != RenaDownloadManager.COMPLETED) {
            return false;
        }
        String ext = DownloadNames.extensionOf(item.fileName);
        return RenaPaths.IMAGES.equals(item.folder) || RenaPaths.VIDEOS.equals(item.folder) ||
                "apk".equals(ext) || "pdf".equals(ext) || RenaPaths.AUDIO.equals(item.folder) || RenaPaths.VOICE.equals(item.folder);
    }

    public static void load(final Context context, final RenaDownloadManager.DownloadItem item, final int sizePx, final Callback callback) {
        final String key = keyFor(item);
        final String path = item.path;
        final String name = item.fileName;
        final String folder = item.folder;
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                String extra = null;
                try {
                    String ext = DownloadNames.extensionOf(name);
                    if (RenaPaths.IMAGES.equals(folder) || "jpg".equals(ext) || "png".equals(ext) || "webp".equals(ext) || "gif".equals(ext)) {
                        bitmap = decodeImage(path, sizePx);
                    } else if (RenaPaths.VIDEOS.equals(folder)) {
                        bitmap = videoFrame(path, sizePx);
                    } else if ("apk".equals(ext)) {
                        Object[] result = apkIcon(context, path, sizePx);
                        bitmap = (Bitmap) result[0];
                        extra = (String) result[1];
                    } else if ("pdf".equals(ext)) {
                        bitmap = pdfPage(path, sizePx);
                    } else if (RenaPaths.AUDIO.equals(folder) || RenaPaths.VOICE.equals(folder)) {
                        bitmap = albumArt(path, sizePx);
                    }
                } catch (Throwable ignored) {
                }
                final Bitmap result = bitmap;
                final String resultExtra = extra;
                if (result != null) {
                    CACHE.put(key, result);
                }
                if (resultExtra != null) {
                    EXTRA.put(key, resultExtra);
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoaded(key, result, resultExtra);
                    }
                });
            }
        });
    }

    private static Bitmap decodeImage(String path, int target) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap decoded = BitmapFactory.decodeFile(path, options);
        return centerCrop(decoded, target);
    }

    private static Bitmap videoFrame(String path, int target) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            Bitmap frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) {
                frame = retriever.getFrameAtTime(0L);
            }
            return centerCrop(frame, target);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Bitmap albumArt(String path, int target) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            if (art == null) {
                return null;
            }
            return centerCrop(BitmapFactory.decodeByteArray(art, 0, art.length), target);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Object[] apkIcon(Context context, String path, int target) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(path, 0);
            if (info == null || info.applicationInfo == null) {
                return new Object[]{null, null};
            }
            ApplicationInfo application = info.applicationInfo;
            application.sourceDir = path;
            application.publicSourceDir = path;
            Drawable icon = application.loadIcon(pm);
            CharSequence label = application.loadLabel(pm);
            String extra = (label == null ? "" : label.toString()) + (info.versionName == null ? "" : " \u00B7 v" + info.versionName);
            Bitmap bitmap = icon == null ? null : Ui.drawableToBitmap(icon, target, target);
            return new Object[]{bitmap, extra};
        } catch (Throwable ignored) {
            return new Object[]{null, null};
        }
    }

    private static Bitmap pdfPage(String path, int target) {
        ParcelFileDescriptor descriptor = null;
        PdfRenderer renderer = null;
        PdfRenderer.Page page = null;
        try {
            descriptor = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            if (renderer.getPageCount() == 0) {
                return null;
            }
            page = renderer.openPage(0);
            float scale = (float) target * 1.6f / Math.max(page.getWidth(), page.getHeight());
            int width = Math.max(1, (int) (page.getWidth() * scale));
            int height = Math.max(1, (int) (page.getHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return centerCrop(bitmap, target);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                if (page != null) {
                    page.close();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (renderer != null) {
                    renderer.close();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (descriptor != null) {
                    descriptor.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Bitmap centerCrop(Bitmap source, int target) {
        if (source == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        float scale = Math.max((float) target / width, (float) target / height);
        int scaledWidth = Math.max(target, Math.round(width * scale));
        int scaledHeight = Math.max(target, Math.round(height * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
        int x = Math.max(0, (scaledWidth - target) / 2);
        int y = Math.max(0, (scaledHeight - target) / 2);
        Bitmap cropped = Bitmap.createBitmap(scaled, x, y, Math.min(target, scaledWidth - x), Math.min(target, scaledHeight - y));
        if (scaled != source && scaled != cropped) {
            scaled.recycle();
        }
        return cropped;
    }

    public static Drawable badge(Context context, String fileName, String folder) {
        return new Badge(context, fileName, folder);
    }

    private static int colorFor(String ext, String folder) {
        if ("pdf".equals(ext)) {
            return Color.rgb(229, 57, 53);
        }
        if ("doc".equals(ext) || "docx".equals(ext) || "rtf".equals(ext) || "odt".equals(ext)) {
            return Color.rgb(43, 108, 176);
        }
        if ("xls".equals(ext) || "xlsx".equals(ext) || "csv".equals(ext) || "ods".equals(ext)) {
            return Color.rgb(46, 125, 50);
        }
        if ("ppt".equals(ext) || "pptx".equals(ext) || "odp".equals(ext)) {
            return Color.rgb(230, 81, 0);
        }
        if ("zip".equals(ext) || "rar".equals(ext) || "7z".equals(ext) || "tar".equals(ext) || "gz".equals(ext)) {
            return Color.rgb(141, 110, 99);
        }
        if ("apk".equals(ext) || "apks".equals(ext) || "xapk".equals(ext) || "apkm".equals(ext)) {
            return Color.rgb(61, 168, 107);
        }
        if (RenaPaths.AUDIO.equals(folder) || RenaPaths.VOICE.equals(folder)) {
            return Color.rgb(142, 36, 170);
        }
        if (RenaPaths.VIDEOS.equals(folder)) {
            return Color.rgb(57, 73, 171);
        }
        if (RenaPaths.IMAGES.equals(folder)) {
            return Color.rgb(0, 137, 123);
        }
        if ("txt".equals(ext) || "md".equals(ext) || "json".equals(ext) || "xml".equals(ext) || "html".equals(ext)) {
            return Color.rgb(96, 125, 139);
        }
        return Color.rgb(84, 110, 122);
    }

    private static final class Badge extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String label;
        private final String folder;
        private final float density;

        Badge(Context context, String fileName, String folder) {
            String ext = DownloadNames.extensionOf(fileName);
            this.folder = folder;
            this.label = ext.length() == 0 ? "FILE" : ext.toUpperCase(Locale.US);
            density = context.getResources().getDisplayMetrics().density;
            fill.setColor(colorFor(ext, folder));
            text.setColor(Color.WHITE);
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
            glyph.setColor(Color.argb(70, 255, 255, 255));
            glyph.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            fill.setAlpha(255);
            canvas.drawRoundRect(bounds, 14f * density, 14f * density, fill);
            float width = bounds.width();
            if (RenaPaths.AUDIO.equals(folder) || RenaPaths.VOICE.equals(folder)) {
                canvas.drawCircle(bounds.centerX() - width * 0.12f, bounds.centerY() + width * 0.13f, width * 0.1f, glyph);
                canvas.drawRect(bounds.centerX() - width * 0.04f, bounds.centerY() - width * 0.22f,
                        bounds.centerX() - width * 0.0f, bounds.centerY() + width * 0.13f, glyph);
            } else if (RenaPaths.VIDEOS.equals(folder)) {
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(bounds.centerX() - width * 0.1f, bounds.centerY() - width * 0.18f);
                path.lineTo(bounds.centerX() + width * 0.18f, bounds.centerY());
                path.lineTo(bounds.centerX() - width * 0.1f, bounds.centerY() + width * 0.18f);
                path.close();
                canvas.drawPath(path, glyph);
            }
            float size = label.length() <= 3 ? width * 0.3f : width * 0.24f;
            text.setTextSize(size);
            float y = bounds.centerY() - (text.descent() + text.ascent()) / 2f + width * 0.05f;
            canvas.drawText(label.length() > 4 ? label.substring(0, 4) : label, bounds.centerX(), y, text);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
