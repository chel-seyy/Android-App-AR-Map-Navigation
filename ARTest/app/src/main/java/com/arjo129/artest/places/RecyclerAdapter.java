package com.arjo129.artest.places;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.arjo129.artest.CollectData;
import com.arjo129.artest.MapActivity;
import com.arjo129.artest.R;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Done by Chelsey
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private static final String TAG = "RECYCLER_ADAPTER";
    private static final String BASEMENT = "Basement";
    private static final String LEVEL_1 = "Level 1";
    private static final String LEVEL_2 = "Level 2";
    private static final String[] levelNames = {BASEMENT, LEVEL_1, LEVEL_2};
    private ListItemClickListener mOnClickListener;
    private Set<PlaceSearch> list;
    private Context context;
    private Toast mToast;
    private int level;

    public RecyclerAdapter(Context context, Set<PlaceSearch>list, int level){
        this.context = context;
        this.list = list;
        this.level = level;
    }

    public RecyclerAdapter(Context context, Set<PlaceSearch>list){
        this.context = context;
        this.list = list;
        this.level = -1;
    }

    /*
    List Item Click Listener
     */
    public interface ListItemClickListener{
        void onListItemClick(View v, int index);

    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView place = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.lt_layout, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(place);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        PlaceSearch place = (PlaceSearch)list.toArray()[position];
        if (level == -1){
            holder.place.setText(place.place_name + "   (" + levelNames[place.level] + ")");
        } else {
            holder.place.setText(place.place_name);
        }
        holder.setItemClickListener(new ListItemClickListener() {
            @Override
            public void onListItemClick(View v, int index) {
                PlaceSearch placeClick = (PlaceSearch)list.toArray()[index];
                LatLng latLng = placeClick.coordinate;
                Intent mapActivity = new Intent(context, MapActivity.class);
                mapActivity.putExtra("lat", latLng.getLatitude());
                mapActivity.putExtra("lng", latLng.getLongitude());
                mapActivity.putExtra("place_name", placeClick.place_name);
                mapActivity.putExtra("level", placeClick.level);
//                Log.d(TAG, placeClick.place_name + " at " + placeClick.level);
                context.startActivity(mapActivity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder
                        implements OnClickListener{

        TextView place;
        ListItemClickListener clickListener;

        public MyViewHolder(View itemView) {
            super(itemView);
            place = (TextView)itemView.findViewById(R.id.lt_place);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOnClickListener.onListItemClick(view, getLayoutPosition());
        }
        public void setItemClickListener(ListItemClickListener listener){
            mOnClickListener = listener;
        }
    }

    public void updateList(Set<PlaceSearch> newList){
        list = new LinkedHashSet<>();
        list.addAll(newList);
        notifyDataSetChanged();
    }

}
