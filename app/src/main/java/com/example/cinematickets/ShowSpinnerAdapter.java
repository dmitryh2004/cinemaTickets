package com.example.cinematickets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class ShowSpinnerAdapter extends ArrayAdapter<Show> {
    List<Show> showList;
    int resource;
    Context context;
    String uid;

    public ShowSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<Show> objects, String uid) {
        super(context, resource, objects);
        this.showList = objects;
        this.context = context;
        this.resource = resource;
        this.uid = uid;
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
        if (uid != null)
            tv.setText(showList.get(position).getShowInfo(uid));
        else
            tv.setText(showList.get(position).toString());

        return convertView;
    }
}
