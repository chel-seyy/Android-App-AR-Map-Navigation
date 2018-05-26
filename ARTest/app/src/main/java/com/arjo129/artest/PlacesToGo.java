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
            R.drawable.baseline_star_black_18dp,
            R.drawable.baseline_library_books_black_18dp,
            R.drawable.baseline_location_on_black_18dp
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
        return super.onOptionsItemSelected(item);
    }
}
