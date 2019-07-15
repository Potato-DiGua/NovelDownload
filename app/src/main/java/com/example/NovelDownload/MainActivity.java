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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button downloadbtn;
    private Button autobtn;
    private EditText urledit;
    private EditText typeedit;
    private EditText nameedit;
    private Download download=new Download();
    private Spinner spinner;
    private String path;
    private ProgressBar downloadBar;
    private ProgressBar showBar;
    public static Handler handler;
    public final static int DOWNLOAD_FINISH=22;
    public final static int WRITE_FINISH=33;
    public final static int CHAPTERLIST_START=44;
    public final static int CHAPTERLIST_FINISH=45;
    public final static int CHAPTERLIST_ERROR=46;
    public final static int GET_CHAPTER_ERROR=55;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        downloadBar=findViewById(R.id.downloadBar);
        downloadbtn=findViewById(R.id.downloadbutton);
        urledit=findViewById(R.id.urledittext);
        spinner=findViewById(R.id.spinner);
        typeedit=findViewById(R.id.typeeditText);
        autobtn=findViewById(R.id.autobutton);
        nameedit=findViewById(R.id.nameeditText);
        showBar=findViewById(R.id.progressBar);
        handler=new Handler(){
            private int progress=0;
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what)
                {
                    case R.id.autobutton://自动填写完成
                        setUIEnabled(true);
                        typeedit.setText(download.contenttype);
                        nameedit.setText(download.novelname);
                        showBar.setVisibility(View.GONE);
                        break;
                    case R.id.downloadbutton://设置进度条最大值
                        downloadBar.setMax(download.chaptersize);
                        break;
                    case R.id.downloadBar://调整进度
                        downloadBar.setProgress(++progress);
                        break;
                    case DOWNLOAD_FINISH:
                        showMessage("下载完成,开始写入");
                        break;
                    case WRITE_FINISH:
                        showMessage("写入完成");
                        downloadBar.setVisibility(View.GONE);
                        setUIEnabled(true);
                        break;
                    case CHAPTERLIST_START:
                        showMessage("开始获取所有章节网址");
                        break;
                    case CHAPTERLIST_FINISH:
                        showMessage("获取所有章节网址结束");
                        break;
                    case CHAPTERLIST_ERROR:
                        showMessage("获取所有章节地址失败,请检查网址是否正确或者更换网站");
                        break;
                    case GET_CHAPTER_ERROR:
                        showMessage(msg.obj.toString());

                }
            }
        };
        final ArrayList<Integer> numlist=new ArrayList<>();
        for(int i=1;i<=16;i++)
        {
            numlist.add(i);
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,numlist);
        spinner.setAdapter(adapter);
        spinner.setSelection(5);
        autobtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage("开始自动获取类型与文件名");
                showBar.setVisibility(View.VISIBLE);
                setUIEnabled(false);
                final String url=urledit.getText().toString();
                Log.i("auto","click"+url);
                if(url!=null&&!url.isEmpty())
                {
                    new Thread(new Runnable() {
                        final String strurl=url;
                        @Override
                        public void run() {
                            download.autoset(strurl);
                            handler.sendEmptyMessage(R.id.autobutton);
                            Log.i("auto","3");
                        }
                    }).start();

                }

            }
        });
        downloadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String url=urledit.getText().toString();
                final int threadnum=numlist.get(spinner.getSelectedItemPosition());
                final String contenttype=typeedit.getText().toString();
                final String name=nameedit.getText().toString();
                if(url!=null&&contenttype!=null&&name!=null&&!url.isEmpty()&&!contenttype.isEmpty()&&!name.isEmpty())
                {
                    downloadBar.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("donload",url);
                            download.callDownload(threadnum,contenttype,path+"/"+name+".txt",url);
                        }
                    }).start();

                }
                setUIEnabled(false);


            }
        });
    }
    private void setUIEnabled(Boolean enabled)
    {
        urledit.setEnabled(enabled);
        spinner.setEnabled(enabled);
        nameedit.setEnabled(enabled);
        typeedit.setEnabled(enabled);
        autobtn.setEnabled(enabled);
        downloadbtn.setEnabled(enabled);
    }
    private void showMessage(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
            //Log.e(TAG_SERVICE, "checkPermission: 已经授权！");
        }
    }

}
