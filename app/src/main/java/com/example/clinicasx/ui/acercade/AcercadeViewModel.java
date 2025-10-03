package com.example.clinicasx.ui.acercade;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AcercadeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AcercadeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Acerca de");
    }

    public LiveData<String> getText() {
        return mText;
    }
}