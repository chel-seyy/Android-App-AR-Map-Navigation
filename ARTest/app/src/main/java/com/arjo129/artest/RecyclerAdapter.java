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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;



public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private static final String TAG = "RECYCLER_ADAPTER";
    private ListItemClickListener mOnClickListener;
    private List<String> list;
    private Context context;
    private Toast mToast;

    public RecyclerAdapter(Context context, List<String>list){
        this.context = context;
        this.list = list;
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
        holder.place.setText(list.get(position));
        holder.setItemClickListener(new ListItemClickListener() {
            @Override
            public void onListItemClick(View v, int index) {
                if (mToast != null) {
                    mToast.cancel();
                }
                Log.d(TAG, list.get(position)+" at position: "+index);
                String toastMessage = "Going to: " + list.get(index);
                mToast = Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT);

                mToast.show();
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

    public void updateList(List<String> newList){
        list = new ArrayList<>();
        list.addAll(newList);
        notifyDataSetChanged();
    }
}
