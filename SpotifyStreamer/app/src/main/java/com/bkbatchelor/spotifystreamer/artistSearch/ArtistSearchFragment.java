package com.bkbatchelor.spotifystreamer.artistSearch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bkbatchelor.spotifystreamer.R;
import com.bkbatchelor.spotifystreamer.main.MainActivity;
import com.bkbatchelor.spotifystreamer.shared.ArtistInfo;
import com.bkbatchelor.spotifystreamer.shared.Helper;
import com.bkbatchelor.spotifystreamer.shared.SpotifyAdapter;
import com.bkbatchelor.spotifystreamer.topTenTracks.TopTenTracksActivity;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import retrofit.RetrofitError;


/**
*   Performs Spotify query for a selected artist
 */
public class ArtistSearchFragment extends Fragment {
    private SpotifyAdapter mSpotifyAdapter = null;
    private String mArtistQuery = null;
    private View mRootView = null;
    private ArrayList<ArtistInfo> mArtistList = null;
    private SearchView mSearchText = null;
    private FetchSpotifyTask mTask = null;
    private int mSelectedPosition = -1;
    private OnArtistSearchListener mListener;

    @Override
    public void onStop() {
        super.onStop();
        if(mTask != null) {
            mTask.dismissDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mTask != null) {
            mTask.dismissDialog();
        }
    }

    public static ArtistSearchFragment newInstance(String param1, String param2) {
        ArtistSearchFragment fragment = new ArtistSearchFragment();
        return fragment;
    }

