package barqsoft.footballscores.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by brian.batchelor on 12/1/2015.
 */
public class FootballWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new FootballWidgetRemoteViewFactory(this.getApplicationContext(), intent);
    }
}

class FootballWidgetRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory{

    private Context mContext;
    private int mAppWidgetId;
    private Cursor mCursor;

    public static final int COL_HOME = 0;
    public static final int COL_AWAY = 1;
    public static final int COL_HOME_GOALS = 2;
    public static final int COL_AWAY_GOALS = 3;
    public static final int COL_DATE = 4;
    public static final int COL_LEAGUE = 5;
    public static final int COL_MATCHDAY = 6;
    public static final int COL_MATCHTIME = 7;

    public FootballWidgetRemoteViewFactory(Context context, Intent intent){
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

    }

    @Override
    public void onCreate() {
        String[] projection = {DatabaseContract.scores_table.HOME_COL,
                DatabaseContract.scores_table.AWAY_COL,
                DatabaseContract.scores_table.HOME_GOALS_COL,
                DatabaseContract.scores_table.AWAY_GOALS_COL,
                DatabaseContract.scores_table.DATE_COL,
                DatabaseContract.scores_table.LEAGUE_COL,
                DatabaseContract.scores_table.MATCH_DAY,
                DatabaseContract.scores_table.TIME_COL};


        mCursor =  mContext.getContentResolver().query(DatabaseContract.BASE_CONTENT_URI,
                                                        projection,
                                                        null,null,null);
    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        mCursor.moveToPosition(position);
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.scores_list_item);

        rv.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(mCursor.getString(COL_HOME)));
//        rv.setContentDescription(R.id.home_crest, mCursor.getString(COL_HOME));

        rv.setTextViewText(R.id.home_name, mCursor.getString(COL_HOME));
        rv.setContentDescription(R.id.home_name, mCursor.getString(COL_HOME));

        rv.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(mCursor.getString(COL_AWAY)));
//        rv.setContentDescription(R.id.away_crest,mCursor.getString(COL_AWAY));

        rv.setTextViewText(R.id.away_name, mCursor.getString(COL_AWAY));
        rv.setContentDescription(R.id.away_name, mCursor.getString(COL_AWAY));

        rv.setTextViewText(R.id.score_textview, Utilies.getScores(mCursor.getInt(COL_HOME_GOALS),
                mCursor.getInt(COL_AWAY_GOALS)));
        rv.setContentDescription(R.id.score_textview, Utilies.getScores(mCursor.getInt(COL_HOME_GOALS),
                mCursor.getInt(COL_AWAY_GOALS)));

        rv.setTextViewText(R.id.data_textview, mCursor.getString(COL_MATCHTIME));
        rv.setContentDescription(R.id.data_textview,mCursor.getString(COL_MATCHTIME));

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
