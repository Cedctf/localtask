package com.example.assignment2;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FilterManager {
    private Context context;
    private Dialog filterDialog;
    private FilterCallback callback;
    
    // Filter state variables
    private List<String> selectedCategories = new ArrayList<>();
    private String selectedSort = "";
    private int selectedDistance = 10; // km
    private String selectedPaymentRange = "";
    private List<String> selectedAreas = new ArrayList<>();
    private List<String> selectedDurations = new ArrayList<>();
    private List<String> selectedComplexities = new ArrayList<>();
    
    // SharedPreferences
    private SharedPreferences filterPrefs;
    private static final String FILTER_PREFS = "filter_preferences";
    
    // UI elements
    private CheckBox categoryCleaningCheckbox, categoryTutoringCheckbox, categoryDeliveryCheckbox, categorySmallTasksCheckbox;
    private Spinner sortBySpinner;
    private SeekBar distanceSeekBar;
    private TextView distanceValueText;
    private RadioGroup paymentRangeGroup;
    private RadioButton paymentRangeAny, paymentRange1, paymentRange2, paymentRange3, paymentRange4;
    private CheckBox durationQuick, durationHalfDay, durationFullDay, durationMultiDay;
    private CheckBox complexityBeginner, complexityExperience, complexityProfessional;
    private CheckBox areaSubangJaya, areaPetalingJaya, areaShahAlam, areaKlangValley;
    
    public interface FilterCallback {
        void onFiltersApplied(FilterCriteria criteria);
        void onFilterCountChanged(int activeFilterCount);
    }
    
    public static class FilterCriteria implements Serializable {
        public List<String> categories;
        public String sortBy;
        public int distance; // in km
        public String paymentRange;
        public List<String> areas;
        public List<String> durations;
        public List<String> complexities;
        
        public FilterCriteria(List<String> categories, String sortBy, int distance, 
                            String paymentRange, List<String> areas, 
                            List<String> durations, List<String> complexities) {
            this.categories = categories;
            this.sortBy = sortBy;
            this.distance = distance;
            this.paymentRange = paymentRange;
            this.areas = areas;
            this.durations = durations;
            this.complexities = complexities;
        }
    }
    
    public FilterManager(Context context, FilterCallback callback) {
        this.context = context;
        this.callback = callback;
        this.filterPrefs = context.getSharedPreferences(FILTER_PREFS, Context.MODE_PRIVATE);
        
        // Load existing filter preferences
        loadFilterPreferences();
        
        // Notify initial filter count
        if (callback != null) {
            callback.onFilterCountChanged(getActiveFilterCount());
        }
    }
    
    public void showFilterDialog() {
        if (filterDialog != null && filterDialog.isShowing()) {
            return;
        }
        
        // Create dialog with fullscreen like Task dialog
        filterDialog = new Dialog(context);
        filterDialog.setContentView(R.layout.dialog_filter_tasks);
        
        // Match Task dialog window sizing - fullscreen approach
        WindowManager.LayoutParams layoutParams = filterDialog.getWindow().getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        filterDialog.getWindow().setAttributes(layoutParams);
        
        // Initialize all UI elements
        initializeViews();
        
        // Setup event listeners
        setupEventListeners();
        
        // Show dialog
        filterDialog.show();
    }
    
    private void initializeViews() {
        // Category checkboxes
        categoryCleaningCheckbox = filterDialog.findViewById(R.id.categoryCleaningCheckbox);
        categoryTutoringCheckbox = filterDialog.findViewById(R.id.categoryTutoringCheckbox);
        categoryDeliveryCheckbox = filterDialog.findViewById(R.id.categoryDeliveryCheckbox);
        categorySmallTasksCheckbox = filterDialog.findViewById(R.id.categorySmallTasksCheckbox);
        
        // Sort spinner
        sortBySpinner = filterDialog.findViewById(R.id.sortBySpinner);
        setupSortSpinner();
        
        // Distance seekbar
        distanceSeekBar = filterDialog.findViewById(R.id.distanceSeekBar);
        distanceValueText = filterDialog.findViewById(R.id.distanceValueText);
        
        // Payment range radio group
        paymentRangeGroup = filterDialog.findViewById(R.id.paymentRangeGroup);
        paymentRangeAny = filterDialog.findViewById(R.id.paymentRangeAny);
        paymentRange1 = filterDialog.findViewById(R.id.paymentRange1);
        paymentRange2 = filterDialog.findViewById(R.id.paymentRange2);
        paymentRange3 = filterDialog.findViewById(R.id.paymentRange3);
        paymentRange4 = filterDialog.findViewById(R.id.paymentRange4);
        
        // Duration checkboxes
        durationQuick = filterDialog.findViewById(R.id.durationQuick);
        durationHalfDay = filterDialog.findViewById(R.id.durationHalfDay);
        durationFullDay = filterDialog.findViewById(R.id.durationFullDay);
        durationMultiDay = filterDialog.findViewById(R.id.durationMultiDay);
        
        // Complexity checkboxes
        complexityBeginner = filterDialog.findViewById(R.id.complexityBeginner);
        complexityExperience = filterDialog.findViewById(R.id.complexityExperience);
        complexityProfessional = filterDialog.findViewById(R.id.complexityProfessional);
        
        // Area checkboxes
        areaSubangJaya = filterDialog.findViewById(R.id.areaSubangJaya);
        areaPetalingJaya = filterDialog.findViewById(R.id.areaPetalingJaya);
        areaShahAlam = filterDialog.findViewById(R.id.areaShahAlam);
        areaKlangValley = filterDialog.findViewById(R.id.areaKlangValley);
        
        // Load saved preferences
        loadSavedFilters();
    }
    
    private void setupSortSpinner() {
        String[] sortOptions = {"Default", "Highest Pay", "Newest", "Distance", "Rating"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
                android.R.layout.simple_spinner_dropdown_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);
    }
    
    private void setupEventListeners() {
        // Close button
        ImageView closeButton = filterDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> filterDialog.dismiss());
        
        // Distance seekbar listener
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedDistance = progress;
                if (progress == 0) {
                    distanceValueText.setText("Any distance");
                } else {
                    distanceValueText.setText(progress + " km");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Sort spinner listener
        sortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] sortOptions = {"", "payment", "newest", "distance", "rating"};
                if (position < sortOptions.length) {
                    selectedSort = sortOptions[position];
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Clear all filters
        TextView clearAllButton = filterDialog.findViewById(R.id.clearAllButton);
        clearAllButton.setOnClickListener(v -> clearAllFiltersWithUI());
        
        // Apply filters
        Button applyButton = filterDialog.findViewById(R.id.applyFiltersButton);
        applyButton.setOnClickListener(v -> applyFilters());
    }
    
    private void loadSavedFilters() {
        // Load categories
        selectedCategories.clear();
        if (filterPrefs.getBoolean("category_cleaning", false)) {
            selectedCategories.add("Cleaning");
            categoryCleaningCheckbox.setChecked(true);
        }
        if (filterPrefs.getBoolean("category_tutoring", false)) {
            selectedCategories.add("Tutoring");
            categoryTutoringCheckbox.setChecked(true);
        }
        if (filterPrefs.getBoolean("category_delivery", false)) {
            selectedCategories.add("Delivery");
            categoryDeliveryCheckbox.setChecked(true);
        }
        if (filterPrefs.getBoolean("category_small_tasks", false)) {
            selectedCategories.add("Small Tasks");
            categorySmallTasksCheckbox.setChecked(true);
        }
        
        // Load sort
        int sortPosition = filterPrefs.getInt("sort_position", 0);
        sortBySpinner.setSelection(sortPosition);
        String[] sortOptions = {"", "payment", "newest", "distance", "rating"};
        if (sortPosition < sortOptions.length) {
            selectedSort = sortOptions[sortPosition];
        }
        
        // Load distance
        selectedDistance = filterPrefs.getInt("distance", 10);
        distanceSeekBar.setProgress(selectedDistance);
        if (selectedDistance == 0) {
            distanceValueText.setText("Any distance");
        } else {
            distanceValueText.setText(selectedDistance + " km");
        }
        
        // Load payment range
        String savedPaymentRange = filterPrefs.getString("payment_range", "");
        selectedPaymentRange = savedPaymentRange;
        
        // Load areas, durations, complexities
        loadCheckboxPreferences();
    }
    
    private void loadCheckboxPreferences() {
        // Areas
        selectedAreas.clear();
        if (filterPrefs.getBoolean("area_subang_jaya", false)) {
            selectedAreas.add("Subang Jaya");
            areaSubangJaya.setChecked(true);
        }
        if (filterPrefs.getBoolean("area_petaling_jaya", false)) {
            selectedAreas.add("Petaling Jaya");
            areaPetalingJaya.setChecked(true);
        }
        if (filterPrefs.getBoolean("area_shah_alam", false)) {
            selectedAreas.add("Shah Alam");
            areaShahAlam.setChecked(true);
        }
        if (filterPrefs.getBoolean("area_klang_valley", false)) {
            selectedAreas.add("Klang Valley");
            areaKlangValley.setChecked(true);
        }
        
        // Durations
        selectedDurations.clear();
        if (filterPrefs.getBoolean("duration_quick", false)) {
            selectedDurations.add("Quick");
            durationQuick.setChecked(true);
        }
        if (filterPrefs.getBoolean("duration_half_day", false)) {
            selectedDurations.add("Half Day");
            durationHalfDay.setChecked(true);
        }
        if (filterPrefs.getBoolean("duration_full_day", false)) {
            selectedDurations.add("Full Day");
            durationFullDay.setChecked(true);
        }
        if (filterPrefs.getBoolean("duration_multi_day", false)) {
            selectedDurations.add("Multi-day");
            durationMultiDay.setChecked(true);
        }
        
        // Complexities
        selectedComplexities.clear();
        if (filterPrefs.getBoolean("complexity_beginner", false)) {
            selectedComplexities.add("Beginner");
            complexityBeginner.setChecked(true);
        }
        if (filterPrefs.getBoolean("complexity_experience", false)) {
            selectedComplexities.add("Experience");
            complexityExperience.setChecked(true);
        }
        if (filterPrefs.getBoolean("complexity_professional", false)) {
            selectedComplexities.add("Professional");
            complexityProfessional.setChecked(true);
        }
    }
    
    private void saveFilters() {
        SharedPreferences.Editor editor = filterPrefs.edit();
        
        // Save categories
        editor.putBoolean("category_cleaning", selectedCategories.contains("Cleaning"));
        editor.putBoolean("category_tutoring", selectedCategories.contains("Tutoring"));
        editor.putBoolean("category_delivery", selectedCategories.contains("Delivery"));
        editor.putBoolean("category_small_tasks", selectedCategories.contains("Small Tasks"));
        
        // Save sort
        editor.putInt("sort_position", sortBySpinner.getSelectedItemPosition());
        
        // Save distance
        editor.putInt("distance", selectedDistance);
        
        // Save payment range
        editor.putString("payment_range", selectedPaymentRange);
        
        // Save areas
        editor.putBoolean("area_subang_jaya", selectedAreas.contains("Subang Jaya"));
        editor.putBoolean("area_petaling_jaya", selectedAreas.contains("Petaling Jaya"));
        editor.putBoolean("area_shah_alam", selectedAreas.contains("Shah Alam"));
        editor.putBoolean("area_klang_valley", selectedAreas.contains("Klang Valley"));
        
        // Save durations
        editor.putBoolean("duration_quick", selectedDurations.contains("Quick"));
        editor.putBoolean("duration_half_day", selectedDurations.contains("Half Day"));
        editor.putBoolean("duration_full_day", selectedDurations.contains("Full Day"));
        editor.putBoolean("duration_multi_day", selectedDurations.contains("Multi-day"));
        
        // Save complexities
        editor.putBoolean("complexity_beginner", selectedComplexities.contains("Beginner"));
        editor.putBoolean("complexity_experience", selectedComplexities.contains("Experience"));
        editor.putBoolean("complexity_professional", selectedComplexities.contains("Professional"));
        
        editor.apply();
    }
    
    /**
     * Load filter preferences from SharedPreferences (without UI)
     */
    private void loadFilterPreferences() {
        // Load categories
        selectedCategories.clear();
        if (filterPrefs.getBoolean("category_cleaning", false)) {
            selectedCategories.add("Cleaning");
        }
        if (filterPrefs.getBoolean("category_tutoring", false)) {
            selectedCategories.add("Tutoring");
        }
        if (filterPrefs.getBoolean("category_delivery", false)) {
            selectedCategories.add("Delivery");
        }
        if (filterPrefs.getBoolean("category_small_tasks", false)) {
            selectedCategories.add("Small Tasks");
        }
        
        // Load sort
        int sortPosition = filterPrefs.getInt("sort_position", 0);
        String[] sortOptions = {"", "payment", "newest", "distance", "rating"};
        if (sortPosition < sortOptions.length) {
            selectedSort = sortOptions[sortPosition];
        }
        
        // Load distance
        selectedDistance = filterPrefs.getInt("distance", 10);
        
        // Load payment range
        selectedPaymentRange = filterPrefs.getString("payment_range", "");
        
        // Load areas
        selectedAreas.clear();
        if (filterPrefs.getBoolean("area_subang_jaya", false)) {
            selectedAreas.add("Subang Jaya");
        }
        if (filterPrefs.getBoolean("area_petaling_jaya", false)) {
            selectedAreas.add("Petaling Jaya");
        }
        if (filterPrefs.getBoolean("area_shah_alam", false)) {
            selectedAreas.add("Shah Alam");
        }
        if (filterPrefs.getBoolean("area_klang_valley", false)) {
            selectedAreas.add("Klang Valley");
        }
        
        // Load durations
        selectedDurations.clear();
        if (filterPrefs.getBoolean("duration_quick", false)) {
            selectedDurations.add("Quick");
        }
        if (filterPrefs.getBoolean("duration_half_day", false)) {
            selectedDurations.add("Half Day");
        }
        if (filterPrefs.getBoolean("duration_full_day", false)) {
            selectedDurations.add("Full Day");
        }
        if (filterPrefs.getBoolean("duration_multi_day", false)) {
            selectedDurations.add("Multi Day");
        }
        
        // Load complexities
        selectedComplexities.clear();
        if (filterPrefs.getBoolean("complexity_beginner", false)) {
            selectedComplexities.add("Beginner");
        }
        if (filterPrefs.getBoolean("complexity_experience", false)) {
            selectedComplexities.add("Experience Required");
        }
        if (filterPrefs.getBoolean("complexity_professional", false)) {
            selectedComplexities.add("Professional");
        }
    }
    
    /**
     * Calculate the number of active filters
     */
    public int getActiveFilterCount() {
        int count = 0;
        
        // Count categories
        if (!selectedCategories.isEmpty()) {
            count++;
        }
        
        // Count payment range (only if not "Any Amount")
        if (!selectedPaymentRange.isEmpty()) {
            count++;
        }
        
        // Count distance (only if not default/any)
        if (selectedDistance > 0) {
            count++;
        }
        
        // Count areas
        if (!selectedAreas.isEmpty()) {
            count++;
        }
        
        // Count durations
        if (!selectedDurations.isEmpty()) {
            count++;
        }
        
        // Count complexities
        if (!selectedComplexities.isEmpty()) {
            count++;
        }
        
        // Count sort (only if not default)
        if (!selectedSort.isEmpty() && !selectedSort.equals("")) {
            count++;
        }
        
        return count;
    }
    
    /**
     * Get a summary of active filters for display
     */
    public String getActiveFiltersSummary() {
        if (getActiveFilterCount() == 0) {
            return "No filters active";
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (!selectedCategories.isEmpty()) {
            summary.append("📂 Categories: ").append(String.join(", ", selectedCategories)).append("\n");
        }
        
        if (!selectedPaymentRange.isEmpty()) {
            summary.append("💰 Payment: RM ").append(selectedPaymentRange).append("\n");
        }
        
        if (selectedDistance > 0) {
            if (selectedDistance == 20) {
                summary.append("📍 Distance: Any\n");
            } else {
                summary.append("📍 Distance: ≤").append(selectedDistance).append(" km\n");
            }
        }
        
        if (!selectedAreas.isEmpty()) {
            summary.append("🗺️ Areas: ").append(String.join(", ", selectedAreas)).append("\n");
        }
        
        if (!selectedDurations.isEmpty()) {
            summary.append("⏱️ Duration: ").append(String.join(", ", selectedDurations)).append("\n");
        }
        
        if (!selectedComplexities.isEmpty()) {
            summary.append("🔧 Complexity: ").append(String.join(", ", selectedComplexities)).append("\n");
        }
        
        if (!selectedSort.isEmpty() && !selectedSort.equals("")) {
            summary.append("📊 Sort: ").append(selectedSort).append("\n");
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Check if any filters are currently active
     */
    public boolean hasActiveFilters() {
        return getActiveFilterCount() > 0;
    }
    
    /**
     * Public method to clear all filters (can be called from outside)
     */
    public void clearAllFilters() {
        selectedCategories.clear();
        selectedSort = "";
        selectedDistance = 10;
        selectedPaymentRange = "";
        selectedAreas.clear();
        selectedDurations.clear();
        selectedComplexities.clear();
        
        // Save cleared preferences
        saveFilters();
        
        // Notify callback about filter count change
        if (callback != null) {
            callback.onFilterCountChanged(getActiveFilterCount());
        }
        
        Toast.makeText(context, "All filters cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void clearAllFiltersWithUI() {
        // Clear data using the public method
        clearAllFilters();
        
        // Reset UI components
        categoryCleaningCheckbox.setChecked(false);
        categoryTutoringCheckbox.setChecked(false);
        categoryDeliveryCheckbox.setChecked(false);
        categorySmallTasksCheckbox.setChecked(false);
        
        sortBySpinner.setSelection(0);
        
        distanceSeekBar.setProgress(10);
        distanceValueText.setText("10 km");
        
        paymentRangeAny.setChecked(true);
        
        durationQuick.setChecked(false);
        durationHalfDay.setChecked(false);
        durationFullDay.setChecked(false);
        durationMultiDay.setChecked(false);
        
        complexityBeginner.setChecked(false);
        complexityExperience.setChecked(false);
        complexityProfessional.setChecked(false);
        
        areaSubangJaya.setChecked(false);
        areaPetalingJaya.setChecked(false);
        areaShahAlam.setChecked(false);
        areaKlangValley.setChecked(false);
    }
    
    private void applyFilters() {
        // Collect categories
        selectedCategories.clear();
        if (categoryCleaningCheckbox.isChecked()) selectedCategories.add("Cleaning");
        if (categoryTutoringCheckbox.isChecked()) selectedCategories.add("Tutoring");
        if (categoryDeliveryCheckbox.isChecked()) selectedCategories.add("Delivery");
        if (categorySmallTasksCheckbox.isChecked()) selectedCategories.add("Small Tasks");
        
        // Collect payment range
        int selectedPaymentId = paymentRangeGroup.getCheckedRadioButtonId();
        selectedPaymentRange = "";
        if (selectedPaymentId == R.id.paymentRange1) selectedPaymentRange = "10-50";
        else if (selectedPaymentId == R.id.paymentRange2) selectedPaymentRange = "50-100";
        else if (selectedPaymentId == R.id.paymentRange3) selectedPaymentRange = "100-200";
        else if (selectedPaymentId == R.id.paymentRange4) selectedPaymentRange = "200+";
        
        // Collect durations
        selectedDurations.clear();
        if (durationQuick.isChecked()) selectedDurations.add("Quick");
        if (durationHalfDay.isChecked()) selectedDurations.add("Half Day");
        if (durationFullDay.isChecked()) selectedDurations.add("Full Day");
        if (durationMultiDay.isChecked()) selectedDurations.add("Multi-day");
        
        // Collect complexities
        selectedComplexities.clear();
        if (complexityBeginner.isChecked()) selectedComplexities.add("Beginner");
        if (complexityExperience.isChecked()) selectedComplexities.add("Experience");
        if (complexityProfessional.isChecked()) selectedComplexities.add("Professional");
        
        // Collect areas
        selectedAreas.clear();
        if (areaSubangJaya.isChecked()) selectedAreas.add("Subang Jaya");
        if (areaPetalingJaya.isChecked()) selectedAreas.add("Petaling Jaya");
        if (areaShahAlam.isChecked()) selectedAreas.add("Shah Alam");
        if (areaKlangValley.isChecked()) selectedAreas.add("Klang Valley");
        
        // Save current preferences
        saveFilters();
        
        // Create filter criteria
        FilterCriteria criteria = new FilterCriteria(
            selectedCategories,
            selectedSort,
            selectedDistance,
            selectedPaymentRange,
            selectedAreas,
            selectedDurations,
            selectedComplexities
        );
        
        // Notify callback about filters and count
        if (callback != null) {
            callback.onFiltersApplied(criteria);
            callback.onFilterCountChanged(getActiveFilterCount());
        }
        
        // Show feedback
        String message = "Filters applied";
        int activeCount = getActiveFilterCount();
        if (activeCount > 0) {
            message += " (" + activeCount + " active)";
        }
        if (!selectedCategories.isEmpty()) {
            message += " - " + String.join(", ", selectedCategories);
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        
        // Close dialog
        filterDialog.dismiss();
    }
} 