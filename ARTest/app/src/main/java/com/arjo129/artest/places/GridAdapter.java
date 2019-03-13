package com.arjo129.artest.places;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.arjo129.artest.ARActivity;
import com.arjo129.artest.R;

/**
 * Done by Chelsey
 */
public class GridAdapter extends BaseAdapter{
    Context context;
    private final String[] values;
    private final int[] images;
    View view;
    LayoutInflater layoutInflater;

    public GridAdapter(Context context, String[] values, int[] images){
        this.context = context;
        this.values = values;
        this.images = images;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }


    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(view == null){
            view = new View(context);
            view = layoutInflater.inflate(R.layout.grid_item, null);
            ImageButton imageButton = (ImageButton) view.findViewById(R.id.grid_imageButton);
            TextView textview = (TextView)view.findViewById(R.id.grid_text);
            imageButton.setImageResource(images[i]);
            textview.setText(values[i]);

            if(i == 0){
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent favsAct = new Intent(context,PlacesLectureTheatres.class);
                        context.startActivity(favsAct);
                    }
                });
            }
            if(i == 1){
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent ARMode = new Intent(context,ARActivity.class);
                        context.startActivity(ARMode);
                    }
                });
            }
        }
        return view;
    }
}
