package com.bkbatchelor.spotifystreamer.shared;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bkbatchelor.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

/**
 * Custom ArrayAdapter that organizes artist name, albums, track and images
 */
public class SpotifyAdapter<T> extends ArrayAdapter {

    private Context context;
    private int layoutResId;
    private List<T> data = null;

    public SpotifyAdapter(Context context, int layoutResId, List<T> data) {
        super(context, layoutResId, data);
        this.context = context;
        this.layoutResId = layoutResId;
        this.data = data;

    }

    public List<T> getData() {
        return data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArtistHolder holder = null;

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(layoutResId, parent, false);

            holder = new ArtistHolder(convertView);

            convertView.setTag(holder);
        } else {
            holder = (ArtistHolder) convertView.getTag();
        }


        if (!data.isEmpty()) {
            if (layoutResId == R.layout.list_item_artist) {
                ArtistInfo artistInfo = ((List<ArtistInfo>) data).get(position);
//                holder.playIndictorImage.setVisibility(View.INVISIBLE);
                holder.artistNameTextView.setText(artistInfo.getArtistName());
                int pop = artistInfo.getPopularity();
                holder.artistPopularityTextView.setText(String.valueOf(pop));
                if (artistInfo.getArtistImages() != null) {
                    Picasso.with(this.context)
                            .load(artistInfo.getArtistImages())
                            .transform(new CropCircleTransformation())
                            .into(holder.artistImage);
                } else {
                    holder.artistImage.setImageResource(R.drawable.ic_no_image);
                }
            }else if(layoutResId == R.layout.item_track){
                TrackInfo trackInfo = ((List<TrackInfo>) data).get(position);
                holder.smallTrackName.setText(trackInfo.getTrackName());
                holder.smallAlbumName.setText(trackInfo.getAlbumName());
                holder.smallPopularity.setText(String.valueOf(trackInfo.getPopularity()));
                if (trackInfo.getImage() != null) {
                    Picasso.with(this.context)
                            .load(trackInfo.getImage())
                            .transform(new CropCircleTransformation())
                            .into(holder.smallTrackImage);
                } else {
                    holder.artistImage.setImageResource(R.drawable.ic_no_image);
                }

            }else if (layoutResId == R.layout.list_item_track) {
                TrackInfo trackInfo = ((List<TrackInfo>) data).get(position);
                holder.trackNameTextView.setText(trackInfo.getTrackName());
                holder.albumNameTextView.setText(trackInfo.getAlbumName());
                int pop = trackInfo.getPopularity();
                holder.trackPopularityTextView.setText(String.valueOf(pop));
                if (trackInfo.getImage() != null) {
                    Picasso.with(this.context)
                            .load(trackInfo.getImage())
                            .transform(new CropCircleTransformation())
                            .into(holder.artistImage);
                } else {
                    holder.artistImage.setImageResource(R.drawable.ic_no_image);
                }
            }
        }

        return convertView;
    }

    private class ArtistHolder {
        ImageView artistImage;
        TextView artistNameTextView;
        TextView albumNameTextView;
        TextView trackNameTextView;

//        ImageView playIndictorImage;
        TextView trackPopularityTextView;
        TextView artistPopularityTextView;

        ImageView smallTrackImage;
        TextView smallAlbumName;
        TextView smallTrackName;
        TextView smallPopularity;

       public  ArtistHolder(View convertView){
          artistImage = (ImageView) convertView.findViewById(R.id.ss_artist_list_item_image);
           artistNameTextView = (TextView) convertView.findViewById(R.id.ss_artist_list_item_name);
           albumNameTextView = (TextView) convertView.findViewById(R.id.ablum_textView);
           trackNameTextView = (TextView) convertView.findViewById(R.id.track_textView);
//           playIndictorImage = (ImageView)convertView.findViewById(R.id.ss_artist_list_item_playImage);
           trackPopularityTextView = (TextView)convertView.findViewById(R.id.ss_track_list_item_popularity);
           artistPopularityTextView = (TextView)convertView.findViewById(R.id.ss_artist_list_item_popularity);
           smallTrackImage = (ImageView)convertView.findViewById(R.id.ss_track_image);
           smallAlbumName = (TextView)convertView.findViewById(R.id.ss_track_album);
           smallTrackName = (TextView)convertView.findViewById(R.id.ss_track_track);
           smallPopularity = (TextView)convertView.findViewById(R.id.ss_track_popularity);
       }

    }
}
