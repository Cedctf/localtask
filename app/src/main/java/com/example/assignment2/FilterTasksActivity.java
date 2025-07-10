package com.example.assignment2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FilterTasksActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView clearAllButton;
    private MaterialButton resetFiltersButton, applyFiltersButton;
    
    // Category checkboxes
    private CheckBox categoryCleaningCheckbox, categoryTutoringCheckbox, 
                    categoryDeliveryCheckbox, categorySmallTasksCheckbox;
    
    // Sort spinner
    private Spinner sortBySpinner;
    
    // Distance seekbar
    private SeekBar distanceSeekBar;
    private TextView distanceValueText;
    
    // Payment range radio group
    private RadioGroup paymentRangeGroup;
    
    // Duration checkboxes
    private CheckBox durationQuick, durationHalfDay, durationFullDay, durationMultiDay;
    
    // Complexity checkboxes
    private CheckBox complexityBeginner, complexityExperience, complexityProfessional;
    
    // Area checkboxes
    private CheckBox areaSubangJaya, areaPetalingJaya, areaShahAlam, areaKlangValley;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_tasks);

        initializeViews();
        setupSpinner();
        setupListeners();
        loadCurrentFilters();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        clearAllButton = findViewById(R.id.clearAllButton);
        resetFiltersButton = findViewById(R.id.resetFiltersButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);
        
        // Category checkboxes
        categoryCleaningCheckbox = findViewById(R.id.categoryCleaningCheckbox);
        categoryTutoringCheckbox = findViewById(R.id.categoryTutoringCheckbox);
        categoryDeliveryCheckbox = findViewById(R.id.categoryDeliveryCheckbox);
        categorySmallTasksCheckbox = findViewById(R.id.categorySmallTasksCheckbox);
        
        // Sort spinner
        sortBySpinner = findViewById(R.id.sortBySpinner);
        
        // Distance seekbar
        distanceSeekBar = findViewById(R.id.distanceSeekBar);
        distanceValueText = findViewById(R.id.distanceValueText);
        
        // Payment range radio group
        paymentRangeGroup = findViewById(R.id.paymentRangeGroup);
        
        // Duration checkboxes
        durationQuick = findViewById(R.id.durationQuick);
        durationHalfDay = findViewById(R.id.durationHalfDay);
        durationFullDay = findViewById(R.id.durationFullDay);
        durationMultiDay = findViewById(R.id.durationMultiDay);
        
        // Complexity checkboxes
        complexityBeginner = findViewById(R.id.complexityBeginner);
        complexityExperience = findViewById(R.id.complexityExperience);
        complexityProfessional = findViewById(R.id.complexityProfessional);
        
        // Area checkboxes
        areaSubangJaya = findViewById(R.id.areaSubangJaya);
        areaPetalingJaya = findViewById(R.id.areaPetalingJaya);
        areaShahAlam = findViewById(R.id.areaShahAlam);
        areaKlangValley = findViewById(R.id.areaKlangValley);
    }

    private void setupSpinner() {
        String[] sortOptions = {
            "Most Recent",
            "Highest Payment",
            "Nearest Location",
            "Soonest Deadline",
            "Best Match"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, sortOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        clearAllButton.setOnClickListener(v -> clearAllFilters());
        
        resetFiltersButton.setOnClickListener(v -> clearAllFilters());
        
        applyFiltersButton.setOnClickListener(v -> applyFilters());
        
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceValueText.setText(progress + " km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadCurrentFilters() {
        // Set default values - we'll keep it simple for now
        // In the future, this could load from SharedPreferences
        
        // Set default sort option
        sortBySpinner.setSelection(0);
        
        // Set default distance
        distanceSeekBar.setProgress(10);
        distanceValueText.setText("10 km");
        
        // Set default payment range to "Any Amount"
        paymentRangeGroup.check(R.id.paymentRangeAny);
    }

    private void clearAllFilters() {
        // Clear category filters
        categoryCleaningCheckbox.setChecked(false);
        categoryTutoringCheckbox.setChecked(false);
        categoryDeliveryCheckbox.setChecked(false);
        categorySmallTasksCheckbox.setChecked(false);
        
        // Reset sort to default
        sortBySpinner.setSelection(0);
        
        // Reset distance
        distanceSeekBar.setProgress(10);
        distanceValueText.setText("10 km");
        
        // Reset payment range to "Any Amount"
        paymentRangeGroup.check(R.id.paymentRangeAny);
        
        // Clear duration filters
        durationQuick.setChecked(false);
        durationHalfDay.setChecked(false);
        durationFullDay.setChecked(false);
        durationMultiDay.setChecked(false);
        
        // Clear complexity filters
        complexityBeginner.setChecked(false);
        complexityExperience.setChecked(false);
        complexityProfessional.setChecked(false);
        
        // Clear area filters
        areaSubangJaya.setChecked(false);
        areaPetalingJaya.setChecked(false);
        areaShahAlam.setChecked(false);
        areaKlangValley.setChecked(false);
    }

    private void applyFilters() {
        // Collect filter data
        List<String> categories = new ArrayList<>();
        if (categoryCleaningCheckbox.isChecked()) {
            categories.add("Cleaning");
        }
        if (categoryTutoringCheckbox.isChecked()) {
            categories.add("Tutoring");
        }
        if (categoryDeliveryCheckbox.isChecked()) {
            categories.add("Delivery");
        }
        if (categorySmallTasksCheckbox.isChecked()) {
            categories.add("Small Tasks");
        }
        
        // Get sort option
        String[] sortOptions = {"", "payment", "newest", "distance", "rating"};
        String sortBy = sortOptions[sortBySpinner.getSelectedItemPosition()];
        
        // Get distance
        int distance = distanceSeekBar.getProgress();
        
        // Get payment range
        String paymentRange = getSelectedPaymentRange();
        
        // Collect duration filters
        List<String> durations = new ArrayList<>();
        if (durationQuick.isChecked()) {
            durations.add("Quick");
        }
        if (durationHalfDay.isChecked()) {
            durations.add("Half Day");
        }
        if (durationFullDay.isChecked()) {
            durations.add("Full Day");
        }
        if (durationMultiDay.isChecked()) {
            durations.add("Multi-day");
        }
        
        // Collect complexity filters
        List<String> complexities = new ArrayList<>();
        if (complexityBeginner.isChecked()) {
            complexities.add("Beginner friendly");
        }
        if (complexityExperience.isChecked()) {
            complexities.add("Requires experience");
        }
        if (complexityProfessional.isChecked()) {
            complexities.add("Professional skills needed");
        }
        
        // Collect area filters
        List<String> areas = new ArrayList<>();
        if (areaSubangJaya.isChecked()) {
            areas.add("Subang Jaya");
        }
        if (areaPetalingJaya.isChecked()) {
            areas.add("Petaling Jaya");
        }
        if (areaShahAlam.isChecked()) {
            areas.add("Shah Alam");
        }
        if (areaKlangValley.isChecked()) {
            areas.add("Klang Valley");
        }
        
        // Create FilterCriteria object
        FilterManager.FilterCriteria criteria = new FilterManager.FilterCriteria(
            categories, sortBy, distance, paymentRange, areas, durations, complexities
        );
        
        // Return result to calling activity with criteria
        Intent resultIntent = new Intent();
        resultIntent.putExtra("filters_applied", true);
        resultIntent.putExtra("filter_criteria", criteria);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private int getSortIndex(String sortBy) {
        String[] sortOptions = {"Most Recent", "Highest Payment", "Nearest Location", "Soonest Deadline", "Best Match"};
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortOptions[i].equals(sortBy)) {
                return i;
            }
        }
        return 0; // Default to "Most Recent"
    }



    private String getSelectedPaymentRange() {
        int checkedId = paymentRangeGroup.getCheckedRadioButtonId();
        
        if (checkedId == R.id.paymentRangeAny) {
            return "";
        } else if (checkedId == R.id.paymentRange1) {
            return "RM 10 - RM 50";
        } else if (checkedId == R.id.paymentRange2) {
            return "RM 50 - RM 100";
        } else if (checkedId == R.id.paymentRange3) {
            return "RM 100 - RM 200";
        } else if (checkedId == R.id.paymentRange4) {
            return "RM 200+";
        } else {
            return "";
        }
    }
} 