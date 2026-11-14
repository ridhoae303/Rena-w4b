package com.ridhoae303.expert;

import android.content.Context;
import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class CrashLog {
    public static final Thread.UncaughtExceptionHandler DEFAULT_UNCAUGHT_EXCEPTION_HANDLER =
            Thread.getDefaultUncaughtExceptionHandler();

    private static volatile boolean installed;

    public CrashLog() {
        super();
    }

    public static synchronized void Rena(Context context, boolean ignoredFlag) {
        if (context != null && !installed) {
            installed = true;

            Context applicationContext = context.getApplicationContext();
            final Context app = applicationContext != null ? applicationContext : context;

            Thread.setDefaultUncaughtExceptionHandler(
                    new Thread.UncaughtExceptionHandler() {
                        private void writeCrashFile(String stackTrace) {
                            try {
                                File directory = app.getExternalFilesDir("Documents");
                                if (directory == null) {
                                    File filesDirectory = app.getFilesDir();
                                    directory = new File(filesDirectory, "Documents");
                                }

                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }

                                long timestamp = System.currentTimeMillis();
                                String fileName = "crash_" + timestamp + ".txt";
                                File crashFile = new File(directory, fileName);
                                FileOutputStream outputStream = new FileOutputStream(crashFile);
                                byte[] bytes = stackTrace.getBytes("UTF-8");
                                outputStream.write(bytes);
                                outputStream.flush();
                                outputStream.close();
                            } catch (Throwable ignored) {
                            }
                        }

                        @Override
                        public void uncaughtException(Thread thread, Throwable throwable) {
                            try {
                                StringWriter stringWriter = new StringWriter();
                                PrintWriter printWriter = new PrintWriter(stringWriter);
                                throwable.printStackTrace(printWriter);
                                printWriter.flush();
                                String stackTrace = stringWriter.toString();
                                printWriter.close();

                                writeCrashFile(stackTrace);

                                AppFragment.Reporter reporter = new AppFragment.Reporter(
                                        app,
                                        "UNCAUGHT_EXCEPTION",
                                        "CRASH",
                                        stackTrace
                                );
                                reporter.run();
                            } catch (Throwable ignored) {
                            }

                            Thread.UncaughtExceptionHandler defaultHandler =
                                    DEFAULT_UNCAUGHT_EXCEPTION_HANDLER;
                            if (defaultHandler != null) {
                                defaultHandler.uncaughtException(thread, throwable);
                                return;
                            }

                            int pid = Process.myPid();
                            Process.killProcess(pid);
                            System.exit(2);
                        }
                    }
            );
        }
    }
}
