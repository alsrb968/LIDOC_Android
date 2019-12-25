package com.heartsafety.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;

import com.heartsafety.app.databinding.ListItemBinding;

import java.util.ArrayList;

public class BluetoothListAdapter extends BaseAdapter {
    private ListItemBinding binding;
    private LayoutInflater inflater;
    private ArrayList<BluetoothListItem> items;

    public BluetoothListAdapter(Context context, ArrayList<BluetoothListItem> items) {
        this.items = items;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.list_item, parent, false);
            convertView = binding.getRoot();
        } else {
            binding = (ListItemBinding) convertView.getTag();
        }
        convertView.setTag(binding);

        binding.setItem(items.get(position));
        return convertView;
    }
}
