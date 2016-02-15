package com.bkbatchelor.spotifystreamer.topTenTracks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.dialog.PlayerDialogFragment;
import com.bkbatchelor.spotifystreamer.main.MainActivity;
import com.bkbatchelor.spotifystreamer.player.MediaService;
import com.bkbatchelor.spotifystreamer.player.PlayerActivity;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;

import java.util.ArrayList;

/**
 * Contains and organizes fragment to display Top Ten Track for a particular artist
 */
public class TopTenTracksActivity extends AppCompatActivity {
    private String artistName = "";
    private MenuItem nowPlayingItem = null;


    /**
     * Broadcast filter and reciever
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(nowPlayingItem != null) {
                if (intent.getBooleanExtra("isPlaying", false)) {
                    nowPlayingItem.setVisible(true);
                } else {
                    nowPlayingItem.setVisible(false);
                }
            }
        }
    };
    private IntentFilter filter = new IntentFilter(MediaService.NOW_PLAYING_EVENT);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setElevation(0f);

        setContentView(R.layout.activity_top_ten_tracks);
        if(savedInstanceState == null) {
            if(getIntent().getExtras()!= null) {
                String spotifyId = getIntent().getExtras().getString("spotifyId");
                artistName = getIntent().getExtras().getString("artistName");
                TopTenTracksFragment topTenTracksFragment = (TopTenTracksFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.activity_top_ten_track);

                topTenTracksFragment.onTrackRequest(spotifyId,artistName);

                ActionBar actionbar = getSupportActionBar();
                actionbar.setSubtitle(artistName);
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_top_ten_tracks, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        nowPlayingItem = menu.findItem(R.id.ss_action_now_playing);
        nowPlayingItem.setVisible(false);

        MediaService mediaService = MediaService.getMediaServiceInstance();
        if(mediaService != null) {
            boolean isCompletedPlaying = mediaService.isCompletedPlaying();
            nowPlayingItem.setVisible(!isCompletedPlaying);
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Two pane preference
        SharedPreferences sharedpreferences  =
                getSharedPreferences(MainActivity.TWO_PANE_PERFERENCE,
                        Context.MODE_PRIVATE);

        boolean mIsTwoPane = sharedpreferences.getBoolean(MainActivity.IS_TWO_PANE,false);

        int id = item.getItemId();
        if(id == R.id.ss_action_now_playing){
            MediaService mediaService = MediaService.getMediaServiceInstance();
            String artist = mediaService.getArtist();
            int trackPosition = mediaService.getTrackPosition();
            ArrayList<TrackInfo> trackInfoList = mediaService.getTrackInfoList();
            if(!mIsTwoPane) {
                if (mediaService != null) {
                    Intent playerIntent = new Intent(this, PlayerActivity.class);
                    playerIntent.putExtra("artist", artist);
                    playerIntent.putExtra("playTrack", false);
                    playerIntent.putExtra("position", trackPosition);
                    playerIntent.putParcelableArrayListExtra("top10tracks", trackInfoList);

                    startActivity(playerIntent);
                }
            }else{

                if (mediaService != null) {
                    PlayerDialogFragment playerDialogFragment = new PlayerDialogFragment();
                    playerDialogFragment.setTrackInfo(trackInfoList.get(trackPosition));
                    playerDialogFragment.setArtistName(artistName);
                    playerDialogFragment.setPositon(trackPosition);
                    playerDialogFragment.setTrackList(trackInfoList);
                    playerDialogFragment.playTrack(false);
                    playerDialogFragment.show(getFragmentManager(), "player_dialog");
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("artistName", artistName);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            String artistNameSaved = savedInstanceState.getString("artistName");
            artistName = artistNameSaved;



            ActionBar actionbar = getSupportActionBar();
            actionbar.setSubtitle(artistNameSaved);
        }
    }

    @Override
    protected void onStart() {
        this.registerReceiver(receiver, filter);

        if(nowPlayingItem != null) {
            MediaService mediaService = MediaService.getMediaServiceInstance();
            if (mediaService != null) {
                if (mediaService.isPlaying() || mediaService.isPaused()) {
                    nowPlayingItem.setVisible(true);
                } else {
                    nowPlayingItem.setVisible(false);
                }
            }
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(receiver);
        super.onPause();
    }
}
