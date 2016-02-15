package com.bkbatchelor.spotifystreamer.shared;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Groups artist information, such as, SpotifyID, artistName and album image
 */
public class ArtistInfo implements Parcelable {
    String spotifyArtistID = "";
    String artistName = "";
    String artistImageStr = "";
    int popularity = 0;


    public ArtistInfo(String spotifyArtistID, String artistName, String artistImageStr, int popularity) {
        this.spotifyArtistID = spotifyArtistID;
        this.artistName = artistName;
        this.artistImageStr = artistImageStr;
        this.popularity = popularity;
    }

    public ArtistInfo(Parcel parcel) {
        spotifyArtistID = parcel.readString();
        artistName = parcel.readString();
        artistImageStr = parcel.readString();
        popularity = parcel.readInt();
    }

    public String getArtistImages() {
        return artistImageStr;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getSpotifyArtistID() {
        return spotifyArtistID;
    }

    public int getPopularity(){return popularity;}


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(spotifyArtistID);
        dest.writeString(artistName);
        dest.writeString(artistImageStr);
        dest.writeInt(popularity);
    }

    public static final Parcelable.Creator<ArtistInfo> CREATOR = new Creator<ArtistInfo>() {
        @Override
        public ArtistInfo createFromParcel(Parcel source) {
            return new ArtistInfo(source);
        }

        @Override
        public ArtistInfo[] newArray(int size) {
            return new ArtistInfo[size];
        }
    };
}
