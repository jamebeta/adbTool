package utils;

import status.AllKt;

import java.io.*;
import java.util.Objects;

public class FileUtil {
    public static void copyFileUsingFileStreams(InputStream source, File dest)
            throws IOException {
        try (source; OutputStream output = new FileOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = source.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        }
    }

    public static void initFile() throws IOException {
        File cfgParent = new File(BashUtil.workDir, "cfg");
        if (!cfgParent.exists()) {
            cfgParent.mkdirs();
        }

        //adb环境
        if (Objects.equals(BashUtil.split, "\\")) {
            File adb1 = new File(cfgParent, "adb.exe");
            if (!adb1.exists()) {

                LogUtil.Companion.d("create windows adb.exe");
            }
            new Thread(()->{
                InputStream inputStream = ClassLoader.getSystemResourceAsStream("adb.exe");
                if (inputStream != null) {
                    try {
                        FileUtil.copyFileUsingFileStreams(inputStream, adb1);
                    } catch (IOException e) {
                        LogUtil.Companion.d(e.getMessage());
                    }
                }
                AllKt.getAdb().setValue(adb1.getAbsolutePath());
                PropertiesUtil.Companion.setValue("adb", AllKt.getAdb().getValue(), "");
            }).start();
        } else {
            File adb1 = new File(cfgParent, "adb");
            if (!adb1.exists()) {
                new Thread(()->{
                    InputStream inputStream = ClassLoader.getSystemResourceAsStream("adb");
                    if (inputStream != null) {
                        try {
                            FileUtil.copyFileUsingFileStreams(inputStream, adb1);
                        } catch (IOException e) {
                            LogUtil.Companion.d(e.getMessage());
                        }
                    }
                    adb1.setExecutable(true);
                    AllKt.getAdb().setValue(adb1.getAbsolutePath());
                    PropertiesUtil.Companion.setValue("adb", AllKt.getAdb().getValue(), "");
                    LogUtil.Companion.d("create mac adb");
                }).start();
            }
        }
    }
}
