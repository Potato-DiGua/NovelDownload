package com.example.NovelDownload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button downloadBtn;
    private Button autoBtn;
    public EditText urlEt;
    public Spinner typeSp;
    public EditText nameEt;
    private Download download = new Download(this);
    public Spinner spinner;
    private String path;
    public ProgressBar downloadBar;
    public ProgressBar showBar;

    public final static int DOWNLOAD_FINISH = 22;
    public final static int WRITE_FINISH = 33;
    public final static int CHAPTER_LIST_START = 44;
    public final static int CHAPTER_LIST_FINISH = 45;
    public final static int CHAPTER_LIST_ERROR = 46;
    public final static int GET_CHAPTER_ERROR = 55;


    public final Handler handler = new MyHandler(this);

    static class MyHandler extends Handler {
        private int progress = 0;
        WeakReference<MainActivity> mWeakReference;

        public MyHandler(MainActivity mainActivity) {
            super();
            mWeakReference = new WeakReference<>(mainActivity);
        }

        public void handleMessage(@NonNull Message msg) {
            MainActivity mainActivity = mWeakReference.get();
            switch (msg.what) {
                case R.id.btn_auto://自动填写完成
                    mainActivity.setUIEnabled(true);
                    int pos=mainActivity.download.contentType.toLowerCase().equals("utf-8")?0:1;
                    mainActivity.typeSp.setSelection(pos);
                    mainActivity.nameEt.setText(mainActivity.download.novelName);
                    mainActivity.showBar.setVisibility(View.GONE);
                    break;
                case R.id.btn_download://设置进度条最大值
                    mainActivity.downloadBar.setMax(mainActivity.download.chapterSize);
                    break;
                case R.id.downloadBar://调整进度
                    mainActivity.downloadBar.setProgress(++progress);
                    break;
                case DOWNLOAD_FINISH:
                    mainActivity.showMessage("下载完成,开始写入");
                    break;
                case WRITE_FINISH:
                    mainActivity.showMessage("写入完成");
                    mainActivity.downloadBar.setVisibility(View.GONE);
                    mainActivity.setUIEnabled(true);
                    break;
                case CHAPTER_LIST_START:
                    mainActivity.showMessage("开始获取所有章节网址");
                    break;
                case CHAPTER_LIST_FINISH:
                    mainActivity.showMessage("获取所有章节网址结束");
                    break;
                case CHAPTER_LIST_ERROR:
                    mainActivity.showMessage("获取所有章节地址失败,请检查网址是否正确或者更换网站");
                    mainActivity.setUIEnabled(true);
                    break;
                case GET_CHAPTER_ERROR:
                    mainActivity.showMessage(msg.obj.toString());
                    break;

            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        downloadBar = findViewById(R.id.downloadBar);
        downloadBtn = findViewById(R.id.btn_download);
        urlEt = findViewById(R.id.et_url);
        spinner = findViewById(R.id.spinner_thread);
        typeSp = findViewById(R.id.spinner_type);
        autoBtn = findViewById(R.id.btn_auto);
        nameEt = findViewById(R.id.et_name);
        showBar = findViewById(R.id.progressBar);
        final ArrayList<Integer> numlist = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            numlist.add(i);
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, numlist);
        spinner.setAdapter(adapter);
        spinner.setSelection(5);
        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                final String url = urlEt.getText().toString();
                Log.i("auto", "click " + url);

                if (!TextUtils.isEmpty(url)) {
                    if (isURL(url)) {
                        setUIEnabled(false);
                        showBar.setVisibility(View.VISIBLE);
                        showMessage("开始自动获取类型与文件名");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                download.autoSet(url);
                                handler.sendEmptyMessage(R.id.btn_auto);
                                Log.i("auto", "3");
                            }
                        }).start();
                    }
                    else
                    {
                        showMessage("网址不合法");
                    }
                } else {
                    showMessage("网址不能为空");
                }

            }
        });
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String url = urlEt.getText().toString();
                final int threadNum = numlist.get(spinner.getSelectedItemPosition());
                final String contentType = typeSp.getSelectedItem().toString();
                final String name = nameEt.getText().toString();
                if (!url.isEmpty() && !contentType.isEmpty() && !name.isEmpty()) {
                    downloadBar.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("download", url);
                            download.callDownload(threadNum, contentType, path + File.separator + name + ".txt", url);
                        }
                    }).start();

                }
                setUIEnabled(false);


            }
        });
    }

    public static boolean isURL(String str) {
        String regex = "(https|http)://(([a-z0-9]+\\.)|(www\\.))"
                + "[a-zA-Z0-9]+\\.[a-zA-Z0-9]+"
                +"(/[a-zA-Z0-9\\-_]+)+/?";//设置正则表达式
        return str.matches(regex);


    }

    public void setUIEnabled(Boolean enabled) {
        urlEt.setEnabled(enabled);
        spinner.setEnabled(enabled);
        nameEt.setEnabled(enabled);
        typeSp.setEnabled(enabled);
        autoBtn.setEnabled(enabled);
        downloadBtn.setEnabled(enabled);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
            //Log.e(TAG_SERVICE, "checkPermission: 已经授权！");
        }
    }

}
