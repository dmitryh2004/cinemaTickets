package com.example.cinematickets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class ShowroomSpinnerAdapter extends ArrayAdapter<Showroom> {
    List<Showroom> showroomList;
    int resource;
    Context context;

    public ShowroomSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<Showroom> objects) {
        super(context, resource, objects);
        this.showroomList = objects;
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);

        TextView tv = (TextView) (convertView.findViewById(R.id.textView));
        Showroom showroom = showroomList.get(position);
        tv.setText(showroom.getName());

        return convertView;
    }
}
