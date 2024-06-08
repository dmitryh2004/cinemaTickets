package com.example.cinematickets;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;

import com.example.cinematickets.databinding.FragmentAuthorBinding;
import com.example.cinematickets.databinding.FragmentProgramBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProgramFragment extends Fragment {
    private FragmentProgramBinding binding;
    private boolean isAdmin;

    public ProgramFragment(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProgramBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        updateFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFragment();
    }

    private void updateFragment() {
        Resources res = getResources();
        String instructionUrl = "https://dmitryh2004.github.io/cinemaTicketsInstructionsite/";
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        Log.d("programInstructionDebug", "" + nightModeFlags);
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            instructionUrl += (isAdmin) ? "admin_instruction_night.html" : "user_instruction_night.html";
        } else {
            instructionUrl += (isAdmin) ? "admin_instruction.html" : "user_instruction.html";
        }
        binding.aboutProgramText.setWebViewClient(new WebViewClient());

        binding.aboutProgramText.loadUrl(instructionUrl);
    }
}