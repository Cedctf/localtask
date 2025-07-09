package com.example.assignment2;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatusBarHelper {

    /**
     * Configure the status bar for optimal display
     * @param activity The activity to configure
     */
    public static void configureStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Make status bar transparent
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            
            // Set light status bar (dark icons on light background)
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }

    /**
     * Apply system window insets to a view to avoid overlap with system UI
     * @param view The view to apply insets to
     */
    public static void applySystemWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            // Get the system window insets
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

            // Apply padding to avoid overlap
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (params != null) {
                params.topMargin = topInset;
                params.bottomMargin = bottomInset;
                params.leftMargin = leftInset;
                params.rightMargin = rightInset;
                v.setLayoutParams(params);
            } else {
                v.setPadding(
                    v.getPaddingLeft() + leftInset,
                    v.getPaddingTop() + topInset,
                    v.getPaddingRight() + rightInset,
                    v.getPaddingBottom() + bottomInset
                );
            }

            return insets;
        });
    }

    /**
     * Set the status bar to be light or dark based on the background
     * @param activity The activity to configure
     * @param isLightBackground Whether the background is light
     */
    public static void setStatusBarIconsColor(Activity activity, boolean isLightBackground) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = activity.getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            
            if (isLightBackground) {
                // Dark icons for light background
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                // Light icons for dark background
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            
            decorView.setSystemUiVisibility(flags);
        }
    }
} 