package com.arjo129.artest.places;

import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.SearchView;

import com.arjo129.artest.MapActivity;
import com.arjo129.artest.R;
import com.arjo129.artest.places.PlaceLevel1;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Done by Chelsey
 */
public class PlacesLectureTheatres extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Context mContext;
    private SearchView searchView;
    private RecyclerAdapter adapter;
    private Set<PlaceSearch> list;
    private SearchPlaces searchPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_lecture_theatres);
        ActionBar actionBar = this.getSupportActionBar();

        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.rv_lt);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        searchPlaces = new SearchPlaces(this);
        list = searchPlaces.getPlaces();

        adapter = new RecyclerAdapter(this, list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        Button level0 = findViewById(R.id.button_level0);
        Button level1 = findViewById(R.id.button_level1);
        Button level2 = findViewById(R.id.button_level2);
        level0.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent act_level = new Intent(mContext, PlaceLevel1.class);
                act_level.putExtra("level",(int)0);
                mContext.startActivity(act_level);
            }
        });
        level1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent act_level = new Intent(mContext, PlaceLevel1.class);
                act_level.putExtra("level",(int)1);
                mContext.startActivity(act_level);
            }
        });
        level2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent act_level = new Intent(mContext, PlaceLevel1.class);
                act_level.putExtra("level",(int)2);
                mContext.startActivity(act_level);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lt, menu);
        MenuItem mapButton = menu.findItem(R.id.go_to_map);

        final MenuItem searchItem = menu.findItem(R.id.action_search_lt);
        searchView = (SearchView) searchItem.getActionView();

//        changeSearchViewTextColor(searchView);

        ((EditText)searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)).
                setHintTextColor(getResources().getColor(R.color.white));


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if(!searchView.isIconified())
                    searchView.setIconified(true);
                searchItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                final Set<PlaceSearch>filtermodelist = filter(list, s);
                adapter.updateList(filtermodelist);
                return true;
            }
        });
        return true;
    }

    private Set<PlaceSearch>filter(Set<PlaceSearch> p1, String query){
        query = query.toLowerCase();
        final Set<PlaceSearch> filteredModeList = new LinkedHashSet<>();
        for(PlaceSearch place: p1){
            final String text = place.place_name.toLowerCase();
            if(text.startsWith(query)){
                filteredModeList.add(place);
            }
        }
        return filteredModeList;
    }

//    private void changeSearchViewTextColor(View view){
//        if(view!=null){
//            if(view instanceof TextView){
//                ((TextView)view).setTextColor(Color.WHITE);
//                return;
//            }
//            else if(view instanceof ViewGroup){
//                ViewGroup viewGroup = (ViewGroup)view;
//                for(int i=0; i<viewGroup.getChildCount(); i++){
//                    changeSearchViewTextColor(viewGroup.getChildAt(i));
//                }
//            }
//        }
//    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // When the home button is pressed, take the user back to the VisualizerActivity
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        else if(id == R.id.go_to_map){
            Intent mapIntent = new Intent(this, MapActivity.class);
            startActivity(mapIntent);
        } else if (id == R.id.action_search_lt) {
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
        }
        return super.onOptionsItemSelected(item);
    }


}
