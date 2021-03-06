package com.example.fiteditapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.garmin.fit.csv.CSVTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_EX_STORAGE = 10884;
    AddEditCSV addEditCSV;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler(Looper.getMainLooper());

        // https://blog.goo.ne.jp/marunomarunogoo/e/943906fc518b7c0c767c9804efb297e1
        TextView newOut = findViewById(R.id.textView);
        System.setOut(new TextViewPrintStream(System.out, newOut));
        System.setErr(new TextViewPrintStream(System.out, newOut));

        findViewById(R.id.floatingActionButton).setOnClickListener(view -> openFile());

        findViewById(R.id.addEditCSVButton).setOnClickListener(view -> {
            addEditCSV = new AddEditCSV(this);
            addEditCSV.add();
        });

        findViewById(R.id.csvToGPXButton).setOnClickListener(view -> csv2gpx());

        // permission check
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
            || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            System.out.println("Permission denied!");
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },PERMISSION_EX_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // https://docs.kii.com/ja/guides/cloudsdk/android/quickstart/install-sdk/activity-implementation-note/
        if (requestCode == PERMISSION_EX_STORAGE)
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "????????????????????????????????????", Toast.LENGTH_LONG).show();
            }
    }

    private static final int READ_FIT_FILE = 20884;
    private void openFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); //????????????????????????

        startActivityForResult(intent, READ_FIT_FILE);
    }

    private static final int ADD_EDIT_CSV = 30884;
    public class AddEditCSV {
        private final ArrayList<Uri> uriArrayList;
        private final Context context;
        private double lat,lon;
        private double homeArea;
        private int shrink;
        private String outputFilename;

        private final Thread editThread = new Thread(()-> {
            try {
                addEditCSV.edit();
            } catch (IOException e) {
                handler.post(e::printStackTrace);
            }
        });

        public AddEditCSV(Context c) {
            this.context = c;
            this.uriArrayList = new ArrayList<>();
            this.outputFilename = "new.csv";
        }

        public void add(){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");

            startActivityForResult(intent,ADD_EDIT_CSV);
        }

        public void appendToList(Uri uri){
            uriArrayList.add(uri);
            setOutputFilename();
        }

        public void configAndEdit(){
            EditText et = new EditText(context);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setText(R.string.etLatLon); //35.6851,139.7527
            EditText et1 = new EditText(context);
            et1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et1.setText(R.string.et1dist); //0
            EditText et2 = new EditText(context);
            et2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            et2.setText(R.string.et2shrink); //30
            EditText et3 = new EditText(context);
            et3.setInputType(InputType.TYPE_CLASS_TEXT);
            et3.setText(outputFilename);

            new AlertDialog.Builder(context).setTitle("???????????????")
                    .setMessage("latitude,longitude")
                    .setView(et)
                    .setPositiveButton("?????????",(dialogInterface, i) -> {
                        String[] input = et.getText().toString().split(",");
                        lat = Double.parseDouble(input[0]);
                        lon = Double.parseDouble(input[1]);

            new AlertDialog.Builder(context).setTitle("???????????????(km)")
                    .setView(et1)
                    .setPositiveButton("??????", (d1,i1) -> {
                        homeArea = Double.parseDouble( et1.getText().toString() );

            new AlertDialog.Builder(context).setTitle("????????????(sec)")
                    .setView(et2)
                    .setPositiveButton("OK",(d2, i2) -> {
                        shrink = Integer.parseInt( et2.getText().toString() );

            new AlertDialog.Builder(context).setTitle("???????????????")
                    .setView(et3)
                    .setPositiveButton("OK",(d3,i3)-> {
                        String name = et3.getText().toString();
                        if(!name.isEmpty()){
                            outputFilename = name;
                        }
                        editThread.start();
                    })
                    .show();
            }).show();
            }).show();
            }).show();
        }

        private void setOutputFilename(){
            if(uriArrayList.isEmpty())return;
            String firstFileStartTime = checkStartTime(uriArrayList.get(0));
            String firstFile = getFullPath(uriArrayList.get(0));
            String[] ffSplit = firstFile.split("/");
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<ffSplit.length -1; i++){
                sb.append(ffSplit[i]).append("/"); // ????????????????????????????????????????????????????????????????????????
            }
            sb.append(firstFileStartTime).append(".csv");
            outputFilename = sb.toString();
        }

        private void edit() throws IOException {
            String[] filePaths = new String[uriArrayList.size()+1];
            filePaths[0] = outputFilename;
            for(int i=0; i<uriArrayList.size(); i++){
                filePaths[i+1] = getFullPath(uriArrayList.get(i));
            }
            CsvEdit.main(handler, filePaths, new GetDistance(lat,lon), homeArea, shrink);
        }
    }

    private static final int CSV_TO_GPX = 40884;
    private void csv2gpx(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");

        startActivityForResult(intent,CSV_TO_GPX);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);

        if(resultCode != Activity.RESULT_OK){
            System.out.println("intent result error");
            return;
        }
        if(requestCode == READ_FIT_FILE){
            Uri uri = resultData.getData();
            if(canWriteFile()){
                String fullPath = getFullPath(uri);
                System.out.println("java -jar FitCSVTool.jar "+ fullPath);
                CSVTool.main(new String[]{fullPath});
            }
        }else if(requestCode == ADD_EDIT_CSV){
            System.out.flush();
            Uri uri = resultData.getData();
            addEditCSV.appendToList(uri);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getFullPath(uri)+" selected")
                    .setMessage("?????????????????????????????????????????????")
                    .setPositiveButton("??????", (dialogInterface, i) -> addEditCSV.add())
                    .setNegativeButton("?????????", (dialogInterface, i) -> addEditCSV.configAndEdit());
            AlertDialog dialog = builder.create();
            dialog.show();
        }else if(requestCode == CSV_TO_GPX){
            Uri uri = resultData.getData();
            String input = getFullPath(uri);
            CsvToGPX.main(handler,new String[]{input});
        }
    }

    private boolean canWriteFile(){
        // https://stackoverflow.com/questions/8330276/write-a-file-in-external-storage-in-android
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageWriteable = false;
        }

        return mExternalStorageWriteable;
    }

    private String getFullPath(Uri uri){
        Log.d("getPath",uri.getPath());
        String path = uri.getPath().split(":")[1];
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d(root,path);
        return root + "/" + path;
    }


    public String checkStartTime(Uri uri){
        String path = getFullPath(uri);
        String rtn = path.substring(0,path.length()-4);
        File file = new File(path);
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String text;
            while((text = br.readLine()) != null){
                String[] data = text.split(",");
                if(data[0].equals("Data")
                && data[1].equals("8")
                && data[2].equals("record")
                && data[3].equals("timestamp")){
                    String ts = data[4].split("\"")[1];
                    long epochTime = Integer.parseInt(ts) + 631065600;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(epochTime*1000);
                    rtn = sdf.format(c.getTime());
                    // 1???????????????????????????
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rtn;
    }
}