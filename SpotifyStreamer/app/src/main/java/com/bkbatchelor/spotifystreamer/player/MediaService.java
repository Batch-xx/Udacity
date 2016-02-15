package com.bkbatchelor.spotifystreamer.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.shared.PlayState;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import jp.wasabeef.picasso.transformations.CropSquareTransformation;


public class MediaService extends Service {

    private   MediaPlayer mMediaPlayerReference;
    private   boolean mIsPaused = false;
    //    private  PlayerFragment mListener = null;
    private  Thread mMediaPlayerThread = null;
    private  boolean mIsMediaThreadRunning = false;
    private  Thread mProgressThread = null;
    private String mArtist;
    private int mTrackPosition;
    private ArrayList<TrackInfo> mTrackInfos;
    private boolean mIsCompletedPlaying = true;
    private static MediaService mMediaServiceInstance = null;
    private NotificationManager mNotificationManager;
    private PlayState playState;
    private final int NOTIFICATION_ID = 102;
    //Broadcast events
    public static String THREAD_RUNNING_EVENT = "com.bkbatchelor.spotifystreamer.player.MediaService.RUNNING";
    public static String NOW_PLAYING_EVENT = "com.bkbatchelor.spotifystreamer.player.MediaService.NOW_PLAYING";
    public static String IS_COMPLETED_PLAYING_EVENT =  "com.bkbatchelor.spotifystreamer.player.MediaService.IS_COMPLETED_PLAYING_EVENT";
    public static String TRACK_PROGRESS_EVENT = "com.bkbatchelor.spotifystreamer.player.MediaService.TRACK_PROGRESS";
    public static String TRACK_STATE_EVENT = "com.bkbatchelor.spotifystreamer.player.MediaService.TRACK_STATE";
    public static String PLAY_EVENT ="com.bkbatchelor.spotifystreamer.player.MediaService.ACTION_PLAY";
    public static String PREVIOUS_EVENT ="com.bkbatchelor.spotifystreamer.player.MediaService.ACTION_PREVIOUS";
    public static String NEXT_EVENT ="com.bkbatchelor.spotifystreamer.player.MediaService.ACTION_NEXT";
    public static String TRACK_CHANGE_EVENT ="com.bkbatchelor.spotifystreamer.player.MediaService.TRACK_CHANGE";


    private String TAG = "mediaService";



