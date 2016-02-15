package com.bkbatchelor.spotifystreamer.shared;

import android.app.Activity;
import android.app.ActivityManager;
import android.widget.Toast;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

public class Helper {

    static public Image preferredThumbnailImageSize(List<Image> imageList){
        Image smallestImage = null;
        int preferred_height = 200;
        int smallestDiff = preferred_height;

        for(int imgInc=1;imgInc < imageList.size(); imgInc++){
            Image image = imageList.get(imgInc);

            int diff = Math.abs(preferred_height - image.height);
            if(diff <= smallestDiff){
                smallestImage = imageList.get(imgInc);
                smallestDiff = diff;
            }
        }
        return smallestImage;
    }

    static public Image preferredLargeImageSize(List<Image> imageList){
        Image smallestImage = null;
        int preferred_height = 600;
        int smallestDiff = preferred_height;

        for(int imgInc=1;imgInc < imageList.size(); imgInc++){
            Image image = imageList.get(imgInc);

            int diff = Math.abs(preferred_height - image.height);
            if(diff <= smallestDiff){
                smallestImage = imageList.get(imgInc);
                smallestDiff = diff;
            }
        }
        return smallestImage;
    }
    public static String timeFormatter(long duration){
        int mDurationSec = 0;
        int mMinutes = 0;
        int mSeconds = 0;

        mDurationSec = ((int)duration)/1000;
        mMinutes = mDurationSec/60;
        if(mDurationSec % 60 != 0){
            mSeconds = mDurationSec - (mMinutes * 60);
        }else {
            mSeconds = 0;
        }

        if(mSeconds < 10){
            return String.valueOf(mMinutes) + ":0" + String.valueOf(mSeconds);
        }else{
            return String.valueOf(mMinutes) + ":" + String.valueOf(mSeconds);
        }

    }

    public static String timeFormatter(int duration){
        int mDurationSec = 0;
        int mMinutes = 0;
        int mSeconds = 0;

        mDurationSec = duration;
        mMinutes = mDurationSec/60;
        if(mDurationSec % 60 != 0){
            mSeconds = mDurationSec - (mMinutes * 60);
        }else {
            mSeconds = 0;
        }

        if(mSeconds < 10){
            return String.valueOf(mMinutes) + ":0" + String.valueOf(mSeconds);
        }else{
            return String.valueOf(mMinutes) + ":" + String.valueOf(mSeconds);
        }

    }

    private static void showToast(Activity context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }



}
