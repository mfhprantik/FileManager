package com.prantik.filemanager;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    String appDirectory = "/Android/data/com.prantik.filemanager/files";
    Stack<String> locations = new Stack<>();
    ListView listView;
    LinearLayout buttonBar, selectionBar;
    Button paste;
    ArrayAdapter adapter;
    ArrayList<String> list = new ArrayList<>();
    ArrayList<String> listLocation = new ArrayList<>();
    boolean selectionMode, copied = false, cut = false;
    File directory;
    ArrayList<File> copiedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            getPermissions();

            init();
            setListView();
            loadStorages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem rename = menu.findItem(R.id.menu_rename);
        MenuItem compress = menu.findItem(R.id.menu_compress);
        MenuItem lock = menu.findItem(R.id.menu_lock);
        MenuItem backup = menu.findItem(R.id.menu_backup);
        MenuItem share = menu.findItem(R.id.menu_share);


        if (selectionMode) {
            rename.setVisible(true);
            compress.setVisible(true);
            lock.setVisible(true);
            backup.setVisible(true);
            share.setVisible(true);
        } else {
            rename.setVisible(false);
            compress.setVisible(false);
            lock.setVisible(false);
            backup.setVisible(false);
            share.setVisible(false);
        }

        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private void inflateList() {
        cleanList();

        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith(".")) continue;

            listLocation.add(files[i].getAbsolutePath());
            list.add(files[i].getName());
        }

        adapter.notifyDataSetChanged();
        setTitle(directory.getName());
    }

    private void loadStorages() {
        cleanList();

        File[] storages = ContextCompat.getExternalFilesDirs(this, null);
        for (int i = 0; i < storages.length; i++) {
            if (storages[i] == null) continue;
            String location = storages[i].getAbsolutePath().replace(appDirectory, "");
            listLocation.add(location);
            list.add("storage" + i);
        }

        File backupFolder = new File(getFilesDir() + File.separator + "backups");
        list.add("Backups");
        listLocation.add(backupFolder.getAbsolutePath());
        if (!backupFolder.exists()) backupFolder.mkdir();

        File receiveFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "Received Files");
        list.add("Receive");
        listLocation.add("receive");
        if (!receiveFolder.exists()) receiveFolder.mkdir();

        adapter.notifyDataSetChanged();
        setTitle("File Manager");
    }

    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 2);
            }

            if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 3);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 4);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 5);
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 6);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (locations.size() == 0) super.onBackPressed();
        else if (locations.peek().equals("start")) {
            locations.pop();
            loadStorages();
        } else {
            directory = new File(locations.pop());
            inflateList();
        }
    }

    private void setListView() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectionMode) {
                    listView.setItemChecked(i, listView.isItemChecked(i));
                    return;
                }

                if (listLocation.get(i).equals("receive")) {
                    Intent intent = new Intent(getApplicationContext(), ReceiveActivity.class);
                    startActivity(intent);
                    return;
                }

                File f = new File(listLocation.get(i));

                if (f.isDirectory()) {
                    directory = f;
                    inflateList();
                    if (locations.size() == 0) locations.push("start");
                    else locations.push(f.getParent());
                } else {
                    if (f.getAbsolutePath().endsWith(".zip")) {
                        decompressFiles(f);
                        return;
                    } else if (f.getAbsolutePath().endsWith(".lock")) {
                        unlockFiles(f);
                        return;
                    } else if (f.getAbsolutePath().endsWith(".backup")) {
                        restoreFiles(f);
                        return;
                    }

                    MimeTypeMap map = MimeTypeMap.getSingleton();
                    String ext = MimeTypeMap.getFileExtensionFromUrl(f.getName());
                    String type = map.getMimeTypeFromExtension(ext);

                    if (type == null)
                        type = "*/*";

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri data = Uri.fromFile(f);

                    intent.setDataAndType(data, type);

                    startActivity(intent);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (locations.size() == 0) return false;

                if (!selectionMode) {
                    selectionMode = true;
                    activateBars();
                }

                listView.setItemChecked(position, listView.isItemChecked(position) ^ true);
                return true;
            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, list);
        listView.setAdapter(adapter);
    }

    private void activateBars() {
        buttonBar.setVisibility(View.VISIBLE);
        selectionBar.setVisibility(View.VISIBLE);
    }

    private void cleanList() {
        list.clear();
        listLocation.clear();
        selectionMode = false;
        if (buttonBar.getVisibility() == View.VISIBLE && !paste.isEnabled()) buttonBar.setVisibility(View.GONE);
        if (selectionBar.getVisibility() == View.VISIBLE) selectionBar.setVisibility(View.GONE);

        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            listView.setItemChecked(checkedItemPositions.keyAt(i), false);
        }
    }

    public void cancelSelection(View view) {
        selectionMode = false;
        if (!paste.isEnabled()) buttonBar.setVisibility(View.GONE);
        selectionBar.setVisibility(View.GONE);

        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.get(checkedItemPositions.keyAt(i)))
                listView.setItemChecked(checkedItemPositions.keyAt(i), false);
        }
    }

    public void selectAll(View view) {
        for (int i = 0; i < list.size(); i++) {
            listView.setItemChecked(i, true);
        }
    }

    public void copyFiles(View view) {
        if (listView.getCheckedItemCount() == 0) return;

        copiedFiles = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.get(checkedItemPositions.keyAt(i)))
                copiedFiles.add(new File(listLocation.get(checkedItemPositions.keyAt(i))));
        }

        if (copiedFiles.size() > 0) {
            paste.setEnabled(true);
            if (!cut) copied = true;
        }

        cancelSelection(view);

        Toast.makeText(this, "Copy successful", Toast.LENGTH_SHORT).show();
    }

    public void pasteFiles(View view) {
        if (locations.size() == 0) return;

        try {
            if (copied) {
                for (int i = 0; i < copiedFiles.size(); i++) {
                    String duplicate = "";
                    String location = directory + File.separator + duplicate + copiedFiles.get(i).getName();
                    while (new File(location).exists()) {
                        duplicate += "copy";
                        location = directory + File.separator + duplicate + copiedFiles.get(i).getName();
                    }

                    if (copiedFiles.get(i).isDirectory()) {
                        FileUtils.copyDirectory(copiedFiles.get(i), new File(directory + File.separator + duplicate + copiedFiles.get(i).getName()));
                    } else
                        FileUtils.copyFile(copiedFiles.get(i), new File(directory + File.separator + duplicate + copiedFiles.get(i).getName()));
                }
            } else if (cut) {
                for (int i = 0; i < copiedFiles.size(); i++) {
                    String duplicate = "";
                    String location = directory + File.separator + duplicate + copiedFiles.get(i).getName();
                    while (new File(location).exists()) {
                        duplicate += "copy";
                        location = directory + File.separator + duplicate + copiedFiles.get(i).getName();
                    }

                    if (copiedFiles.get(i).isDirectory()) {
                        FileUtils.copyDirectory(copiedFiles.get(i), new File(directory + File.separator + duplicate + copiedFiles.get(i).getName()));
                        delete(copiedFiles.get(i));
                    } else {
                        FileUtils.copyFile(copiedFiles.get(i), new File(directory + File.separator + duplicate + copiedFiles.get(i).getName()));
                        delete(copiedFiles.get(i));
                    }
                }
            }

            inflateList();
            Toast.makeText(this, "Paste successful", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void init() {
        listView = findViewById(R.id.list);
        buttonBar = findViewById(R.id.buttonbar);
        selectionBar = findViewById(R.id.selectionBar);
        paste = findViewById(R.id.btn_paste);
    }

    public void cutFiles(View view) {
        if (listView.getCheckedItemCount() == 0) return;
        cut = true;
        copyFiles(view);
    }

    public void deleteFiles(View view) {
        if (listView.getCheckedItemCount() == 0) return;

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.delete_dialog);

        Button mDialogNo = dialog.findViewById(R.id.btn_no);
        mDialogNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button mDialogOk = dialog.findViewById(R.id.btn_yes);
        mDialogOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray checkedItemLocations = listView.getCheckedItemPositions();
                for (int i = 0; i < checkedItemLocations.size(); i++) {
                    if (checkedItemLocations.get(checkedItemLocations.keyAt(i))) {
                        File f = new File(listLocation.get(checkedItemLocations.keyAt(i)));
                        if (f.isDirectory()) delete(f);
                        else f.delete();
                    }
                }
                dialog.dismiss();
                inflateList();

                Toast.makeText(getApplicationContext(), "Delete successful", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    public static void delete(File f) {
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) delete(list[i]);
                else list[i].delete();
            }
            f.delete();
        } else f.delete();
    }

    public void renameFiles(MenuItem item) {
        if (listView.getCheckedItemCount() == 0) return;

        final int[] renamed = {0};
        final SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.get(checkedItemPositions.keyAt(i))) {
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.rename_dialog);

                final EditText name = dialog.findViewById(R.id.edit_name);
                name.setText(list.get(checkedItemPositions.keyAt(i)));

                Button cancel = dialog.findViewById(R.id.btn_cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        renamed[0]++;
                        dialog.dismiss();
                    }
                });

                Button save = dialog.findViewById(R.id.btn_save);
                final File source = new File(listLocation.get(checkedItemPositions.keyAt(i)));
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!name.getText().toString().equals("")) {
                            File destination = new File(source.getParent() + File.separator + name.getText().toString());
                            source.renameTo(destination);
                            renamed[0]++;
                            dialog.dismiss();

                            if (renamed[0] == listView.getCheckedItemCount()) {
                                inflateList();
                                Toast.makeText(getApplicationContext(), "Rename successful", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                dialog.show();
            }
        }
    }

    public void compressFiles(MenuItem item) {
        if (listView.getCheckedItemCount() == 0) return;

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.compress_dialog);

        final EditText name = dialog.findViewById(R.id.edit_name2);
        final EditText password = dialog.findViewById(R.id.edit_password);

        Button cancel = dialog.findViewById(R.id.btn_cancel2);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button compress = dialog.findViewById(R.id.btn_compress);
        compress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (name.getText().toString().length() == 0) return;

                    File temp = new File(getFilesDir() + File.separator + "temp" + File.separator + directory.getName());
                    temp.mkdirs();

                    SparseBooleanArray checkedItemLocations = listView.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemLocations.size(); i++) {
                        if (checkedItemLocations.get(checkedItemLocations.keyAt(i))) {
                            File f = new File(listLocation.get(checkedItemLocations.keyAt(i)));
                            if (f.isDirectory())
                                FileUtils.copyDirectory(f, new File(temp + File.separator + f.getName()));
                            else
                                FileUtils.copyFile(f, new File(temp + File.separator + f.getName()));
                        }
                    }

                    Toast.makeText(getApplicationContext(), "Compressing...Please wait!", Toast.LENGTH_SHORT).show();
                    Compressor.zip(temp.getAbsolutePath(), directory + File.separator + name.getText().toString(), password.getText().toString(), Toast.makeText(getApplicationContext(), "Compression successful", Toast.LENGTH_SHORT));
                    dialog.dismiss();
                    inflateList();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    public void decompressFiles(final File f) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.decompress_dialog);

        final EditText password = dialog.findViewById(R.id.edit_password2);

        Button cancel = dialog.findViewById(R.id.btn_cancel3);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button decompress = dialog.findViewById(R.id.btn_decompress);
        decompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Decompressing...Please wait!", Toast.LENGTH_SHORT).show();
                    Compressor.unzip(f.getAbsolutePath(), directory.getAbsolutePath(), password.getText().toString(), Toast.makeText(getApplicationContext(), "Decompression successful", Toast.LENGTH_SHORT));
                    dialog.dismiss();
                    inflateList();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    public void lockFiles(MenuItem item) {
        if (listView.getCheckedItemCount() == 0) return;

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.lock_dialog);

        final EditText password = dialog.findViewById(R.id.edit_password3);

        Button cancel = dialog.findViewById(R.id.btn_cancel3);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button lock = dialog.findViewById(R.id.btn_lock);
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File temp = new File(getFilesDir() + File.separator + "temp" + File.separator + directory.getName());
                    temp.mkdirs();

                    SparseBooleanArray checkedItemLocations = listView.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemLocations.size(); i++) {
                        if (checkedItemLocations.get(checkedItemLocations.keyAt(i))) {
                            File f = new File(listLocation.get(checkedItemLocations.keyAt(i)));
                            if (f.isDirectory())
                                FileUtils.copyDirectory(f, new File(temp + File.separator + f.getName()));
                            else
                                FileUtils.copyFile(f, new File(temp + File.separator + f.getName()));
                            f.delete();
                        }
                    }

                    Toast.makeText(getApplicationContext(), "Locking...Please wait!", Toast.LENGTH_SHORT).show();
                    Compressor.zip(temp.getAbsolutePath(), directory + File.separator + new RandomString(10).nextString() + ".lock", password.getText().toString(), Toast.makeText(getApplicationContext(), "Lock successful", Toast.LENGTH_SHORT));

                    dialog.dismiss();
                    inflateList();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    public void unlockFiles(final File f) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.unlock_dialog);

        final EditText password = dialog.findViewById(R.id.edit_password4);

        Button cancel = dialog.findViewById(R.id.btn_cancel4);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button unlock = dialog.findViewById(R.id.btn_unlock);
        unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Unlocking...Please wait!", Toast.LENGTH_SHORT).show();
                    Compressor.unzip(f.getAbsolutePath(), directory.getAbsolutePath(), password.getText().toString(), Toast.makeText(getApplicationContext(), "Unlock successful", Toast.LENGTH_SHORT));
                    dialog.dismiss();
                    inflateList();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    public void backupFiles(MenuItem item) {
        if (listView.getCheckedItemCount() == 0) return;

        try {
            File backupFolder = new File(getFilesDir() + File.separator + "backups" + File.separator + directory.getName());
            if (!backupFolder.exists()) backupFolder.mkdir();

            SparseBooleanArray checkedItemLocations = listView.getCheckedItemPositions();
            for (int i = 0; i < checkedItemLocations.size(); i++) {
                if (checkedItemLocations.get(checkedItemLocations.keyAt(i))) {
                    File f = new File(listLocation.get(checkedItemLocations.keyAt(i)));
                    if (f.isDirectory())
                        FileUtils.copyDirectory(f, new File(backupFolder + File.separator + f.getName()));
                    else
                        FileUtils.copyFile(f, new File(backupFolder + File.separator + f.getName()));
                }
            }

            Toast.makeText(getApplicationContext(), "Backing up...Please wait!", Toast.LENGTH_SHORT).show();
            Compressor.zip(backupFolder.getAbsolutePath(), getFilesDir() + File.separator + "backups" + File.separator + new Date().toString() + ".backup", "", Toast.makeText(getApplicationContext(), "Backup successful", Toast.LENGTH_SHORT));
            inflateList();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void restoreFiles(final File f) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.restore_dialog);

        Button no = dialog.findViewById(R.id.btn_no2);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button yes = dialog.findViewById(R.id.btn_yes2);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Restoring...Please wait!", Toast.LENGTH_SHORT).show();
                    Compressor.unzip(f.getAbsolutePath(), Environment.getExternalStorageDirectory() + File.separator + "Restored Files", "", Toast.makeText(getApplicationContext(), "Restore successful", Toast.LENGTH_SHORT));
                    dialog.dismiss();
                    inflateList();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    public void shareFiles(MenuItem item) {
        if (listView.getCheckedItemCount() == 0) return;

        try {
            File temp = new File(getFilesDir() + File.separator + "temp" + File.separator + directory.getName());
            if (!temp.exists()) temp.mkdir();

            SparseBooleanArray checkedItemLocations = listView.getCheckedItemPositions();
            for (int i = 0; i < checkedItemLocations.size(); i++) {
                if (checkedItemLocations.get(checkedItemLocations.keyAt(i))) {
                    File f = new File(listLocation.get(checkedItemLocations.keyAt(i)));
                    if (f.isDirectory())
                        FileUtils.copyDirectory(f, new File(temp + File.separator + f.getName()));
                    else FileUtils.copyFile(f, new File(temp + File.separator + f.getName()));
                }
            }

            Compressor.zip(temp.getAbsolutePath(), getFilesDir() + File.separator + "temp" + File.separator + "share.zip", "", Toast.makeText(getApplicationContext(), "Preparing files to share", Toast.LENGTH_SHORT));

            Intent intent = new Intent(getApplicationContext(), ShareActivity.class);
            intent.putExtra("location", getFilesDir() + File.separator + "temp" + File.separator + "share.zip");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void exit(MenuItem item) {
        System.exit(1);
    }
}
