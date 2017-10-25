package com.example.charles.twintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by charl on 25/10/2017.
 */

public class FeedingsAdapter extends ArrayAdapter<feeding> {

    Context context;
    private ArrayList<feeding> feedings;

    public FeedingsAdapter(Context context, int textViewResourceId, ArrayList<feeding> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.feedings = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.feeding, null);
        }
        feeding o = feedings.get(position);
        if (o != null) {
            TextView name = (TextView) v.findViewById(R.id.name);
            TextView start = (TextView) v.findViewById(R.id.start);
            TextView duration = (TextView) v.findViewById(R.id.duration);


            name.setText(String.valueOf(o.getName()));
            start.setText(String.valueOf(o.getStart()));
            duration.setText(String.valueOf(o.getDuration()));
        }
        return v;
    }
}
