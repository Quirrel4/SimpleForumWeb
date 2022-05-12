package com.he.community.music;

import org.springframework.stereotype.Component;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

@Component
public class Background {

    public static void VideoRun() throws MalformedURLException {
        File file = new File("D:\\SteamLibrary\\steamapps\\music\\music1.wav");
        URI uri = file.toURI();
        System.out.println(uri);
        // AudioClip 只能播放 wav 格式的music
        AudioClip audioClip = Applet.newAudioClip(uri.toURL());
        audioClip.play();// 独立线程播放
        //循环播放
        audioClip.loop();
    }

    public static void main(String[] args) throws MalformedURLException {
        VideoRun();
    }

}
