package com.bkbatchelor.spotifystreamer.perferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.bkbatchelor.spotifystreamer.R;

public class SpotifySettingsFragment extends PreferenceFragment {



    public SpotifySettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.perferences);
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
