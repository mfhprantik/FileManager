package com.prantik.filemanager;

import android.widget.Toast;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;

public class Compressor {
    public static void zip(final String targetPath, final String destinationFilePath, final String password, final Toast toast) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZipParameters parameters = new ZipParameters();
                    parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                    parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

                    if (password.length() > 0) {
                        parameters.setEncryptFiles(true);
                        parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
                        parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
                        parameters.setPassword(password);
                    }

                    ZipFile zipFile = new ZipFile(destinationFilePath);

                    File targetFile = new File(targetPath);
                    if (targetFile.isFile()) {
                        zipFile.addFile(targetFile, parameters);
                    } else if (targetFile.isDirectory()) {
                        zipFile.addFolder(targetFile, parameters);
                    }

                    toast.show();
                    MainActivity.delete(new File(targetPath));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        while(t.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void unzip(final String targetZipFilePath, final String destinationFolderPath, final String password, final Toast toast) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZipFile zipFile = new ZipFile(targetZipFilePath);
                    if (zipFile.isEncrypted()) {
                        zipFile.setPassword(password);
                    }
                    zipFile.extractAll(destinationFolderPath);

                    toast.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        while(t.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}