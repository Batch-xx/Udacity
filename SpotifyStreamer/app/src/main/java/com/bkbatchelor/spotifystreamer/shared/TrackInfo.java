package com.bkbatchelor.spotifystreamer.shared;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Groups teack information, such as , track name, album name and image
 */
public class TrackInfo implements Parcelable {
    public static final Parcelable.Creator<TrackInfo> CREATOR = new Creator<TrackInfo>() {
        @Override
        public TrackInfo createFromParcel(Parcel source) {
            return new TrackInfo(source);
        }

        @Override
        public TrackInfo[] newArray(int size) {
            return new TrackInfo[size];
        }
    };
    private String trackName;
    private String albumName;
    private String image;
    private String imageLarge;
    private int popularity = 0;
    private String previewUrl = "";
    private long mDuration = 0;

    public TrackInfo(String albumName, String trackName, String imageThumbnail, String imageLarge,
                     int popularity, String previewUrl, long duration) {
        this.albumName = albumName;
        this.trackName = trackName;
        this.imageLarge = imageLarge;
        this.image = imageThumbnail;
        this.popularity = popularity;
        this.previewUrl = previewUrl;
        this.mDuration = duration;
    }

    public TrackInfo(Parcel parcel) {
        trackName = parcel.readString();
        albumName = parcel.readString();
        image = parcel.readString();
        imageLarge = parcel.readString();
        popularity = parcel.readInt();
        previewUrl = parcel.readString();
        mDuration = parcel.readLong();
    }

    public String getTrackName() {
        return trackName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getImage() {
        return image;
    }

    public String getImageLarge() {return imageLarge;}

    public int getPopularity(){return popularity;}

    public String getPreviewUrl() {
        return previewUrl;
    }

    public long getDuration() {return mDuration;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(albumName);
        dest.writeString(trackName);
        dest.writeString(image);
        dest.writeString(imageLarge);
        dest.writeInt(popularity);
        dest.writeString(previewUrl);
        dest.writeLong(mDuration);
    }
}
