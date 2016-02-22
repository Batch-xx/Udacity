package com.udacity.gradle.builditbigger;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.udacity.gradle.builditbiger.backend.myApi.MyApi;
import com.udacity.gradle.builditbigger.androidlib.JokeActivity;

import java.io.IOException;

/**
 * Created by BKBatchelor on 2/20/2016.
 */
class EndpointsAsyncTask extends AsyncTask<Context, Void, String> {
    private MyApi myApiService = null;
    private Context context = null;
    private JokeTaskListener listener = null;



    @Override
    protected String doInBackground(Context... params) {
        if(myApiService == null) {  // Only do this once
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    // options for running against local devappserver
                    // - 10.0.2.2 is localhost's IP address in Android emulator
                    // - turn off compression when running against local devappserver
                    .setRootUrl("http://10.0.2.2:8080/_ah/api/")
                    .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                        @Override
                        public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                            abstractGoogleClientRequest.setDisableGZipContent(true);
                        }
                    });
            // end options for devappserver

            myApiService = builder.build();
        }

        if(params.length > 0) {
            context = params[0];
        }

        try {
            return myApiService.sayJoke().execute().getData();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String joke) {
        joke = joke != null && !joke.isEmpty() ? joke : "no jokes";

        if(context != null) {
            Intent jokeIntent = new Intent(context, JokeActivity.class);
            jokeIntent.putExtra("joke", joke);
            context.startActivity(jokeIntent);
        }
        if(listener != null){
            this.listener.onComplete(joke);
        }
    }

    public EndpointsAsyncTask setListener(JokeTaskListener listener){
        this.listener = listener;
        return  this;
    }

    public interface JokeTaskListener{
         void onComplete(String joke);
    }
}
