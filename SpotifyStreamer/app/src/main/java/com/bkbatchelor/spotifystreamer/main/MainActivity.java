package com.bkbatchelor.spotifystreamer.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.artistSearch.ArtistSearchFragment;
import com.bkbatchelor.spotifystreamer.dialog.PlayerDialogFragment;
import com.bkbatchelor.spotifystreamer.perferences.SpotifySettingsActivity;
import com.bkbatchelor.spotifystreamer.player.MediaService;
import com.bkbatchelor.spotifystreamer.player.PlayerActivity;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;
import com.bkbatchelor.spotifystreamer.topTenTracks.TopTenTracksFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ArtistSearchFragment.OnArtistSearchListener {
    private MenuItem mNowPlayingItem = null;
    private MenuItem mSharedItem = null;
    private MenuItem mPlayExternalItem = null;
    private boolean mIsTwoPane = false;
    private static final String TOPTENFRAGMENT_TAG = "TTFTAG";
    public static String TWO_PANE_PERFERENCE = "TTPERF";
    public static String IS_TWO_PANE = "isTwoPane";
    public ShareActionProvider mShareActionProvider;
    public String mPlayingArtist = "";
    public String mPlayingTrack = "";
    public String mPreviewUrl = "";
    public boolean mShowSharedItem = false;
    public boolean mShowPlayExternalItem = false;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mNowPlayingItem != null) {
                if (intent.getBooleanExtra("isPlaying", false)) {
                    mNowPlayingItem.setVisible(true);

                    setArtist();
                    setTrack();
                    setPreviewUrl();

                    if(mIsTwoPane){
                        mShareActionProvider.setShareIntent(getDefaultIntent());
                        mSharedItem.setVisible(true);
                        mPlayExternalItem.setVisible(true);
                    }
                } else {
                    mNowPlayingItem.setVisible(false);
                    if(mIsTwoPane){
                        mSharedItem.setVisible(false);
                        mPlayExternalItem.setVisible(false);
                    }
                }
            }
        }
    };

    private IntentFilter filter = new IntentFilter(MediaService.NOW_PLAYING_EVENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //Two pane preference
        SharedPreferences sharedpreferences  = getSharedPreferences(MainActivity.TWO_PANE_PERFERENCE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        //Enable action bar icon
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_spotify);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setElevation(0f);


        //Check for two-panes
        if(findViewById(R.id.ss_TopTenTracks_container) != null){
            mIsTwoPane =true;
            editor.putBoolean(IS_TWO_PANE,mIsTwoPane);

            ImageView logo = (ImageView)findViewById(R.id.ss_search_logo);
            logo.setVisibility(View.INVISIBLE);

            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction().replace(R.id.ss_TopTenTracks_container,
                        new TopTenTracksFragment(),TOPTENFRAGMENT_TAG)
                        .commit();
            }
        }else{
            mIsTwoPane = false;
            editor.putBoolean(IS_TWO_PANE,mIsTwoPane);
        }
        editor.commit();

        if(savedInstanceState != null){
            mIsTwoPane = savedInstanceState.getBoolean("twoPane");
            if(mIsTwoPane){
                mShowSharedItem = savedInstanceState.getBoolean("shareItem");
                mShowPlayExternalItem = savedInstanceState.getBoolean("externalPlay");
                mPlayingArtist = savedInstanceState.getString("artist");
                mPlayingTrack = savedInstanceState.getString("track");
                mPreviewUrl = savedInstanceState.getString("previewUrl");
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("twoPane",mIsTwoPane);
        if(mIsTwoPane) {
            outState.putBoolean("shareItem", mSharedItem.isVisible());
            outState.putBoolean("externalPlay", mPlayExternalItem.isVisible());
        }
        outState.putString("artist", mPlayingArtist);
        outState.putString("track", mPlayingTrack);
        outState.putString("previewUrl",mPreviewUrl);
        super.onSaveInstanceState(outState);
    }

    private Intent getDefaultIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_TEXT, "Listening to " + mPlayingArtist + "'s " + "\"" + mPlayingTrack + "\" ");

        return intent;
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

    private void setPreviewUrl(){
        MediaService mediaService = MediaService.getMediaServiceInstance();
        if(mediaService != null){
            mPreviewUrl =  mediaService.getPreviewUrl();
        }else{
            mPreviewUrl =  "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (mIsTwoPane){
            MenuItem shareItem = menu.findItem(R.id.ss_action_share);
            mShareActionProvider = (ShareActionProvider)
                    MenuItemCompat.getActionProvider(shareItem);
            mShareActionProvider.setShareIntent(getDefaultIntent());
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mNowPlayingItem = menu.findItem(R.id.ss_action_now_playing);
        mNowPlayingItem.setVisible(false);
        if(mIsTwoPane) {
            mSharedItem = menu.findItem(R.id.ss_action_share);
            mPlayExternalItem = menu.findItem(R.id.ss_external_play);

            if(mIsTwoPane) {
                if (mShowSharedItem) {
                    mSharedItem.setVisible(true);
                }
                if (mShowPlayExternalItem) {
                    mPlayExternalItem.setVisible(true);
                }
            }
        }


        MediaService mediaService = MediaService.getMediaServiceInstance();
        if(mediaService != null) {
            boolean isCompletedPlaying = mediaService.isCompletedPlaying();
            mNowPlayingItem.setVisible(!isCompletedPlaying);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(!mIsTwoPane) {
            if (id == R.id.ss_country_settings) {
                Intent countryCodeIntent = new Intent(this, SpotifySettingsActivity.class);
                startActivity(countryCodeIntent);
            } else if (id == R.id.ss_action_now_playing) {
                MediaService mediaService = MediaService.getMediaServiceInstance();
                String artist = mediaService.getArtist();
                int trackPosition = mediaService.getTrackPosition();
                ArrayList<TrackInfo> trackInfoList = mediaService.getTrackInfoList();
                if (mediaService != null) {
                    Intent playerIntent = new Intent(this, PlayerActivity.class);
                    playerIntent.putExtra("artist", artist);
                    playerIntent.putExtra("playTrack", false);
                    playerIntent.putExtra("position", trackPosition);
                    playerIntent.putParcelableArrayListExtra("top10tracks", trackInfoList);

                    startActivity(playerIntent);
                }
            }
        }else {
            if (id == R.id.ss_country_settings) {
                Intent countryCodeIntent = new Intent(this, SpotifySettingsActivity.class);
                startActivity(countryCodeIntent);
            } else if (id == R.id.ss_action_now_playing) {
                MediaService mediaService = MediaService.getMediaServiceInstance();
                String artist = mediaService.getArtist();
                int trackPosition = mediaService.getTrackPosition();
                ArrayList<TrackInfo> trackInfoList = mediaService.getTrackInfoList();

                if (mediaService != null) {
                    PlayerDialogFragment playerDialogFragment = new PlayerDialogFragment();
                    playerDialogFragment.setTrackInfo(trackInfoList.get(trackPosition));
                    playerDialogFragment.setArtistName(artist);
                    playerDialogFragment.setPositon(trackPosition);
                    playerDialogFragment.setTrackList(trackInfoList);
                    playerDialogFragment.playTrack(false);
                    playerDialogFragment.show(getFragmentManager(), "player_dialog");
                }
            }else if (id == R.id.ss_external_play) {
                Intent externalIntent = new Intent(Intent.ACTION_VIEW);
                externalIntent.setData(Uri.parse(mPreviewUrl));
                startActivity(externalIntent);
                if(!mIsTwoPane) {
                    finish();
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onStart() {

        this.registerReceiver(receiver, filter);

        if (mNowPlayingItem != null) {
            MediaService mediaService = MediaService.getMediaServiceInstance();
            if (mediaService != null) {
                if (mediaService.isPlaying()|| mediaService.isPaused()) {
                    mNowPlayingItem.setVisible(true);
                    if(mIsTwoPane){
                        mSharedItem.setVisible(true);
                        mPlayExternalItem.setVisible(true);
                    }
                } else {
                    mNowPlayingItem.setVisible(false);
                    if(mIsTwoPane){
                        mSharedItem.setVisible(false);
                        mPlayExternalItem.setVisible(false);
                    }
                }
            }
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        if(isFinishing()) {
            Intent stopIntent = new Intent(MainActivity.this, MediaService.class);
            stopService(stopIntent);
        }
        super.onDestroy();
    }


    @Override
    public void onTopTenRequest(String spotifyId, String artistName) {
        TopTenTracksFragment topTenTracksFragment = (TopTenTracksFragment)getSupportFragmentManager()
                .findFragmentByTag(TOPTENFRAGMENT_TAG);
        topTenTracksFragment.onTrackRequest(spotifyId, artistName);
    }
}
