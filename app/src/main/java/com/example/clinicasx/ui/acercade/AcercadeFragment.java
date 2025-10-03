package com.example.clinicasx.ui.acercade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.clinicasx.databinding.FragmentAcercadeBinding;

public class AcercadeFragment extends Fragment {

    private FragmentAcercadeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AcercadeViewModel acercadeViewModel =
                new ViewModelProvider(this).get(AcercadeViewModel.class);

        binding = FragmentAcercadeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAcercade;
        acercadeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}