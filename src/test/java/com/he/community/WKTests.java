package com.he.community;

import java.io.IOException;

public class WKTests {
    public static void main(String[] args) {
        String cmd="d:/wkHtmlToPDF/bin/wkhtmltoimage --quality 75 https://www.baidu.com d:/work/data/wk-images/1.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