    public ArtistSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("artistQuery", mArtistQuery);
        outState.putParcelableArrayList("artistList", mArtistList);
        outState.putInt("position", mSelectedPosition);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_search, container, false);

        if (mSpotifyAdapter == null) {
            mSpotifyAdapter = new SpotifyAdapter(getActivity(),
                    R.layout.list_item_artist,
                    new ArrayList<ArtistInfo>());
        }

        ListView listView = (ListView) mRootView.findViewById(R.id.body_listview);
        listView.setAdapter(mSpotifyAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedPosition = position;

                ArtistInfo artistInfo = (ArtistInfo) mSpotifyAdapter.getData().get(position);

                //Two pane preference
                SharedPreferences sharedpreferences  =
                        getActivity().getSharedPreferences(MainActivity.TWO_PANE_PERFERENCE,
                                Context.MODE_PRIVATE);
                boolean mIsTwoPane = sharedpreferences.getBoolean(MainActivity.IS_TWO_PANE, false);

                if(!mIsTwoPane) {
                    Intent topTenTrackIntent = new Intent(getActivity(), TopTenTracksActivity.class);
                    topTenTrackIntent.putExtra("spotifyId", artistInfo.getSpotifyArtistID());
                    topTenTrackIntent.putExtra("artistName", artistInfo.getArtistName());
                    startActivity(topTenTrackIntent);
                }else{
                    //Call activity method to fire TopTenFragment
                    mListener.onTopTenRequest(artistInfo.getSpotifyArtistID(),
                                              artistInfo.getArtistName());
                }
            }
        });


        //Implement search view
        mSearchText = (SearchView) mRootView.findViewById(R.id.search_text);

        mSearchText.setIconifiedByDefault(false);
        mSearchText.setQueryHint(getResources().getString(R.string.ss_artist_search_hint));
        mSearchText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (isNetworkAvailable()) {
                    mArtistQuery = mSearchText.getQuery().toString();
                    onArtistSearchRequest(mArtistQuery);
                } else {
                    CharSequence text = getResources().getString(R.string.no_internet);
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(getActivity(), text, duration);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    if (layout.getChildCount() > 0) {
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    toast.show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        ImageView searchCancelBtn = (ImageView) mSearchText.findViewById(R.id.search_close_btn);

        searchCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clears listview and search
                mSearchText.setQuery("", false);
                mSpotifyAdapter.clear();
                mSpotifyAdapter.addAll(new ArrayList<ArtistInfo>());
                if (mArtistList != null) {
                    mArtistList.clear();
                }

                ListView listView = (ListView) mRootView.findViewById(R.id.body_listview);
                listView.clearChoices();
            }
        });
        return mRootView;
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            ListView listView = (ListView) mRootView.findViewById(R.id.body_listview);
            mArtistQuery = savedInstanceState.getString("artistQuery");

            if (mSearchText != null && mArtistQuery != null) {
                mSearchText.setQuery(mArtistQuery, false);

                mSpotifyAdapter.clear();
                ArrayList<ArtistInfo> returnList = savedInstanceState
                        .getParcelableArrayList("artistList");
                if (returnList == null) {
                    returnList = new ArrayList<ArtistInfo>();
                }
                mArtistList = returnList;
                mSpotifyAdapter.addAll(returnList);

                int position = savedInstanceState.getInt("position");
                listView.setSelection(position);
            }

        }
    }



    /**
     * Display album's name and image inside a listview
     */
    private void displayAlbum(ArrayList<ArtistInfo> artistInfoList) {
        mArtistList = artistInfoList;

        mSpotifyAdapter.clear();
        mSpotifyAdapter.addAll(artistInfoList);

        ListView listView = (ListView) mRootView.findViewById(R.id.body_listview);
        listView.setSelection(0);
        mSearchText.clearFocus();
    }



    /**
     *  Sends an artist request to Spotify WebAPI
     */
    private void onArtistSearchRequest(String artistName) {

        mTask = new FetchSpotifyTask();
        mTask.execute(artistName);
    }

    /**
     * Checks is device has connectivity
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnArtistSearchListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public interface OnArtistSearchListener {
        public void onTopTenRequest(String spotifyId, String artistName);
    }

    /**
     * Background thread that performs Spotify queries, then returns an ArrayList  that
     * contains artist name and image
     */
    private class FetchSpotifyTask extends AsyncTask<String, Void, ArrayList<ArtistInfo>> {
        private ProgressDialog progressDialog;
        private boolean spotifyConnectError = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(getActivity(), "Loading", "Loading artists. " +
                                                "\n Please wait...", false, true);
        }

        @Override
        protected ArrayList<ArtistInfo> doInBackground(String... params) {
            Image artistImage = null;
            ArrayList<ArtistInfo> artistInfoList = new ArrayList<ArtistInfo>();

            if (params.length > 0 && !params[0].isEmpty()) {
                try {
                    SpotifyApi api = new SpotifyApi();
                    SpotifyService spotify = api.getService();

                    ArtistsPager pager = spotify.searchArtists(params[0]);
                    Pager artistPager = pager.artists;
                    List<Artist> artistItems = artistPager.items;
                    for (Artist artist : artistItems) {
                        List<Image> imageList = artist.images;
                        if (!artist.images.isEmpty()) {
                            artistImage = Helper.preferredThumbnailImageSize(imageList);
                            artistInfoList.add(new ArtistInfo(artist.id, artist.name,
                                    artistImage.url, artist.popularity));
                        } else {
                            artistInfoList.add(new ArtistInfo(artist.id, artist.name, null,
                                    artist.popularity));
                        }
                    }
                } catch (RetrofitError e) {
                    spotifyConnectError=true;
                }
            }
            return artistInfoList;
        }


        @Override
        protected void onPostExecute(ArrayList<ArtistInfo> artistInfoList) {
            super.onPostExecute(artistInfoList);

            if(progressDialog != null){
                progressDialog.dismiss();
            }
            if(!spotifyConnectError) {
                if (!artistInfoList.isEmpty()) {
                    displayAlbum(artistInfoList);
                } else {
                    displayAlbum(new ArrayList<ArtistInfo>());

                    CharSequence text = getResources().getString(R.string.no_artist_found);
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(getActivity(), text, duration);
                    LinearLayout layout = (LinearLayout) toast.getView();
                    if (layout.getChildCount() > 0) {
                        TextView tv = (TextView) layout.getChildAt(0);
                        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    toast.show();
                }

            }else{
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
