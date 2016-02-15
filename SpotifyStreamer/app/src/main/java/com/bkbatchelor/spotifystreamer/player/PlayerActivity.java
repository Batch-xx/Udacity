package com.bkbatchelor.spotifystreamer.player;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements PlayerFragment.OnFragmentPlayerListener {


    private  int mPosition;
    private String mArtistName;
    private String mPlayingTrack = "";
    private String mPlayingArtist=  "";
    private boolean mHasRestored = false;
    private ShareActionProvider mShareActionProvider;


    private String TAG = "PlayerActivity";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlayerFragment playerFragment = (PlayerFragment)getFragmentManager()
                    .findFragmentById(R.id.ss_player_fragment);
            playerFragment.initializeMediaService();
        }
    };

    private IntentFilter filter = new IntentFilter(MediaService.THREAD_RUNNING_EVENT);

    private BroadcastReceiver playNowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setArtist();
            setTrack();
            mShareActionProvider.setShareIntent(getDefaultIntent());
        }
    };

    private IntentFilter playNowFilter = new IntentFilter(MediaService.NOW_PLAYING_EVENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setElevation(0f);

        if(MediaService.getMediaServiceInstance()== null) {
            Intent serviceForegroundIntent = new Intent();
            serviceForegroundIntent.setClass(PlayerActivity.this, MediaService.class);
            startService(serviceForegroundIntent);
        }

        if(savedInstanceState != null){
            mPlayingArtist = savedInstanceState.getString("artist");
            mPlayingTrack = savedInstanceState.getString("track");
        }

        setContentView(R.layout.activity_player);
    }


    @Override
    protected void onResume() {
        this.registerReceiver(playNowReceiver, playNowFilter);
        this.registerReceiver(receiver,filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(playNowReceiver);
        this.unregisterReceiver(receiver);

        super.onPause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(getIntent().getExtras()!= null) {
            boolean doPlayTrack = getIntent().getExtras().getBoolean("playTrack");
            mArtistName = getIntent().getExtras().getString("artist");
            ActionBar actionbar = getSupportActionBar();
            if (mArtistName != null) {
                actionbar.setTitle(mArtistName);
            } else {
                actionbar.setTitle("");
            }

            if (!mHasRestored) {
                mPosition = getIntent().getExtras().getInt("position");
                ArrayList<TrackInfo> top10tracks = getIntent().getExtras().getParcelableArrayList("top10tracks");

                PlayerFragment playerFragment = (PlayerFragment) getFragmentManager()
                        .findFragmentById(R.id.ss_player_fragment);
                playerFragment.initializeTrack(mArtistName, top10tracks, mPosition, doPlayTrack);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mHasRestored = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("artist", mPlayingArtist);
        outState.putString("track", mPlayingTrack);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_player, menu);

        MenuItem shareItem = menu.findItem(R.id.ss_action_share);
        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);
        mShareActionProvider.setShareIntent(getDefaultIntent());
        return super.onCreateOptionsMenu(menu);
    }

    private Intent getDefaultIntent() {
//        FragmentManager fm = getFragmentManager();
//        PlayerFragment fragment = (PlayerFragment)fm.findFragmentById(R.id.ss_player_fragment);
//        String artist = fragment.getArtist();
//        String track = fragment.getTrackName();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_TEXT, "Listening to " + mPlayingArtist + "'s " + "\"" + mPlayingTrack + "\" ");
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.ss_external_play) {
            FragmentManager fm = getFragmentManager();
            PlayerFragment fragment = (PlayerFragment)fm.findFragmentById(R.id.ss_player_fragment);
            String url = fragment.getPreviewUrl();
            Intent externalIntent = new Intent(Intent.ACTION_VIEW);
            externalIntent.setData(Uri.parse(url));
            startActivity(externalIntent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPlayerInteraction(Uri uri) {

    }


    /* Start Media Player Service */
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setArtist(){
        MediaService mediaService = MediaService.getMediaServiceInstance();

        if(mediaService != null){
            mPlayingArtist =  mediaService.getArtist();
        }else{
            mPlayingArtist = "";
        }
    }

    private void setTrack(){
        MediaService mediaService = MediaService.getMediaServiceInstance();
        if(mediaService != null){
            mPlayingTrack =  mediaService.getTrackName();
        }else{
            mPlayingTrack =  "";
        }
    }

}
