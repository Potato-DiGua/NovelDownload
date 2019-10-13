package com.example.NovelDownload;

import android.text.TextUtils;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Download {
    public String title;
    public List<String> chapterTitleList =new ArrayList<>();
    public List<String> chapterUrlList =new ArrayList<>();
    private static String sUserAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    public String contentType;
    public ExecutorService fixedThreadPool;
    public String[] chapterArray;
    public String novelName;
    public int chapterSize;
    private MainActivity mainActivity;

    public Download(MainActivity mainActivity)
    {
        this.mainActivity=mainActivity;
    }

    public void callDownload(int threadNum,String contentType,String path,String url)
    {
        fixedThreadPool=Executors.newFixedThreadPool(threadNum);
        this.contentType=contentType;
        Log.d("type",contentType);
        resultGetURL(url);
        save(getNovelContent(),path);
    }
    private String downLoad(String strUrl) {

        String result = "";
        BufferedReader rd = null;
        try {
            URL url = new URL(strUrl);
            HttpURLConnection hConnect = (HttpURLConnection) url.openConnection();
            //定义请求头
            hConnect.addRequestProperty("User-Agent", sUserAgent);
            hConnect.addRequestProperty("Connection" , "keep-alive");
            hConnect.setConnectTimeout(5000);
            hConnect.setReadTimeout(5000);
            hConnect.setDefaultUseCaches(false);
            hConnect.connect();

            int response_code = hConnect.getResponseCode();// 获取响应码
            if (response_code == 200)
            {
                rd = new BufferedReader(new InputStreamReader(hConnect.getInputStream(),contentType));
                String r;
                while ((r = rd.readLine()) != null) {
                    result += r;
                }
            }
            else
                result="";
            hConnect.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rd != null)
                    rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    public void autoSet(String url)
    {
        contentType=getEncodeType(url);
        novelName =getTitle(url);
        chapterTitleList.clear();
        chapterUrlList.clear();
    }
    public String getTitle(String url)
    {
        String result=downLoad(url);
        if(!result.isEmpty())
        {
            String name=Jsoup.parse(result).select("div#info").select("h1").first().text();
            return name;
        }
        else
            return "";

    }
    public String getEncodeType(String strUrl)// 获取网站编码格式
    {
        URL url=null;
        HttpURLConnection hConnect=null;
        try {
            url = new URL(strUrl);
            hConnect = (HttpURLConnection) url.openConnection();
            hConnect.setConnectTimeout(5000);
            hConnect.setReadTimeout(5000);
            int responseCode = hConnect.getResponseCode();// 获取响应码
            if (responseCode != 200)// 网站无响应
                return "";
        } catch (IOException e1) {
            // TODO 自动生成的 catch 块
            e1.printStackTrace();
        }
        String contenttype=null;
        if(hConnect!=null)
            contenttype =hConnect.getContentType();
        if (contenttype!=null&&!contenttype.isEmpty() && contenttype.contains("charset"))// 从响应头获取编码格式，但是会出现编码格式的情况
        {
            return contenttype.substring(contenttype.indexOf("charset=")+8);

        } else { // 从网页源代码中获取编码格式
            BufferedReader rd = null;
            try {
                hConnect.connect();
                rd= new BufferedReader(new InputStreamReader(hConnect.getInputStream()));

                String r;

                StringBuilder stringBuilder=new StringBuilder();
                while ((r = rd.readLine()) != null) {
                    stringBuilder.append(r);
                }
                hConnect.disconnect();
                //System.out.println(result);
                Document doc = Jsoup.parse(stringBuilder.toString());
                String s;
                if(doc.hasAttr("charset"))
                    s= String.valueOf(doc.select("meta[charset]").first().attr("charset"));
                else {
                    s="";
                }
                if(s!=null&&!s.isEmpty())
                {
                    return s;
                }
                else {
                    Element e=doc.select("meta[http-equiv=\"content-type\"]").first();
                    if(e==null)
                    {
                        e=doc.select("meta[http-equiv=\"Content-Type\"]").first();
                    }

                    //System.out.println(e.attr("content"));
                    //System.out.println(e.attr("content").indexOf("charset="));
                    //System.out.println(e.attr("content").substring(e.attr("content").indexOf("charset=")+8));
                    return e.attr("content").substring(e.attr("content").indexOf("charset=")+8);
                }

            } catch (Exception e) {
                // TODO 自动生成的 catch 块
                e.printStackTrace();
            }finally {
                try {
                    if(rd!=null)
                        rd.close();
                } catch (IOException e) {
                    // TODO 自动生成的 catch 块
                    e.printStackTrace();
                }
            }
        }
        return "";//获取编码格式失败

    }

    public boolean  resultGetURL(String strUrl) {
        mainActivity.handler.sendEmptyMessage(MainActivity.CHAPTER_LIST_START);
        String result = downLoad(strUrl);
        if(result.isEmpty())
            return false;
        try {
            title= Jsoup.parse(result).title();
            Document document=Jsoup.parse(result,strUrl.substring(0,strUrl.indexOf("/",8)));
            Elements urlList=document.select("div#list").select("a[href]");
            Pattern r = Pattern.compile("第?[0-9一二三四五六七八九十百千万]+章?");
            for(Element e:urlList)
            {
                String name=e.text();
                Matcher m=r.matcher(name);
                if(m.find())
                {
                    if(!name.contains("章")&&!name.contains("第"))
                    {
                        name=name.replaceAll("[0-9一二三四五六七八九十百千万]+","第$0章");
                    }
                    //Log.d("name",name);
                    chapterTitleList.add(name);
                    chapterUrlList.add(e.absUrl("href"));
                    //Log.d("urllist",e.absUrl("href"));
                }

                //System.out.print(e.select("a").first().absUrl("href"));
                //System.out.println(e.text());
            }
            mainActivity.handler.sendEmptyMessage(MainActivity.CHAPTER_LIST_FINISH);
            return true;

        } catch (Exception e) {
            // TODO 自动生成的 catch 块
            System.out.println(result);
            e.printStackTrace();
            return false;
        }

    }
    public String getNovelContent()
    {
        if(chapterUrlList.size()==0|| chapterTitleList.size()==0|| chapterTitleList.size()!= chapterUrlList.size())
        {
            mainActivity.handler.sendEmptyMessage(MainActivity.CHAPTER_LIST_ERROR);
            return "";
        }

        int size= chapterTitleList.size();
        chapterSize =size;
        mainActivity.handler.sendEmptyMessage(R.id.btn_download);
        chapterArray =new String[size];
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<size;i++)
        {
            final int index=i;
            fixedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if(getChapterContent(index))
                    {
                        mainActivity.handler.sendEmptyMessage(R.id.downloadBar);
                    }
                    else
                    {
                        mainActivity.handler.sendMessage(mainActivity.handler.obtainMessage(
                                MainActivity.GET_CHAPTER_ERROR,(chapterTitleList.get(index)+"下载失败")));
                    }
                }
            });
        }
        fixedThreadPool.shutdown();
        while (true)
        {
            if(fixedThreadPool.isTerminated())
            {
                for(String str: chapterArray)
                {
                    stringBuilder.append(str);
                }
                mainActivity.handler.sendEmptyMessage(MainActivity.DOWNLOAD_FINISH);
                return stringBuilder.toString();
            }
        }

    }
    public boolean getChapterContent(int i)
    {
        StringBuilder stringBuilder=new StringBuilder();
        String result=downLoad(chapterUrlList.get(i));
        if(!result.isEmpty());
        {
            result=result.replaceAll("<br />","###");
            result=result.replaceAll("&nbsp;","nbsp");
            Element e=Jsoup.parse(result).body().select("div#content").first();
            if(e==null)
                return false;
            String text=e.ownText().replaceAll("######","\n");
            if(TextUtils.isEmpty(text))
                return false;
            text=text.replaceAll("###","\n");
            text=text.replaceAll("nbsp"," ")+"\n";
            stringBuilder.append(chapterTitleList.get(i));
            stringBuilder.append("\n");
            stringBuilder.append(text);
            chapterArray[i]=stringBuilder.toString();
            return !TextUtils.isEmpty(chapterArray[i]);
        }
    }
    public void save(String content,String path)
    {
        if(content.isEmpty())
            return;
        try {
            File f=new File(path);
            if(!f.getParentFile().exists())
            {
                if(f.getParentFile().mkdirs())
                    return;
            }
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"utf-8"));
            bw.write(content,0,content.length());
            bw.flush();
            bw.close();
            mainActivity.handler.sendEmptyMessage(MainActivity.WRITE_FINISH);
            //System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
