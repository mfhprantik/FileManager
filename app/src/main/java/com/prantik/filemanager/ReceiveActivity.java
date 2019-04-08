package com.prantik.filemanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveActivity extends AppCompatActivity {

    Button create;

    final int PORT = 6416;
    final int BUFFER_SIZE = (int) Math.pow(2, 20);
    ServerClass sc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        setTitle("Receive");

        create = findViewById(R.id.btn_start);
    }

    public void cancelReceive(View view) {
        stopHotspot();
        sc = null;
        onBackPressed();
    }

    public void start(View view) {
        create.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Creating hotspot...", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                createHotspot();
                Toast.makeText(getApplicationContext(), "Waiting for a device to connect...", Toast.LENGTH_SHORT).show();

                sc = new ServerClass();
                sc.start();
            }
        } else {
            createHotspot();
            Toast.makeText(getApplicationContext(), "Waiting for a device to connect...", Toast.LENGTH_SHORT).show();

            sc = new ServerClass();
            sc.start();
        }
    }

    private void createHotspot() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("setWifiApEnabled")) {
                WifiConfiguration netConfig = new WifiConfiguration();
                netConfig.SSID = "fm_" + android.os.Build.MODEL;
                netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

                try {
                    boolean apstatus = (Boolean) method.invoke(wifiManager, netConfig, true);
                    for (Method isWifiApEnabledmethod : wmMethods) {
                        if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")) {
                            while (!(Boolean) isWifiApEnabledmethod.invoke(wifiManager)) {
                            }
                            ;
                            for (Method method1 : wmMethods) {
                                if (method1.getName().equals("getWifiApState")) {
                                    int apstate;
                                    apstate = (Integer) method1.invoke(wifiManager);
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.getCause().printStackTrace();
                }
            }
        }
    }

    public class ServerClass extends Thread {
        BufferedInputStream inputStream;
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            boolean flag = true;
            while (flag) {
                try {
                    serverSocket = new ServerSocket(PORT);
                    socket = serverSocket.accept();
                    flag = false;
                    inputStream = new BufferedInputStream(socket.getInputStream());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connected! Receiving file...", Toast.LENGTH_SHORT).show();
                        }
                    });

                    File saveLocation = new File(getFilesDir() + File.separator + "temp");
                    if (!saveLocation.exists()) saveLocation.mkdir();

                    FileOutputStream fos = new FileOutputStream(saveLocation + File.separator + "receive.zip");
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    final long startTime = System.nanoTime();

                    byte [] buffer = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = inputStream.read(buffer)) > 0) {
                        bos.write(buffer, 0, count);
                        bos.flush();
                    }

                    final long endTime = System.nanoTime();

                    fos.close();
                    bos.close();

                    inputStream.close();
                    serverSocket.close();
                    socket.close();

                    final long fileSize = new File(saveLocation + File.separator + "receive.zip").length();
                    Compressor.unzip(saveLocation + File.separator + "receive.zip", Environment.getExternalStorageDirectory() + File.separator + "Received Files", "", Toast.makeText(getApplicationContext(), "File receive successful", Toast.LENGTH_SHORT));
                    saveLocation.delete();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double time = (endTime - startTime) / 1000000000.0;
                            double speed = (fileSize / 1000) / time;
                            Toast.makeText(getApplicationContext(), "File received successfully! Took " + (int) time + "s at " + (int) speed + "kbps", Toast.LENGTH_SHORT).show();
                            cancelReceive(null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopHotspot() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        create.setEnabled(true);
    }
}
