package barqsoft.footballscores;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import barqsoft.footballscores.service.FootballWidgetService;

/**
 * Created by brian.batchelor on 11/27/2015.
 */
public class FootballScoresAppWidgetProvider extends AppWidgetProvider{

    public static final String TOAST_ACTION = "barqsoft.footballscoresFootballScoresAppWidget.TOAST_ACTION";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Updated all widget with remote adapter
        for(int i=0;i<appWidgetIds.length; i++){
            //Here we setup the intent to point to the FootballWidgetService, which will
            //provide the views for this collection.
            Intent intent = new Intent(context, FootballWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            //When intents are compared the extras are ignored. so we need to embed the extras
            // into the data so that the extras will not be ignored
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.scores_widget);
            rv.setRemoteAdapter(appWidgetIds[i], R.id.widget_list, intent);

            //The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(R.id.widget_list,R.id.empty_view);

            // Here we setup a pending intent template. Individuals items of a collection
            // cannot setup their own pending intents, instead, the collection as a whole can
            // setup a pending intent template and the individual items can set a fillInIntent
            // to create unique before on an item to item basis
            Intent toastIntent = new Intent(context, FootballScoresAppWidgetProvider.class);
            toastIntent.setAction(FootballScoresAppWidgetProvider.TOAST_ACTION);
            toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_list, toastPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i],rv);
        }
        super.onUpdate(context,appWidgetManager,appWidgetIds);
    }
}
