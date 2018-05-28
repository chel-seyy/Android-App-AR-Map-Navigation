package com.arjo129.artest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;

public class PlacesToGo extends AppCompatActivity {

    private GridView gridView;
    String[] places = {
            "Favourites",
            "Recent",
            "Lecture Theatres"
    };
    int[] images = {
            R.mipmap.ic_star,
            R.mipmap.ic_recent_books,
            R.mipmap.ic_location
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_to_go);

        gridView = (GridView)findViewById(R.id.grid_view);

        GridAdapter gridAdapter = new GridAdapter(this, places, images);
        gridView.setAdapter(gridAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem loginItem = menu.findItem(R.id.activity_login);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.activity_login){
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        }
        if(item.getItemId() == R.id.activity_compass){
            Intent compassIntent = new Intent(this, CompassActivity.class);
            startActivity(compassIntent);
        }
        if(item.getItemId() == R.id.activity_wifi){
            Intent wifiIntent = new Intent(this, WifiActivity.class);
            startActivity(wifiIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
