package com.bkbatchelor.spotifystreamer.topTenTracks;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.dialog.PlayerDialogFragment;
import com.bkbatchelor.spotifystreamer.main.MainActivity;
import com.bkbatchelor.spotifystreamer.player.MediaService;
import com.bkbatchelor.spotifystreamer.player.PlayerActivity;
import com.bkbatchelor.spotifystreamer.shared.Helper;
import com.bkbatchelor.spotifystreamer.shared.SpotifyAdapter;
import com.bkbatchelor.spotifystreamer.shared.TrackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

/**
 * Performs a Spotify query for top ten track for a particular artist
 */
public class TopTenTracksFragment extends Fragment {
    private SpotifyAdapter spotifyAdapter = null;
    private ArrayList<TrackInfo> trackInfoArray = null;
    private View rootView = null;
    private FetchSpotifyTask task = null;
    private String artistName = "";
    private  String playingArtist = "";
    private ListView listView;
    private boolean mIsTwoPane;

    private BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            int position = intent.getIntExtra("trackChange", 0);
//            listView.clearChoices();
//            listView.setSelected(true);
//            listView.setSelection(position);
        }
    };
    private IntentFilter trackChangeFilter = new IntentFilter(MediaService.TRACK_CHANGE_EVENT);



    public TopTenTracksFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Two pane preference
        SharedPreferences sharedpreferences  =
                getActivity().getSharedPreferences(MainActivity.TWO_PANE_PERFERENCE,
                        Context.MODE_PRIVATE);

        mIsTwoPane = sharedpreferences.getBoolean(MainActivity.IS_TWO_PANE,false);

        rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        spotifyAdapter = new SpotifyAdapter(getActivity(),
                R.layout.list_item_track,
                new ArrayList<TrackInfo>());

        listView = (ListView) rootView.findViewById(R.id.top_ten_body_listview);


        listView.setAdapter(spotifyAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (!mIsTwoPane) {
                    TrackInfo trackInfo = (TrackInfo) spotifyAdapter.getData().get(position);
                    Intent playerIntent = new Intent(getActivity(), PlayerActivity.class);
                    playerIntent.putExtra("track", trackInfo.getTrackName());
                    playerIntent.putExtra("album", trackInfo.getAlbumName());
                    playerIntent.putExtra("artist", artistName);
                    playerIntent.putExtra("position", position);
                    playerIntent.putExtra("previewUrl", trackInfo.getPreviewUrl());
                    playerIntent.putExtra("duration", trackInfo.getDuration());
                    playerIntent.putExtra("playTrack", true);
                    playerIntent.putParcelableArrayListExtra("top10tracks", trackInfoArray);

                    startActivity(playerIntent);
                } else {

                    PlayerDialogFragment existingDialog = (PlayerDialogFragment)getActivity()
                                                    .getFragmentManager()
                                                    .findFragmentByTag("player_dialog");

                    if(existingDialog == null) {
                        TrackInfo trackInfo = (TrackInfo) spotifyAdapter.getData().get(position);

                        PlayerDialogFragment playerDialogFragment = new PlayerDialogFragment();
                        playerDialogFragment.setTrackInfo(trackInfo);
                        playerDialogFragment.setArtistName(artistName);
                        playerDialogFragment.setPositon(position);
                        playerDialogFragment.setTrackList(trackInfoArray);
                        playerDialogFragment.playTrack(true);
                        playerDialogFragment.show(getActivity().getFragmentManager(), "player_dialog");
                    }
                }
            }
        });


        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ArrayList<TrackInfo> returnList = savedInstanceState.getParcelableArrayList("trackList");
            if (returnList == null) {
                returnList = new ArrayList<TrackInfo>();
            }else{
                trackInfoArray = returnList;
            }
            spotifyAdapter.addAll(returnList);

            int position = savedInstanceState.getInt("position");
            listView.setSelection(position);

            artistName = savedInstanceState.getString("artist");
            playingArtist = savedInstanceState.getString("playingArtist");
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("trackList", trackInfoArray);

        int position = listView.getSelectedItemPosition();

        outState.putInt("position", position);
        outState.putString("artist", artistName);
        outState.putString("playingArtist", playingArtist);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(trackChangeReceiver,trackChangeFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(trackChangeReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(task != null) {
            task.dismissDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(task != null) {
            task.dismissDialog();
        }
    }

    public void onTrackRequest(String spotifyId, String artistName) {

        this.artistName = artistName;
        FetchSpotifyTask task = new FetchSpotifyTask();
        task.execute(spotifyId);
    }

    private void displayTrack(ArrayList<TrackInfo> trackInfoList) {

        MediaService mediaService = MediaService.getMediaServiceInstance();
        if(mediaService != null){
            playingArtist = mediaService.getArtist();
            if(playingArtist.equals(artistName)){
                int position = mediaService.getTrackPosition();
                listView.setItemChecked(position, true);
            }else{
                listView.clearChoices();
            }
        }
        trackInfoArray = trackInfoList;
        spotifyAdapter.clear();
        spotifyAdapter.addAll(trackInfoList);
//        spotifyAdapter.notifyDataSetChanged();


    }

    private class FetchSpotifyTask extends AsyncTask<String, Void, ArrayList<TrackInfo>> {
        private ProgressDialog progressDialog = null;
        private boolean spotifyConnectError = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(getActivity(),"Please wait ...",
                    "Downloading Tracks ...", true);
        }

        @Override
        protected ArrayList<TrackInfo> doInBackground(String... params) {
            ArrayList<TrackInfo> trackInfoList = new ArrayList<TrackInfo>();

            if (params.length > 0) {
                if (params.length > 0 && !params[0].isEmpty()) {
                    try {
                        SpotifyApi api = new SpotifyApi();
                        SpotifyService spotify = api.getService();

                        HashMap<String, Object> countryMap = new HashMap<>();
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        String countryCode = sharedPreferences.getString("country_code","US");
                        countryMap.put("country",countryCode);
                        Tracks tracks = spotify.getArtistTopTrack(params[0], countryMap);
                        for (Track track : tracks.tracks) {
                            try {
                                List<Image> listImage = track.album.images;
                                trackInfoList.add(new TrackInfo(track.album.name,
                                        track.name,
                                        listImage.isEmpty()? null : Helper.preferredThumbnailImageSize(listImage).url,
                                        listImage.isEmpty()? null : Helper.preferredLargeImageSize(listImage).url,
                                        track.popularity,
                                        track.preview_url,
                                        track.duration_ms));
                            }catch(Exception e){
                                Log.e("Track List Error", e.toString());
                            }
                        }
                    } catch (RetrofitError e) {
                        spotifyConnectError = true;
                    }
                }
            }
            return trackInfoList;
        }


        @Override
        protected void onPostExecute(ArrayList<TrackInfo> trackInfos) {
            super.onPostExecute(trackInfos);

            if(progressDialog != null){
                progressDialog.dismiss();
            }
            if(!spotifyConnectError){
                if(!trackInfos.isEmpty()){
                    displayTrack(trackInfos);
                }else {
                    displayTrack(new ArrayList<TrackInfo>());
                    CharSequence text = getResources().getString(R.string.no_track_found);

                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    //Center text toast
                    if (layout.getChildCount() > 0) {
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    toast.show();
                }
            }
            else {
                CharSequence text = getResources().getString(R.string.request_error);
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(getActivity(), text, duration);
                LinearLayout layout = (LinearLayout) toast.getView();
                if (layout.getChildCount() > 0) {
                    TextView tv = (TextView) layout.getChildAt(0);
                    tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                }
                toast.show();
            }
        }

        public void dismissDialog(){
            if(progressDialog != null){
                progressDialog.dismiss();
            }
        }
    }
}
