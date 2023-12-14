package com.project.petbreedapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ProbabilityAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> values;

    public ProbabilityAdapter(Context context, List<String> values) {
        super(context, R.layout.item_class_values, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.item_class_values, parent, false);

        TextView textView = rowView.findViewById(R.id.tvClass);
        textView.setText(values.get(position));

        return rowView;
    }
}


