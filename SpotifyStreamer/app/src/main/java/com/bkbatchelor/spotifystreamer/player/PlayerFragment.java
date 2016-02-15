package com.bkbatchelor.spotifystreamer.player;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bkbatchelor.spotifystreamer.R;

import com.bkbatchelor.spotifystreamer.shared.Helper;
import com.bkbatchelor.spotifystreamer.shared.PlayState;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentPlayerListener} interface
 * to handle interaction events.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends DialogFragment{
    private View rootView = null;
    //UI Elements
    private ImageView mPlayButton = null;
    private ImageView mNextButton = null;
    private ImageView mPreviousButton = null;
    private SeekBar mScrubBar = null;
    private TextView mDurationTextView = null;
    private TextView mTimePlayedView = null;

    private String mPreviewUrl = "";
    private MediaService mMediaService;
    private int mPosition;
    private ArrayList<TrackInfo> mTrackInfos = null;
    private OnFragmentPlayerListener mListener;
    private String mArtist = null;
    private String mFormatedDuration;
    private String mFormatterPlayed;
    private boolean mIsPlaying = true;
//    private ProgressDialog mMusicLoadingDialog;
    private boolean mIsCompletedPlaying;
    private boolean mIsTwoPane;

    /**
     * Broadcast receiver
     */
    private BroadcastReceiver completedPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsCompletedPlaying = intent.getBooleanExtra("completedPlaying",false);
            isCompletePlaying(mIsCompletedPlaying);
        }
    };
    private IntentFilter completedPlayingFilter = new IntentFilter(MediaService.IS_COMPLETED_PLAYING_EVENT);

    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("currentPosition", 0);
            setTrackProgress(progress);
        }
    };
    private IntentFilter progressFilter = new IntentFilter(MediaService.TRACK_PROGRESS_EVENT);

    private BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra("trackChange", 0);
            initializeTrack(mArtist, mTrackInfos,position,false);
        }
    };
    private IntentFilter trackChangeFilter = new IntentFilter(MediaService.TRACK_CHANGE_EVENT);


    private BroadcastReceiver mPlayStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state  = intent.getStringExtra("playState");

            if (state.equals(PlayState.PLAY.toString())) {
                mPlayButton.setImageResource( R.drawable.ic_pause_black_36dp);
            } else if (state.equals(PlayState.PAUSE.toString())) {
                mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
            } else if (state.equals(PlayState.COMPLETED.toString())) {
                mPlayButton.setImageResource(R.drawable.ic_replay_black_36dp);
            }
        }
    };
    private IntentFilter mPlayStateFilter = new IntentFilter(MediaService.TRACK_STATE_EVENT);

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);

