package com.example.NovelDownload;

import android.os.Message;

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
    public List<String> chapterTitle=new ArrayList<>();
    public List<String> chapterurl=new ArrayList<>();
    public static String user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    public String contenttype;
    public ExecutorService fixedThreadPool;
    public String[] chapterList;
    public String novelname;
    public int chaptersize;
    public void callDownload(int threadnum,String contenttype,String path,String url)
    {
        fixedThreadPool=Executors.newFixedThreadPool(threadnum);
        this.contenttype=contenttype;
        resultGetURL(url);
        save(getNovelContent(),path);
    }
    private String downLoad(String strurl) {

        String result = "";
        BufferedReader rd = null;
        // System.setProperties("sun.net.client.defaultConnectTimeout","5000");
        try {
            //System.out.println(contenttype);
            //hConnect.disconnect();
            //System.exit(0);
            URL url = new URL(strurl);
            HttpURLConnection hConnect = (HttpURLConnection) url.openConnection();
            //定义请求头
            hConnect.addRequestProperty("User-Agent", user_agent);
            hConnect.addRequestProperty("Connection" , "keep-alive");
            int responsecode = hConnect.getResponseCode();// 获取响应码
            if (responsecode != 200)// 网站无响应
                return "";
            rd = new BufferedReader(new InputStreamReader(hConnect.getInputStream(),contenttype));

            String r;
            while ((r = rd.readLine()) != null) {
                result += r;
            }
            //System.out.println(result);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

            try {
                if (rd != null)
                    rd.close();
            } catch (IOException e) {
                // TODO 自动生成的 catch 块
                e.printStackTrace();
            }
        }
        return result;
    }
    public void autoset(String url)
    {
        contenttype=getEncodeType(url);
        novelname=getTitle(url);
    }
    public String getTitle(String url)
    {
        String result=downLoad(url);
        if(!result.isEmpty())
        {
            String name=Jsoup.parse(result).title();
            name=name.substring(0,name.indexOf("_"));
            return name;
        }
        else
            return "";

    }
    public String getEncodeType(String strurl)// 获取网站编码格式
    {
        URL url;
        HttpURLConnection hConnect=null;
        try {
            url = new URL(strurl);
            hConnect = (HttpURLConnection) url.openConnection();
            int responsecode = hConnect.getResponseCode();// 获取响应码
            if (responsecode != 200)// 网站无响应
                return "";
        } catch (IOException e1) {
            // TODO 自动生成的 catch 块
            e1.printStackTrace();
        }
        String contenttype =hConnect.getContentType();
        if (contenttype!=null&&!contenttype.isEmpty() && contenttype.contains("charset"))// 从响应头获取编码格式，但是会出现编码格式的情况
        {
			/*String[] content = contenttype.split(";");
			for (String con : content) {
				if (con.contains("charset")) {
					return con.split("=")[1];
				}
			}*/
            return contenttype.substring(contenttype.indexOf("charset=")+8);

        } else { // 从网页源代码中获取编码格式
            BufferedReader rd = null;
            try {

                rd= new BufferedReader(new InputStreamReader(hConnect.getInputStream()));

                String r;
                String result = "";
                while ((r = rd.readLine()) != null) {
                    result += r;
                }
                //System.out.println(result);
                Document doc = Jsoup.parse(result);
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
                    rd.close();
                } catch (IOException e) {
                    // TODO 自动生成的 catch 块
                    e.printStackTrace();
                }
            }
        }
        return "UTF-8";//获取编码格式失败，默认使用utf-8编码

    }

    public boolean  resultGetURL(String strurl) {
        MainActivity.handler.sendEmptyMessage(MainActivity.CHAPTERLIST_START);
        String result = downLoad(strurl);
        if(result.isEmpty()||result==null)
            return false;
        try {
            title= Jsoup.parse(result).title();
            String baseUrl = strurl.substring(0,strurl.indexOf("/",strurl.indexOf(".")));
            Document document=Jsoup.parse(result,baseUrl);
            Elements urllist=document.select("a[href]");
            Pattern r = Pattern.compile("第?[0-9一二三四五六七八九十百千万]+章? ");
            for(Element e:urllist)
            {
                String name=e.text();
                Matcher m=r.matcher(name);
                if(m.find())
                {
                    if(!name.contains("章")&&!name.contains("第"))
                    {
                        name=name.replaceAll("[\\d一二三四五六七八九十百千万]+","第$0章");
                    }
                    chapterTitle.add(name);
                    chapterurl.add(e.absUrl("href"));
                }

                //System.out.print(e.select("a").first().absUrl("href"));
                //System.out.println(e.text());
            }

            //resulthandle = new String(Jsoup.clean(result, );

            MainActivity.handler.sendEmptyMessage(MainActivity.CHAPTERLIST_FINISH);
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
        if(chapterurl.size()==0||chapterTitle.size()==0||chapterTitle.size()!=chapterurl.size())
        {
            MainActivity.handler.sendEmptyMessage(MainActivity.CHAPTERLIST_ERROR);
            return "";
        }

        int size=chapterTitle.size();
        chaptersize=size;
        MainActivity.handler.sendEmptyMessage(R.id.downloadbutton);
        chapterList=new String[size];
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<size;i++)
        {
            final int index=i;
            fixedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    getChaprerContent(index);
                }
            });
        }
        fixedThreadPool.shutdown();
        while (true)
        {
            if(fixedThreadPool.isTerminated())
            {
                for(String str:chapterList)
                {
                    stringBuilder.append(str);
                }
                MainActivity.handler.sendEmptyMessage(MainActivity.DOWNLOAD_FINISH);
                return stringBuilder.toString();
            }
        }

    }
    public boolean getChaprerContent(int i)
    {
        StringBuilder stringBuilder=new StringBuilder();
        String result=downLoad(chapterurl.get(i));
        result=result.replaceAll("<br />","###");
        result=result.replaceAll("&nbsp;","nbsp");
        if(result!=null&&!result.isEmpty());
        {
            Element e=Jsoup.parse(result).body().select("div#content").first();
            String text=e.ownText().replaceAll("######","\n");
            text=text.replaceAll("####","\n");
            text=text.replaceAll("nbsp"," ")+"\n";
            stringBuilder.append(chapterTitle.get(i));
            stringBuilder.append("\n");
            stringBuilder.append(text);
            chapterList[i]=stringBuilder.toString();
            if(chapterList[i]==null||chapterList[i].isEmpty())
            {
                MainActivity.handler.sendMessage(MainActivity.handler.obtainMessage(
                        MainActivity.GET_CHAPTER_ERROR,(chapterTitle.get(i)+"下载失败")));
                return false;
            }
            //System.out.println(i+"章下载完成");
            MainActivity.handler.sendEmptyMessage(R.id.downloadBar);
            return true;
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
                f.getParentFile().mkdirs();
            }
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"utf-8"));
            bw.write(content,0,content.length());
            bw.flush();
            bw.close();
            MainActivity.handler.sendEmptyMessage(MainActivity.WRITE_FINISH);
            //System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
