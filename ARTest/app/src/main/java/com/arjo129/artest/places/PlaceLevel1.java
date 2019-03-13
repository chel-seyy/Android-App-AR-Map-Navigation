package com.arjo129.artest.places;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.arjo129.artest.MapActivity;
import com.arjo129.artest.R;

import java.util.List;
import java.util.Set;

/**
 * Done by Chelsey
 */
public class PlaceLevel1 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerAdapter adapter;
    private Set<PlaceSearch> list;
    private SearchPlaces searchPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_level1);
        ActionBar actionBar = this.getSupportActionBar();

        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.rv_level1);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        Intent before = getIntent();
        int level = 0;
        if(before.hasExtra("level")){
            level = getIntent().getIntExtra("level", 0);
            searchPlaces = new SearchPlaces(this, level);
            list = searchPlaces.getPlaces();
        }
        if(level == 0){
            setTitle("Basement");
        }
        else{
            setTitle("Level "+ level);
        }
        adapter = new RecyclerAdapter(this, list, level);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lt, menu);
        MenuItem item = menu.findItem(R.id.action_search_lt);
        item.setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // When the home button is pressed, take the user back to the VisualizerActivity
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        if(id == R.id.go_to_map){
            Intent mapIntent = new Intent(this, MapActivity.class);
            startActivity(mapIntent);
        }
        return super.onOptionsItemSelected(item);
    }

}