//        //Two pane preference
//        SharedPreferences sharedpreferences  =
//                getActivity().getSharedPreferences(MainActivity.TWO_PANE_PERFERENCE,
//                        Context.MODE_PRIVATE);
//
//        mIsTwoPane = sharedpreferences.getBoolean(MainActivity.IS_TWO_PANE,false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        //Define controller buttons
        mPlayButton = (ImageView)rootView.findViewById(R.id.ss_player_play);
        mNextButton = (ImageView)rootView.findViewById(R.id.ss_player_next);
        mPreviousButton = (ImageView)rootView.findViewById(R.id.ss_player_previous);

        mScrubBar = (SeekBar)rootView.findViewById(R.id.ss_player_seekBar);
        mScrubBar.setMax(30);
        mScrubBar.setEnabled(false);

        mDurationTextView = (TextView)rootView.findViewById(R.id.ss_player_duration);
        TextView mDurationPreviewTextView = (TextView) rootView.findViewById(R.id.ss_player_preview_duration);
        mDurationPreviewTextView.setText("0:30");
        mTimePlayedView = (TextView)rootView.findViewById(R.id.ss_player_time_played);

        if(savedInstanceState != null){
            if(savedInstanceState.getBoolean("isDialogShowing")){
//                mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                        "\n Please wait...", false, false);
            }
            mPosition = savedInstanceState.getInt("position");
            mPreviewUrl = savedInstanceState.getString("previewUrl");
            mTrackInfos = savedInstanceState.getParcelableArrayList("tracklist");
            mArtist = savedInstanceState.getString("artist");
            int mScrubBarPosition = savedInstanceState.getInt("seekPosition");
            mIsPlaying = savedInstanceState.getBoolean("isPlaying");
            mMediaService = MediaService.getMediaServiceInstance();
            if(!mMediaService.isPlaying()) {
                mScrubBar.setEnabled(false);
                mScrubBar.setProgress(mScrubBarPosition);
            }else{
                mScrubBar.setEnabled(true);
            }

            setAlbumArt(mArtist, mTrackInfos.get(mPosition));

            mFormatedDuration = savedInstanceState.getString("trackDuration");
            mDurationTextView.setText("(" + mFormatedDuration + ")");

            mFormatterPlayed = savedInstanceState.getString("trackPlayed");
            mTimePlayedView.setText(mFormatterPlayed);

            initiateButtonState();
            initializeOnClicks();

            MediaService mediaService = MediaService.getMediaServiceInstance();
            if(mediaService != null){
                isCompletePlaying(mediaService.isCompletedPlaying());

                int position = mediaService.getTrackPosition();
                initializeTrack(mArtist, mTrackInfos, position, false);

                String state = mediaService.getPlayState();

                if (state.equals(PlayState.PLAY.toString())) {
                    mPlayButton.setImageResource( R.drawable.ic_pause_black_36dp);
                } else if (state.equals(PlayState.PAUSE.toString())) {
                    mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
                } else if (state.equals(PlayState.COMPLETED.toString())) {
                    mPlayButton.setImageResource(R.drawable.ic_replay_black_36dp);
                }
            }
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(completedPlayingReceiver, completedPlayingFilter);
        getActivity().registerReceiver(progressReceiver, progressFilter);
        getActivity().registerReceiver(mPlayStateReceiver, mPlayStateFilter);
        getActivity().registerReceiver(trackChangeReceiver, trackChangeFilter);
    }


    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(completedPlayingReceiver);
        getActivity().unregisterReceiver(progressReceiver);
        getActivity().unregisterReceiver(mPlayStateReceiver);
        getActivity().unregisterReceiver(trackChangeReceiver);
    }

    public void initializeMediaService(){
        if(mMediaService == null) {
            mMediaService = MediaService.getMediaServiceInstance();
            initiateButtonState();
            initializeOnClicks();
            mMediaService.playPreview(mArtist, mPosition,mTrackInfos);
//            mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                    "\n Please wait...", false, false);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("previewUrl", mPreviewUrl);
        outState.putInt("position", mPosition);
        outState.putParcelableArrayList("tracklist", mTrackInfos);
        outState.putString("artist", mArtist);
        outState.putString("trackDuration", mFormatedDuration);
        outState.putString("trackPlayed", mFormatterPlayed);
        outState.putBoolean("isPlaying", mIsPlaying);
//        if(mMusicLoadingDialog != null) {
//            outState.putBoolean("isDialogShowing", mMusicLoadingDialog.isShowing());
//        }
        super.onSaveInstanceState(outState);
    }


    public void initializeTrack(String artistName, ArrayList<TrackInfo> trackList, int position,
                                boolean doPlayTrack){
        mTrackInfos = trackList;
        mPosition = position;
        TrackInfo track = mTrackInfos.get(position);
        mPreviewUrl = track.getPreviewUrl();

        setTrackDuration(track.getDuration());

        setAlbumArt(artistName, track);

        mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);

        mMediaService = MediaService.getMediaServiceInstance();
        if(mMediaService != null){ //Check if media service has initial started

            if(doPlayTrack) {
                mMediaService.playPreview(mArtist, mPosition, mTrackInfos);
//                mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                        "\n Please wait...", false, false);
            }
            initiateButtonState();
            initializeOnClicks();
        }
    }

    private  void setTrackDuration(long duration){
        long mDurationMS = duration;
        mFormatedDuration = Helper.timeFormatter(duration);
        mDurationTextView.setText("(" + mFormatedDuration + ")");

        //Initialize time played
        mFormatterPlayed = "0:00";
        mTimePlayedView.setText(mFormatterPlayed);
    }

    private void setAlbumArt(String artistName, TrackInfo track)
    {
        mArtist = artistName;

        ImageView artView = (ImageView)rootView.findViewById(R.id.ss_player_album_art);
        Picasso.with(rootView.getContext())
                .load(track.getImageLarge())
                .into(artView);

        ((TextView)rootView.findViewById(R.id.ss_artist_name)).setText(mArtist);
        ((TextView)rootView.findViewById(R.id.ss_album_name)).setText(track.getAlbumName());
        ((TextView)rootView.findViewById(R.id.ss_track_name)).setText(track.getTrackName());
    }

    private void initiateButtonState(){
        if(mMediaService.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
        }else if(mIsCompletedPlaying){
            mPlayButton.setImageResource(R.drawable.ic_replay_black_36dp);
        }
        else if(mMediaService.isPaused()){
            mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
        }
    }

    private void initializeOnClicks(){
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaService.isPlaying()) {
                    mMediaService.pausePreview();
                    mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
                } else if (mMediaService.isPaused()) {
                    mMediaService.continuePreview();
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                } else if (mIsCompletedPlaying) {
                    mMediaService.playPreview(mArtist,mPosition, mTrackInfos);
//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);
                    mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
//                    mIsCompletedPlaying = false;
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                } else {
                    mMediaService.playPreview(mArtist, mPosition, mTrackInfos);
//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                }
//                mIsPlaying = true;
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mPosition + 1) < mTrackInfos.size()) {
                    TrackInfo trackInfo = mTrackInfos.get(mMediaService.getTrackPosition() + 1);
                    mPreviewUrl = trackInfo.getPreviewUrl();
                    mMediaService.playPreview(mArtist, mPosition + 1, mTrackInfos);

//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);

                    setTrackDuration(trackInfo.getDuration());
                    mPosition++;
                    updateButtonStatus();

                    setAlbumArt(mArtist, trackInfo);
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                    mScrubBar.setProgress(0);
                    mScrubBar.setEnabled(false);
//                    mIsPlaying = false;
                }else{
                    Toast.makeText(getActivity(),"No next track", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((mPosition - 1) >= 0) {
                    TrackInfo trackInfo = mTrackInfos.get(mPosition - 1);
                    mPreviewUrl = trackInfo.getPreviewUrl();

//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);

                    mMediaService.playPreview(mArtist,mPosition - 1,mTrackInfos);
                    setTrackDuration(trackInfo.getDuration());
                    mPosition--;
                    updateButtonStatus();

                    setAlbumArt(mArtist, trackInfo);
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                    mScrubBar.setProgress(0);
                    mScrubBar.setEnabled(false);
                }else{
                    Toast.makeText(getActivity(),"No previous track", Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateButtonStatus();

        mScrubBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //Intentionally Blank
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Intentionally Blank
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mMediaService.setCurrentPosition(progress);
            }
        });
    }


    private void updateButtonStatus(){
        int numberTracks = mTrackInfos.size()-1;
        int lowerBound = mPosition -1;
        int upperBound = mPosition + 1;

        mPreviousButton.setImageResource(R.drawable.ic_skip_previous_black_36dp);
        mNextButton.setImageResource(R.drawable.ic_skip_next_black_36dp);

        if(numberTracks == 0){
            mPreviousButton.setImageResource(R.drawable.ic_skip_previous_grey600_36dp);
            mNextButton.setImageResource(R.drawable.ic_skip_next_grey600_36dp);
        }else if(upperBound > numberTracks){
            mPreviousButton.setImageResource(R.drawable.ic_skip_previous_black_36dp);
            mNextButton.setImageResource(R.drawable.ic_skip_next_grey600_36dp);
        }else if(lowerBound < 0){
            mPreviousButton.setImageResource(R.drawable.ic_skip_previous_grey600_36dp);
            mNextButton.setImageResource(R.drawable.ic_skip_next_black_36dp);
        }
    }

    public void sendToActivity(Uri uri) {
        if (mListener != null) {
            mListener.onPlayerInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentPlayerListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentPlayerListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public void isCompletePlaying(boolean isCompleted) {
        if(isCompleted){
//            mIsPlaying = false;
//            mIsCompletedPlaying = true;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPlayButton.setImageResource(R.drawable.ic_replay_black_36dp);
                    String formattedTime = Helper.timeFormatter(0);
                    mTimePlayedView.setText(formattedTime);
                    mScrubBar.setProgress(0);
                    mScrubBar.setEnabled(false);
                }
            });

        }
    }

    public void setTrackProgress(final int progress) {


        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mScrubBar.setEnabled(true);
                    mScrubBar.setProgress(progress);

                    String formattedTime = Helper.timeFormatter(progress);
                    mTimePlayedView.setText(formattedTime);
//                    if(mMusicLoadingDialog != null) {
//                        mMusicLoadingDialog.dismiss();
//                    }
                }
            });
        }

    }

    public String getArtist(){
        return mArtist;
    }

    public String getAlbumName(){
        TrackInfo info = mTrackInfos.get(mPosition);
        return info.getAlbumName();
    }

    public String getTrackName(){
        TrackInfo info = mTrackInfos.get(mPosition);
        return info.getTrackName();
    }

    public String getPreviewUrl(){
        TrackInfo info = mTrackInfos.get(mPosition);
        return info.getPreviewUrl();
    }

    public interface OnFragmentPlayerListener {
        // TODO: Update argument type and name
        void onPlayerInteraction(Uri uri);
    }

}
