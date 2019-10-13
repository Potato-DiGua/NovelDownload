package com.example.NovelDownload;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String url="https://www.booktxt.net/5_5907/";

        String regex = "(https|http)://(([a-z0-9]+\\.)|(www\\.))"
                + "[a-zA-Z0-9]+\\.[a-zA-Z0-9]+"
                +"(/[a-zA-Z0-9\\-_]+)+/?";//设置正则表达式
        Pattern pattern = Pattern.compile(regex);
        Matcher m=pattern.matcher(url);
        if(m.find())
            System.out.println(m.group(0));
    }
}