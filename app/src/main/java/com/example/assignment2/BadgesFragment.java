package com.example.assignment2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class BadgesFragment extends Fragment {

    private int walletAmount = 0; // Store the wallet amount

    public static BadgesFragment newInstance() {
        return new BadgesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        TextView walletAmountText = view.findViewById(R.id.text_wallet_amount);
        Button beginnerRedeemButton = view.findViewById(R.id.button_beginner_redeem);
        ProgressBar beginnerProgressBar = view.findViewById(R.id.progress_beginner_tasks);
        Button withdrawButton = view.findViewById(R.id.button_redeem);

        Button employerPraiseRedeemButton = view.findViewById(R.id.button_employer_praise_redeem);
        ProgressBar employerPraiseProgressBar = view.findViewById(R.id.progress_employer_praise);

        Button consistentPerformerRedeemButton = view.findViewById(R.id.button_consistent_performer_redeem);
        ProgressBar consistentPerformerProgressBar = view.findViewById(R.id.progress_consistent_performer);

        // Check claimed state
        boolean beginnerClaimed = requireActivity()
            .getSharedPreferences("badges_prefs", 0)
            .getBoolean("beginner_claimed", false);

        if (beginnerClaimed) {
            beginnerProgressBar.setProgress(0);
            beginnerRedeemButton.setEnabled(false);
            beginnerRedeemButton.setText("Claimed");
        } else {
            beginnerProgressBar.setProgress(100); // Always full on login
            beginnerRedeemButton.setEnabled(true);
            beginnerRedeemButton.setText("Redeem");
        }

        beginnerRedeemButton.setOnClickListener(v -> {
            walletAmount += 20;
            walletAmountText.setText("RM" + walletAmount);

            beginnerProgressBar.setProgress(0);
            beginnerRedeemButton.setEnabled(false);
            beginnerRedeemButton.setText("Claimed");

            // Save claimed state
            requireActivity().getSharedPreferences("badges_prefs", 0)
                .edit()
                .putBoolean("beginner_claimed", true)
                .apply();
        });

        withdrawButton.setOnClickListener(v -> {
            walletAmount = 0;
            walletAmountText.setText("RM" + walletAmount);
            Toast.makeText(getContext(), "The money transfer to bank already.", Toast.LENGTH_SHORT).show();
        });

        // Employer Praise
        boolean employerPraiseClaimed = requireActivity()
            .getSharedPreferences("badges_prefs", 0)
            .getBoolean("employer_praise_claimed", false);

        if (employerPraiseClaimed) {
            employerPraiseProgressBar.setProgress(0);
            employerPraiseRedeemButton.setEnabled(false);
            employerPraiseRedeemButton.setText("Claimed");
        } else {
            employerPraiseProgressBar.setProgress(100);
            employerPraiseRedeemButton.setEnabled(true);
            employerPraiseRedeemButton.setText("Redeem");
        }

        employerPraiseRedeemButton.setOnClickListener(v -> {
            walletAmount += 20;
            walletAmountText.setText("RM" + walletAmount);

            employerPraiseProgressBar.setProgress(0);
            employerPraiseRedeemButton.setEnabled(false);
            employerPraiseRedeemButton.setText("Claimed");

            requireActivity().getSharedPreferences("badges_prefs", 0)
                .edit()
                .putBoolean("employer_praise_claimed", true)
                .apply();
        });

        // Consistent Performer
        boolean consistentPerformerClaimed = requireActivity()
            .getSharedPreferences("badges_prefs", 0)
            .getBoolean("consistent_performer_claimed", false);

        if (consistentPerformerClaimed) {
            consistentPerformerProgressBar.setProgress(0);
            consistentPerformerRedeemButton.setEnabled(false);
            consistentPerformerRedeemButton.setText("Claimed");
        } else {
            consistentPerformerProgressBar.setProgress(100);
            consistentPerformerRedeemButton.setEnabled(true);
            consistentPerformerRedeemButton.setText("Redeem");
        }

        consistentPerformerRedeemButton.setOnClickListener(v -> {
            walletAmount += 20;
            walletAmountText.setText("RM" + walletAmount);

            consistentPerformerProgressBar.setProgress(0);
            consistentPerformerRedeemButton.setEnabled(false);
            consistentPerformerRedeemButton.setText("Claimed");

            requireActivity().getSharedPreferences("badges_prefs", 0)
                .edit()
                .putBoolean("consistent_performer_claimed", true)
                .apply();
        });

        return view;
    }
} 