package barqsoft.footballscores.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.FootballScoresAppWidgetProvider;

/**
 * Created by brian.batchelor on 11/30/2015.
 */
public class TodayScoresIntentService extends IntentService {

    private static final String[] SCORES_COLUMNS ={
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.MATCH_DAY,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.LEAGUE_COL,
            DatabaseContract.scores_table.TIME_COL
    };
    public TodayScoresIntentService() {
        super("TodayScoresIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                                                FootballScoresAppWidgetProvider.class));
    }
}