    private BroadcastReceiver mPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clickOnPlay();
            updateButtonsState();
        }
    };
    private IntentFilter mPlayFilter = new IntentFilter(PLAY_EVENT);

    private BroadcastReceiver mPreviousReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clickOnPrevious();
        }
    };
    private IntentFilter mPreviousFilter = new IntentFilter(PREVIOUS_EVENT);

    private BroadcastReceiver mNextsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clickOnNext();
        }
    };
    private IntentFilter mNextFilter = new IntentFilter(NEXT_EVENT);


    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mMediaPlayerReference = new MediaPlayer();
                mMediaPlayerReference.setAudioStreamType(AudioManager.STREAM_MUSIC);

                mMediaPlayerReference.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                        playState = PlayState.PLAY;
                        updateButtonsState();

                        if (mProgressThread == null) {
                            startProgressThread();
                        }

                        mIsCompletedPlaying = false;

                        Intent playingIntent = new Intent();
                        playingIntent.setAction(NOW_PLAYING_EVENT);
                        playingIntent.putExtra("isPlaying", true);
                        sendBroadcast(playingIntent);

                        Intent completedIntent = new Intent();
                        completedIntent.setAction(IS_COMPLETED_PLAYING_EVENT);
                        completedIntent.putExtra("completedPlaying", false);
                        sendBroadcast(completedIntent);
                    }
                });
                mMediaPlayerReference.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        setIsPaused(false);
                        mMediaPlayerReference.reset();

                        mIsCompletedPlaying = true;
                        Intent completedIntent = new Intent();
                        completedIntent.setAction(IS_COMPLETED_PLAYING_EVENT);
                        completedIntent.putExtra("completedPlaying", true);
                        sendBroadcast(completedIntent);

                        Intent playingIntent = new Intent();
                        playingIntent.setAction(NOW_PLAYING_EVENT);
                        playingIntent.putExtra("isPlaying", false);
                        sendBroadcast(playingIntent);

                        playState = PlayState.COMPLETED;

                        updateButtonsState();
                    }
                });

                mMediaPlayerReference.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        switch (what){
                            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                                Log.e(TAG,"Unspecified media player error");
                                showToast("Unspecified media player error");
                                break;
                            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                                Log.e(TAG,"Media server died.");
                                showToast("Restarting media server");
                                restartThreads();
                                return true;
                            default:
                                Log.e(TAG,"Unknown Media Player error");
                        }

                        switch(extra){
                            case MediaPlayer.MEDIA_ERROR_IO:
                                Log.e(TAG,"File or network related operation errors");
                                showToast("Network or file error");
                                break;
                            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                                Log.e(TAG,"Bitstream is not conforming to the related coding " +
                                        "standard or file spec.");
                                showToast("Invalid spotify URL");
                                break;
                            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                Log.e(TAG,"Bitstream is not conforming to the related coding " +
                                        "standard or file spec.");
                                showToast("Invalid spotify URL");
                                break;
                            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                Log.e(TAG,"media player timed out");
                                showToast("Operation timed out");
                                break;
                            default:
                                Log.e(TAG,"Unknown Media Player error");
                        }
                        return false;
                    }
                });
            }
        });

        //Fire Broadcast to PlayerActivity
        Intent runningIntent = new Intent();
        runningIntent.setAction(THREAD_RUNNING_EVENT);
        sendBroadcast(runningIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mMediaServiceInstance = this;

        startForeground(NOTIFICATION_ID, getNotification());


        if(mMediaPlayerThread == null){
            onCreate();
        }else if(!mMediaPlayerThread.isAlive()){
            mMediaPlayerThread.start();
        }

        registerReceiver(mPlayReceiver, mPlayFilter);
        registerReceiver(mPreviousReceiver, mPreviousFilter);
        registerReceiver(mNextsReceiver, mNextFilter);

        return START_STICKY;
    }

    public static MediaService getMediaServiceInstance(){
        return mMediaServiceInstance;
    }

    public boolean isMediaServiceRunning(){
        return mMediaPlayerThread.isAlive();
    }

    private void showToast(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    private void restartThreads(){
        if(mMediaPlayerReference != null) {
            mMediaPlayerReference.release();
            mMediaPlayerReference = null;
        }
        if(mMediaPlayerThread != null){
            mMediaPlayerThread.interrupt();
            mMediaPlayerThread = null;
        }

        if(mProgressThread != null){
            mProgressThread.interrupt();
            mProgressThread = null;
        }
        onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void playPreview(String artist, int trackPosition, ArrayList<TrackInfo> trackInfos){
        try {
            playState = PlayState.TRANSISTION;
            mTrackInfos = trackInfos;
            String previewUrl = trackInfos.get(trackPosition).getPreviewUrl();
            mMediaPlayerReference.reset();
            mMediaPlayerReference.setDataSource(previewUrl);
            mMediaPlayerReference.prepareAsync();


            mArtist = artist;
            mTrackPosition = trackPosition;
        } catch (Exception e) {
            Log.e("MediaPlayer Error", e.toString());
        }
    }

    public void setCurrentPosition(int position){
        mMediaPlayerReference.seekTo(position *1000);
    }

    private void startProgressThread(){
        if(mProgressThread == null) {
            mProgressThread = new Thread(new Runnable() {
                Handler mHandler = new Handler();

                public void run() {
                    if (mMediaPlayerReference != null) {
                        int currentPosition = mMediaPlayerReference.getCurrentPosition() / 1000;
                        if(mMediaPlayerReference.isPlaying()) {
                            Intent progressIntent = new Intent();
                            progressIntent.setAction(TRACK_PROGRESS_EVENT);
                            progressIntent.putExtra("currentPosition", currentPosition + 1);
                            sendBroadcast(progressIntent);
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                }
            });
            mProgressThread.start();
        }
    }



    public void continuePreview(){
        if(mIsPaused) {
            mMediaPlayerReference.start();
            setIsPaused(false);
            playState = PlayState.PLAY;
            updateButtonsState();
        }
    }
    public void pausePreview(){
        if(mMediaPlayerReference.isPlaying()) {
            mMediaPlayerReference.pause();
            setIsPaused(true);
            playState = PlayState.PAUSE;
            updateButtonsState();
        }
    }

    public void stopPreview(){
        mMediaPlayerReference.stop();
        setIsPaused(false);

    }

    private void setIsPaused(boolean state){
        mIsPaused = state;
    }

    public boolean isPlaying() {
        return mMediaPlayerReference != null && mMediaPlayerReference.isPlaying();
    }

    public boolean isPaused()
    {
        return mIsPaused;
    }

    public boolean isCompletedPlaying(){
        return mIsCompletedPlaying;
    }

    public String getArtist(){
        return mArtist;
    }

    public String getPlayState(){
        return playState.toString();
    }

    public String getTrackName(){
        return mTrackInfos.get(mTrackPosition).getTrackName();
    }

    public String getPreviewUrl(){
        return mTrackInfos.get(mTrackPosition).getPreviewUrl();
    }

    public int getTrackPosition(){
        return mTrackPosition;
    }

    public ArrayList<TrackInfo> getTrackInfoList(){
        return mTrackInfos;
    }


    private void setPendingIntents(RemoteViews remoteViews){
        Intent resultIntent = new Intent(PLAY_EVENT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, resultIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.ss_notif_player_play, pendingIntent);

        Intent previousIntent = new Intent(PREVIOUS_EVENT);
        PendingIntent previousPendingIntent = PendingIntent.getBroadcast(this, 100, previousIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.ss_notif_player_previous, previousPendingIntent);

        Intent nextIntent = new Intent(NEXT_EVENT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 100, nextIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.ss_notif_player_next, nextPendingIntent);
    }


    private NotificationCompat.Builder setBuidler(){
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showNotification = sharedPreferences.getBoolean("show_notification", true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(showNotification){
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_spotify_white_24dp)
                    .setTicker("Playing Now")
                    .setContentTitle("Spotify")
                    .setContentText("Preview Streamer");
            return mBuilder;
        }else{
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            return mBuilder;
        }
    }
    public void updateNotificationStatus(){
        updateButtonsState();
    }

    private Notification getNotification(){

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        NotificationCompat.Builder mBuilder =  setBuidler();

        setPendingIntents(remoteViews);


        Notification notification = mBuilder.build();
        notification.bigContentView = remoteViews;

        return notification;
    }



    private void clickOnPlay(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        NotificationCompat.Builder mBuilder = setBuidler();

        setPendingIntents(remoteViews);

        if (playState.equals(PlayState.PLAY)) {
            pausePreview();
        } else if (playState.equals(PlayState.PAUSE)) {
            continuePreview();
        } else if (playState.equals(PlayState.COMPLETED)) {
            playPreview(mArtist,mTrackPosition,mTrackInfos);
        }


        Notification notification = mBuilder.build();
        notification.bigContentView = remoteViews;

        mNotificationManager.notify(
                NOTIFICATION_ID,
                notification);
    }

    private void clickOnPrevious(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        NotificationCompat.Builder mBuilder = setBuidler();

        setPendingIntents(remoteViews);

        previousTrack();

        Notification notification = mBuilder.build();
        notification.bigContentView = remoteViews;

        mNotificationManager.notify(
                NOTIFICATION_ID,
                notification);
    }

    private void clickOnNext(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        NotificationCompat.Builder mBuilder = setBuidler();

        setPendingIntents(remoteViews);

        nextTrack();

        Notification notification = mBuilder.build();
        notification.bigContentView = remoteViews;

        mNotificationManager.notify(
                NOTIFICATION_ID,
                notification);
    }

    private void updateButtonsState(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        NotificationCompat.Builder mBuilder = setBuidler();

        setPendingIntents(remoteViews);

        //Update buttons

            if (playState.equals(PlayState.PLAY)) {
                remoteViews.setImageViewResource(R.id.ss_notif_player_play, R.drawable.ic_pause_black_36dp);
            } else if (playState.equals(PlayState.PAUSE)) {
                remoteViews.setImageViewResource(R.id.ss_notif_player_play, R.drawable.ic_play_black_36dp);
            } else if (playState.equals(PlayState.COMPLETED)) {
                remoteViews.setImageViewResource(R.id.ss_notif_player_play, R.drawable.ic_replay_black_36dp);
            }


        int numberTracks = mTrackInfos.size()-1;
        int lowerBound = mTrackPosition -1;
        int upperBound = mTrackPosition + 1;

        remoteViews.setImageViewResource(R.id.ss_notif_player_previous,
                R.drawable.ic_skip_previous_black_36dp);
        remoteViews.setImageViewResource(R.id.ss_notif_player_next,
                R.drawable.ic_skip_next_black_36dp);
        if (numberTracks == 0) {
            remoteViews.setImageViewResource(R.id.ss_notif_player_previous,
                    R.drawable.ic_skip_previous_grey600_36dp);
            remoteViews.setImageViewResource(R.id.ss_notif_player_next,
                    R.drawable.ic_skip_next_grey600_36dp);
        }else if(upperBound > numberTracks){
            remoteViews.setImageViewResource(R.id.ss_notif_player_previous,
                    R.drawable.ic_skip_previous_black_36dp);
            remoteViews.setImageViewResource(R.id.ss_notif_player_next,
                    R.drawable.ic_skip_next_grey600_36dp);
        }else if(lowerBound < 0){
            remoteViews.setImageViewResource(R.id.ss_notif_player_previous,
                    R.drawable.ic_skip_previous_grey600_36dp);
            remoteViews.setImageViewResource(R.id.ss_notif_player_next,
                    R.drawable.ic_skip_next_black_36dp);
        }

        Intent playStateIntent = new Intent();
        playStateIntent.setAction(TRACK_STATE_EVENT);
        playStateIntent.putExtra("playState", playState.toString());
        sendBroadcast(playStateIntent);

        //Update Artist, album, track
        remoteViews.setTextViewText(R.id.ss_notif_artist_name, mArtist);
        remoteViews.setTextViewText(R.id.ss_notif_album_name, mTrackInfos.get(mTrackPosition).getAlbumName());
        remoteViews.setTextViewText(R.id.ss_notif_track_name, mTrackInfos.get(mTrackPosition).getTrackName());

        //Update Album Image
        Notification notification = mBuilder.build();
        notification.bigContentView = remoteViews;

        Picasso.with(this)
                .load(mTrackInfos.get(mTrackPosition).getImage())
                .transform(new CropSquareTransformation())
                .into(remoteViews, R.id.ss_notif_album_img, NOTIFICATION_ID,
                        notification);

        mNotificationManager.notify(
                NOTIFICATION_ID,
                notification);
    }


    private void previousTrack(){
        if ((mTrackPosition - 1) >= 0) {
            TrackInfo trackInfo = mTrackInfos.get(mTrackPosition - 1);
            playPreview(mArtist, mTrackPosition - 1, mTrackInfos);

            Intent trackChangeIntent = new Intent();
            trackChangeIntent.setAction(TRACK_CHANGE_EVENT);
            trackChangeIntent.putExtra("trackChange", mTrackPosition);
            sendBroadcast(trackChangeIntent);

            updateButtonsState();
        }
    }

    private void nextTrack(){
        if ((mTrackPosition + 1) < mTrackInfos.size()) {
            TrackInfo trackInfo = mTrackInfos.get(mTrackPosition + 1);
            playPreview(mArtist, mTrackPosition + 1, mTrackInfos);

            Intent trackChangeIntent = new Intent();
            trackChangeIntent.setAction(TRACK_CHANGE_EVENT);
            trackChangeIntent.putExtra("trackChange", mTrackPosition);
            sendBroadcast(trackChangeIntent);

            updateButtonsState();
        }
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this,"Destorying Service...", Toast.LENGTH_SHORT).show();
        mMediaPlayerReference.release();
        mMediaPlayerReference = null;
        mMediaServiceInstance = null;


        mMediaPlayerThread.interrupt();
        mMediaPlayerThread = null;

        mProgressThread.interrupt();
        mProgressThread = null;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        unregisterReceiver(mPlayReceiver);
        unregisterReceiver(mPreviousReceiver);
        unregisterReceiver(mNextsReceiver);
    }
}
