package com.arjo129.artest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
}
