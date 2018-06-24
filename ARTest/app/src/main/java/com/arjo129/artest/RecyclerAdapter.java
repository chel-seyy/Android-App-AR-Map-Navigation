package com.arjo129.artest;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;



public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private static final String TAG = "RECYCLER_ADAPTER";
    private ListItemClickListener mOnClickListener;
    private List<PlaceSearch> list;
    private Context context;
    private Toast mToast;
    private int size;

    public RecyclerAdapter(Context context, List<PlaceSearch>list, int size){
        this.context = context;
        this.list = list;
        this.size = size;
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
        String text = list.get(position).place_name + " - level "+list.get(position).level;
        holder.place.setText(text);
//        holder.place.setText(list.get(position));
        holder.setItemClickListener(new ListItemClickListener() {
            @Override
            public void onListItemClick(View v, int index) {
                if (mToast != null) {
                    mToast.cancel();
                }
//                Log.d(TAG, list.get(position)+" at position: "+index);
//                String toastMessage = "Going to: " + list.get(index);
//                mToast = Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
                LatLng latLng = list.get(index).coordinate;
                Intent mapActivity = new Intent(context,CollectData.class);
                mapActivity.putExtra("lat", latLng.getLatitude());
                mapActivity.putExtra("lng", latLng.getLongitude());
                mapActivity.putExtra("place_name", list.get(index).place_name);
                context.startActivity(mapActivity);

            }
        });
    }

    @Override
    public int getItemCount() {
        return size;
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

    public void updateList(List<PlaceSearch> newList){
        list = new ArrayList<>();
        list.addAll(newList);
        notifyDataSetChanged();
    }
}
