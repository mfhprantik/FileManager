package com.prantik.filemanager;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends Activity {

    ListView hotspots;
    Button scan;

    ArrayAdapter adapter;
    ClientClass cc;

    ArrayList<String> ssids;
    String location;
    final String HOST = "192.168.43.1";
    final int PORT = 6416;
    final int BUFFER_SIZE = (int) Math.pow(2, 20);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        setTitle("Share");

        init();
    }

    private void init() {
        hotspots = findViewById(R.id.list_hotspots);
        scan = findViewById(R.id.btn_scan);

        ssids = new ArrayList<>();

        Bundle b = getIntent().getExtras();
        location = b.getString("location");

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, ssids);
        hotspots.setAdapter(adapter);

        hotspots.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + "fm_" + ssids.get(position) + "\"";
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.addNetwork(conf);

                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration i : list) {
                    if (i.SSID != null && i.SSID.equals("\"" + "fm_" + ssids.get(position) + "\"")) {
                        try {
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(i.networkId, true);
                            wifiManager.reconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                cc = new ClientClass();
                cc.start();
            }
        });
    }

    public void cancelShare(View view) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        cc = null;
        onBackPressed();
    }

    public void scan(View view) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ssids.clear();

        if (wifiManager.isWifiEnabled()) {
            List<ScanResult> list = wifiManager.getScanResults();
            for (ScanResult i : list) {
                if (i.SSID != null && i.SSID.startsWith("fm_")) {
                    ssids.add(i.SSID.replace("fm_", ""));
                }
            }

            adapter.notifyDataSetChanged();
        } else wifiManager.setWifiEnabled(true);
    }

    public class ClientClass extends Thread {
        Socket socket;
        BufferedOutputStream outputStream;

        @Override
        public void run() {
            boolean flag = true;
            while (flag) {
                try {
                    socket = new Socket(HOST, PORT);
                    flag = false;
                    outputStream = new BufferedOutputStream(socket.getOutputStream());

                    FileInputStream fis = new FileInputStream(location);
                    BufferedInputStream bis = new BufferedInputStream(fis);

                    byte [] buffer = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = bis.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, count);
                        outputStream.flush();
                    }

                    bis.close();
                    fis.close();

                    socket.shutdownOutput();
                    new File(location).delete();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "File shared successfully", Toast.LENGTH_SHORT).show();
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
}
