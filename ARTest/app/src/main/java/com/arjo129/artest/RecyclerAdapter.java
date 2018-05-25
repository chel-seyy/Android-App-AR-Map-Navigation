package com.arjo129.artest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    final private ListItemClickListener mOnClickListener;
    private List<String> list;

    public interface ListItemClickListener{
        void onListItemClick(int clickedItemIndex);
    }

    public RecyclerAdapter(List<String>list, ListItemClickListener listener){
        this.list = list;
        mOnClickListener = listener;
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView place = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.lt_layout, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(place);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.place.setText(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder
                        implements OnClickListener{

        TextView place;
        public MyViewHolder(TextView itemView) {
            super(itemView);
            place = itemView;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onListItemClick(clickedPosition);
        }
    }

    public void updateList(List<String> newList){
        list = new ArrayList<>();
        list.addAll(newList);
        notifyDataSetChanged();
    }
}
