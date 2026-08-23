package com.rena.w4b;

import android.content.Context;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class TabStore {
    public static final class Record {
        public int id;
        public String url;
        public String profileId;

        public Record(
                int id,
                String url,
                String profileId
        ) {
            this.id = id;
            this.url = url;
            this.profileId = profileId;
        }
    }

    public static final class Snapshot {
        public final ArrayList<Record> records;
        public final int activeTabId;

        Snapshot(
                ArrayList<Record> records,
                int activeTabId
        ) {
            this.records = records;
            this.activeTabId = activeTabId;
        }
    }

    private static final String FILE_NAME =
            "rena_tabs.json";

    private TabStore() {
    }

    private static File file(Context context) {
        return new File(
                context.getFilesDir(),
                FILE_NAME
        );
    }

    public static Snapshot read(
            Context context
    ) {
        File file = file(context);
        ArrayList<Record> records =
                new ArrayList<Record>();
        int activeId = 1;

        if (!file.exists()) {
            records.add(
                    new Record(
                            1,
                            NativeConfig.webUrl(),
                            "tab_profile_1"
                    )
            );
            return new Snapshot(
                    records,
                    1
            );
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
                long size = channel.size();

                if (size > 0 &&
                        size <= 512 * 1024) {
                    byte[] data =
                            new byte[(int) size];

                    channel.position(0);

                    ByteBuffer buffer =
                            ByteBuffer.wrap(data);

                    while (buffer.hasRemaining() &&
                            channel.read(buffer) > 0) {
                    }

                    String json =
                            new String(
                                    data,
                                    "UTF-8"
                            );

                    JSONObject object =
                            new JSONObject(json);

                    activeId =
                            object.optInt(
                                    "activeTabId",
                                    1
                            );

                    JSONArray array =
                            object.optJSONArray(
                                    "tabs"
                            );

                    if (array != null) {
                        for (int i = 0;
                                i < array.length();
                                i++) {
                            JSONObject item =
                                    array.optJSONObject(i);

                            if (item == null) {
                                continue;
                            }

                            int id =
                                    item.optInt(
                                            "id",
                                            -1
                                    );

                            if (id < 1 || id > 10) {
                                continue;
                            }

                            String url =
                                    item.optString(
                                            "url",
                                            NativeConfig.webUrl()
                                    );

                            String profile =
                                    item.optString(
                                            "profileId",
                                            "tab_profile_" + id
                                    );

                            records.add(
                                    new Record(
                                            id,
                                            url,
                                            profile
                                    )
                            );
                        }
                    }
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

        normalizeRecords(records);

        if (records.isEmpty()) {
            records.add(
                    new Record(
                            1,
                            NativeConfig.webUrl(),
                            "tab_profile_1"
                    )
            );
            activeId = 1;
        }

        boolean activeExists = false;
        for (Record record : records) {
            if (record.id == activeId) {
                activeExists = true;
                break;
            }
        }

        if (!activeExists) {
            activeId = 1;
        }

        return new Snapshot(
                records,
                activeId
        );
    }

    public static void write(
            Context context,
            ArrayList<Record> records,
            int activeId
    ) {
        ArrayList<Record> copy =
                new ArrayList<Record>();

        if (records != null) {
            copy.addAll(records);
        }

        normalizeRecords(copy);

        if (copy.isEmpty()) {
            copy.add(
                    new Record(
                            1,
                            NativeConfig.webUrl(),
                            "tab_profile_1"
                    )
            );
            activeId = 1;
        }

        boolean activeExists = false;
        for (Record record : copy) {
            if (record.id == activeId) {
                activeExists = true;
                break;
            }
        }

        if (!activeExists) {
            activeId = 1;
        }

        JSONObject object =
                new JSONObject();

        JSONArray array =
                new JSONArray();

        try {
            object.put(
                    "activeTabId",
                    activeId
            );

            for (Record record : copy) {
                JSONObject item =
                        new JSONObject();

                item.put(
                        "id",
                        record.id
                );
                item.put(
                        "url",
                        record.url == null
                                ? NativeConfig.webUrl()
                                : record.url
                );
                item.put(
                        "profileId",
                        record.profileId == null
                                ? "tab_profile_" + record.id
                                : record.profileId
                );

                array.put(item);
            }

            object.put(
                    "tabs",
                    array
            );

            byte[] data =
                    object.toString()
                            .getBytes("UTF-8");

            File file = file(context);

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
                channel.truncate(0);
                channel.position(0);
                channel.write(
                        ByteBuffer.wrap(data)
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

    private static void normalizeRecords(
            ArrayList<Record> records
    ) {
        for (int i = records.size() - 1; i >= 0; i--) {
            Record record = records.get(i);

            if (record == null ||
                    record.id < 1 ||
                    record.id > 10) {
                records.remove(i);
            }
        }

        for (int i = records.size() - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (records.get(i).id ==
                        records.get(j).id) {
                    records.remove(i);
                    break;
                }
            }
        }

        boolean hasTab1 = false;

        for (Record record : records) {
            if (record.id == 1) {
                hasTab1 = true;
                break;
            }
        }

        if (!hasTab1) {
            records.add(
                    0,
                    new Record(
                            1,
                            NativeConfig.webUrl(),
                            "tab_profile_1"
                    )
            );
        }

        java.util.Collections.sort(
                records,
                new java.util.Comparator<Record>() {
                    @Override
                    public int compare(
                            Record left,
                            Record right
                    ) {
                        return left.id - right.id;
                    }
                }
        );
    }
}
