package com.example.assignment2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PlacesAutoCompleteAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private List<PlacesHelper.PlaceResult> places;
    private List<PlacesHelper.PlaceResult> filteredPlaces;
    private PlacesHelper placesHelper;
    private LayoutInflater inflater;

    public PlacesAutoCompleteAdapter(Context context, PlacesHelper placesHelper) {
        this.context = context;
        this.placesHelper = placesHelper;
        this.places = new ArrayList<>();
        this.filteredPlaces = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredPlaces.size();
    }

    @Override
    public PlacesHelper.PlaceResult getItem(int position) {
        return filteredPlaces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_place_suggestion, parent, false);
            holder = new ViewHolder();
            holder.placeName = convertView.findViewById(R.id.text_place_name);
            holder.placeAddress = convertView.findViewById(R.id.text_place_address);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PlacesHelper.PlaceResult place = getItem(position);
        if (place != null) {
            // Set place name (primary text)
            if (place.getName() != null && !place.getName().isEmpty()) {
                holder.placeName.setText(place.getName());
                holder.placeName.setVisibility(View.VISIBLE);
            } else {
                holder.placeName.setVisibility(View.GONE);
            }

            // Set place address (secondary text)
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                holder.placeAddress.setText(place.getAddress());
                holder.placeAddress.setVisibility(View.VISIBLE);
            } else {
                holder.placeAddress.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                
                if (constraint == null || constraint.length() < 2) {
                    results.values = new ArrayList<PlacesHelper.PlaceResult>();
                    results.count = 0;
                    return results;
                }

                // This will be handled by the PlacesHelper search
                // We return empty results here and update via callback
                results.values = new ArrayList<PlacesHelper.PlaceResult>();
                results.count = 0;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values != null) {
                    filteredPlaces = (List<PlacesHelper.PlaceResult>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    // Method to update results from PlacesHelper
    public void updateResults(List<PlacesHelper.PlaceResult> newPlaces) {
        this.places.clear();
        this.places.addAll(newPlaces);
        this.filteredPlaces.clear();
        this.filteredPlaces.addAll(newPlaces);
        notifyDataSetChanged();
    }

    // Method to clear results
    public void clearResults() {
        this.places.clear();
        this.filteredPlaces.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView placeName;
        TextView placeAddress;
    }
} 