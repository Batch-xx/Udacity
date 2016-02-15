package com.bkbatchelor.spotifystreamer.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.player.MediaService;
import com.bkbatchelor.spotifystreamer.shared.Helper;
import com.bkbatchelor.spotifystreamer.shared.PlayState;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerDialogFragment extends DialogFragment {

    private String mPreviewUrl = "";
    private TrackInfo mTrackInfo;

    private ImageView mPreviousButton = null;
    private ImageView mPlayButton = null;
    private ImageView mNextButton = null;
    private SeekBar mScrubBar = null;

    private TextView mTimePlayedView = null;
    private TextView mDurationTextView = null;
    private String mFormatedDuration;
    private String mFormatterPlayed;

    private ImageView mCloseBox = null;

    private ArrayList<TrackInfo> mTrackInfos;
    private String mArtist = "";
    private int mPosition;

    private boolean mDoPlayTrack = true;

    private MediaService mMediaService;
    private  ProgressDialog mMusicLoadingDialog;
    private boolean mIsCompletedPlaying;

    private View mRootView;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initializeMediaService();
        }
    };
    private IntentFilter filter = new IntentFilter(MediaService.THREAD_RUNNING_EVENT);

    private BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int trackPosition = intent.getIntExtra("trackChange",0);
            setAlbumArt(mArtist, mTrackInfos.get(trackPosition));
        }
    };
    private IntentFilter trackChangeFilter = new IntentFilter(MediaService.TRACK_CHANGE_EVENT);


    private BroadcastReceiver trackProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("currentPosition", 0);
            setTrackProgress(progress);
        }
    };
    private IntentFilter trackProgressFilter = new IntentFilter(MediaService.TRACK_PROGRESS_EVENT);

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

    private BroadcastReceiver completedPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsCompletedPlaying = intent.getBooleanExtra("completedPlaying",false);
            isCompletePlaying(mIsCompletedPlaying);
        }
    };
    private IntentFilter completedPlayingFilter = new IntentFilter(MediaService.IS_COMPLETED_PLAYING_EVENT);



    public PlayerDialogFragment() {
        // Required empty public constructor

    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);




        if(MediaService.getMediaServiceInstance()== null) {
            Intent serviceForegroundIntent = new Intent();
            serviceForegroundIntent.setClass(getActivity(), MediaService.class);
            getActivity().startService(serviceForegroundIntent);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


        LayoutInflater inflater = getActivity().getLayoutInflater();
        mRootView = inflater.inflate(R.layout.fragment_dialog_player, null);
        builder.setView(mRootView);

        initializeViews();

        if(savedInstanceState != null){
            mDoPlayTrack = savedInstanceState.getBoolean("doPlayTrack");
            mPosition = savedInstanceState.getInt("position");
            mPreviewUrl = savedInstanceState.getString("previewUrl");
            mTrackInfos = savedInstanceState.getParcelableArrayList("tracklist");
            mArtist = savedInstanceState.getString("artist");
            int mScrubBarPosition = savedInstanceState.getInt("seekPosition");
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

            initiatePlayButtonState();
            initializeOnClicks();
        }

        if(MediaService.getMediaServiceInstance() != null) {
            initializeMediaService();
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    private void initializeViews() {
        mScrubBar = (SeekBar) mRootView.findViewById(R.id.ss_player_dialog_seekBar);
        mTimePlayedView = (TextView) mRootView.findViewById(R.id.ss_player_dialog_time_played);
        TextView mPreviewDurationTextView = (TextView) mRootView.findViewById(R.id.ss_player_dialog_preview_duration);
        mPreviewDurationTextView.setText("0:30");
        mDurationTextView = (TextView) mRootView.findViewById(R.id.ss_player_dialog_player_duration);
        mScrubBar.setMax(30);
        mScrubBar.setEnabled(false);

        mPlayButton = (ImageView) mRootView.findViewById(R.id.ss_player_dialog_play);
        mNextButton = (ImageView) mRootView.findViewById(R.id.ss_player_dialog_next);
        mPreviousButton = (ImageView) mRootView.findViewById(R.id.ss_player_dialog_previous);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        if(savedInstanceState != null){
//            if(savedInstanceState.getBoolean("isDialogShowing")){
//                mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                        "\n Please wait...", false, false);
//            }
//
//        }

        mCloseBox = (ImageView)mRootView.findViewById(R.id.ss_player_dialog_close_box);
        mCloseBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismiss();

                //Updates Top Ten Track Listview position
                Intent trackChangeIntent = new Intent();
                trackChangeIntent.setAction(MediaService.TRACK_CHANGE_EVENT);
                trackChangeIntent.putExtra("trackChange", mPosition);
                getActivity().sendBroadcast(trackChangeIntent);
            }
        });
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("doPlayTrack", false);
        outState.putString("previewUrl", mPreviewUrl);
        outState.putInt("position", mPosition);
        outState.putParcelableArrayList("tracklist", mTrackInfos);
        outState.putString("artist", mArtist);
        outState.putString("trackDuration", mFormatedDuration);
        outState.putString("trackPlayed", mFormatterPlayed);
//        if(mMusicLoadingDialog != null) {
//            outState.putBoolean("isDialogShowing", mMusicLoadingDialog.isShowing());
//        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, filter);
        getActivity().registerReceiver(mPlayStateReceiver, mPlayStateFilter);
        getActivity().registerReceiver(completedPlayingReceiver, completedPlayingFilter);
        getActivity().registerReceiver(trackChangeReceiver, trackChangeFilter);
        getActivity().registerReceiver(trackProgressReceiver, trackProgressFilter);
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
//        if (mMusicLoadingDialog != null) {
//                mMusicLoadingDialog.dismiss();
//        }
        super.onDismiss(dialog);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMusicLoadingDialog != null) {
                mMusicLoadingDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(receiver);
        getActivity().unregisterReceiver(mPlayStateReceiver);
        getActivity().unregisterReceiver(completedPlayingReceiver);
        getActivity().unregisterReceiver(trackChangeReceiver);
        getActivity().unregisterReceiver(trackProgressReceiver);
    }

    public void isCompletePlaying(boolean isCompleted) {
        if(isCompleted){
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
    public void setTrackInfo(TrackInfo mTrackInfo) {
        this.mTrackInfo = mTrackInfo;
    }

    public void setArtistName(String mArtistName) {
        this.mArtist = mArtistName;
    }

    public String getArtist() {
        if(mArtist == null){
            return "";
        }else {
            return mArtist;
        }
    }

    public String getTrackName(){

        TrackInfo info = mTrackInfos.get(mPosition);
        if(info == null){
            return "";
        }else {
            return info.getTrackName();
        }
    }
    public void setPositon(int mPositon) {
        this.mPosition = mPositon;
    }

    public void setTrackList(ArrayList<TrackInfo> mTrackList) {
        this.mTrackInfos = mTrackList;
    }

    public void playTrack(boolean playTrack){
        this.mDoPlayTrack = playTrack;
    }

    public void initializeMediaService(){
        mMediaService = MediaService.getMediaServiceInstance();
        if (mDoPlayTrack) {
//            mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                    "\n Please wait...", false, false);

            mMediaService.playPreview(mArtist, mPosition, mTrackInfos);
        }

        initiatePlayButtonState();
        initializeOnClicks();

        TrackInfo track = mTrackInfos.get(mPosition);
        setTrackDuration(track.getDuration());
        setAlbumArt(mArtist, track);

        mDoPlayTrack = true;
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
                    if(mMusicLoadingDialog != null) {
                        mMusicLoadingDialog.dismiss();
                    }
                }
            });
        }
    }

    private  void setTrackDuration(long duration){
        mFormatedDuration = Helper.timeFormatter(duration);
        mDurationTextView.setText("(" + mFormatedDuration + ")");

        //Initialize time played
        mFormatterPlayed = "0:00";
        mTimePlayedView.setText(mFormatterPlayed);
    }

    private void initiatePlayButtonState(){
        if(mMediaService.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
        }else if(mIsCompletedPlaying){
            mPlayButton.setImageResource(R.drawable.ic_replay_black_36dp);
        }
        else if(mMediaService.isPaused()){
            mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
        }
    }

    private void initializeOnClicks() {
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
//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);
                    mMediaService.playPreview(mArtist, mPosition, mTrackInfos);
                    mPlayButton.setImageResource(R.drawable.ic_play_black_36dp);
//                    mIsCompletedPlaying = false;
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                } else {
//                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
//                            "\n Please wait...", false, false);
                    mMediaService.playPreview(mArtist, mPosition, mTrackInfos);
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

                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
                            "\n Please wait...", false, false);
                    mMediaService.playPreview(mArtist, mPosition + 1, mTrackInfos);

                    setTrackDuration(trackInfo.getDuration());
                    mPosition++;
                    updateButtonStatus();

                    setAlbumArt(mArtist, trackInfo);
                    mPlayButton.setImageResource(R.drawable.ic_pause_black_36dp);
                    mScrubBar.setProgress(0);
                    mScrubBar.setEnabled(false);
                }else{
                    Toast.makeText(getActivity(), "No next track", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((mPosition - 1) >= 0) {
                    TrackInfo trackInfo = mTrackInfos.get(mPosition - 1);

                    mMusicLoadingDialog = ProgressDialog.show(getActivity(), "Buffering", "Buffering track. " +
                            "\n Please wait...", false, false);
                    mPreviewUrl = trackInfo.getPreviewUrl();

                    mMediaService.playPreview(mArtist, mPosition - 1, mTrackInfos);
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

    private void setAlbumArt(String artistName, TrackInfo track)
    {
        mArtist = artistName;

        ImageView artView = (ImageView)mRootView.findViewById(R.id.ss_player_dialog_image);
        Picasso.with(mRootView.getContext())
                .load(track.getImageLarge())
                .into(artView);

        ((TextView)mRootView.findViewById(R.id.ss_player_dialog_artist_name)).setText(mArtist);
        ((TextView)mRootView.findViewById(R.id.ss_player_dialog_album_name)).setText(track.getAlbumName());
        ((TextView)mRootView.findViewById(R.id.ss_player_dialog_track_name)).setText(track.getTrackName());
    }



}
