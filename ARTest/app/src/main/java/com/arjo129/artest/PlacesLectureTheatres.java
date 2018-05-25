package com.arjo129.artest;

import android.content.ClipData;
import android.graphics.Color;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.arjo129.artest.RecyclerAdapter.ListItemClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacesLectureTheatres extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<String> list;
    private Toast mToast;
    private SearchView searchView;
    private RecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        list = Arrays.asList(getResources().getStringArray(R.array.places_lt));
        adapter = new RecyclerAdapter(this, list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lt, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search_lt);
        searchView = (SearchView) searchItem.getActionView();

        changeSearchViewTextColor(searchView);

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
                final List<String>filtermodelist = filter(list, s);
                adapter.updateList(filtermodelist);
                return true;
            }
        });
        return true;
    }

    private List<String>filter(List<String> p1, String query){
        query = query.toLowerCase();
        final List<String> filteredModeList = new ArrayList<>();
        for(String place: p1){
            final String text = place.toLowerCase();
            if(text.startsWith(query)){
                filteredModeList.add(place);
            }
        }
        return filteredModeList;
    }

    private void changeSearchViewTextColor(View view){
        if(view!=null){
            if(view instanceof TextView){
                ((TextView)view).setTextColor(Color.WHITE);
                return;
            }
            else if(view instanceof ViewGroup){
                ViewGroup viewGroup = (ViewGroup)view;
                for(int i=0; i<viewGroup.getChildCount(); i++){
                    changeSearchViewTextColor(viewGroup.getChildAt(i));
                }
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // When the home button is pressed, take the user back to the VisualizerActivity
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }



//    @Override
//    public void onListItemClick(int clickedItemIndex) {
//        if (mToast != null) {
//            mToast.cancel();
//        }
//        String toastMessage = "Going to: " + list.get(clickedItemIndex);
//        mToast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT);
//
//        mToast.show();
//    }

}
