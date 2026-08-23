package com.rena.w4b;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Properties;

public final class RenaSettingsStore {
    private static final String FILE_NAME = "rena_settings.properties";

    private RenaSettingsStore() {
    }

    private static File file(Context context) {
        return new File(
                context.getFilesDir(),
                FILE_NAME
        );
    }

    public static boolean getBoolean(
            Context context,
            String key,
            boolean fallback
    ) {
        String value = get(
                context,
                key
        );

        return value == null
                ? fallback
                : "true".equalsIgnoreCase(value);
    }

    public static float getFloat(
            Context context,
            String key,
            float fallback
    ) {
        String value = get(
                context,
                key
        );

        if (value == null) {
            return fallback;
        }

        try {
            return Float.parseFloat(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static int getInt(
            Context context,
            String key,
            int fallback
    ) {
        String value = get(
                context,
                key
        );

        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static void putBoolean(
            Context context,
            String key,
            boolean value
    ) {
        put(
                context,
                key,
                String.valueOf(value)
        );
    }

    public static void putFloat(
            Context context,
            String key,
            float value
    ) {
        put(
                context,
                key,
                String.valueOf(value)
        );
    }

    public static void putInt(
            Context context,
            String key,
            int value
    ) {
        put(
                context,
                key,
                String.valueOf(value)
        );
    }

    private static String get(
            Context context,
            String key
    ) {
        Properties properties = readLocked(context);
        return properties.getProperty(key);
    }

    private static void put(
            Context context,
            String key,
            String value
    ) {
        File file = file(context);

        try {
            RandomAccessFile random =
                    new RandomAccessFile(
                            file,
                            "rw"
                    );

            FileChannel channel =
                    random.getChannel();

            FileChannel lockChannel = channel;
            java.nio.channels.FileLock lock =
                    lockChannel.lock();

            try {
                Properties properties =
                        new Properties();

                if (file.length() > 0) {
                    try {
                        FileInputStream input =
                                new FileInputStream(file);
                        try {
                            properties.load(input);
                        } finally {
                            input.close();
                        }
                    } catch (Throwable ignored) {
                    }
                }

                properties.setProperty(
                        key,
                        value
                );

                channel.truncate(0);
                channel.position(0);

                java.io.ByteArrayOutputStream bytes =
                        new java.io.ByteArrayOutputStream();

                properties.store(
                        bytes,
                        null
                );

                channel.write(
                        java.nio.ByteBuffer.wrap(
                                bytes.toByteArray()
                        )
                );
                channel.force(true);
            } finally {
                try {
                    lock.release();
                } catch (Throwable ignored) {
                }

                try {
                    channel.close();
                } catch (Throwable ignored) {
                }

                try {
                    random.close();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Properties readLocked(
            Context context
    ) {
        Properties properties =
                new Properties();

        File file = file(context);

        if (!file.exists()) {
            return properties;
        }

        try {
            RandomAccessFile random =
                    new RandomAccessFile(
                            file,
                            "rw"
                    );

            FileChannel channel =
                    random.getChannel();

            java.nio.channels.FileLock lock =
                    channel.lock();

            try {
                if (channel.size() > 0) {
                    byte[] data =
                            new byte[(int) Math.min(
                                    channel.size(),
                                    64 * 1024
                            )];

                    channel.position(0);

                    java.nio.ByteBuffer buffer =
                            java.nio.ByteBuffer.wrap(data);

                    while (buffer.hasRemaining() &&
                            channel.read(buffer) > 0) {
                    }

                    properties.load(
                            new java.io.ByteArrayInputStream(
                                    data
                            )
                    );
                }
            } finally {
                try {
                    lock.release();
                } catch (Throwable ignored) {
                }

                try {
                    channel.close();
                } catch (Throwable ignored) {
                }

                try {
                    random.close();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return properties;
    }
}
