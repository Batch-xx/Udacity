package com.bkbatchelor.myappprotfolio;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Click Listeners
    public void onClickSpotify(View view){
        displayToast(view);
    }

    public void onClickScoreApp(View view){
        displayToast(view);
    }

    public void onClickLibraryApp(View view){
        displayToast(view);
    }

    public void onClickBuildItBiggerApp(View view){
        displayToast(view);
    }

    public void onClickBaconReaderApp(View view) {
        displayToast(view);
    }

    public void onClickCapstoneApp(View view) {
        displayToast(view);
    }

    public void displayToast(View view) {

        Button button = (Button) view;

        String buttonText = (String) button.getText();

        Context context = getApplicationContext();
        // open_app would add "Opens the app "
        CharSequence text = getString(R.string.open_app)
                + buttonText;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
