package com.example.meal_app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel class for the Home fragment. This class is responsible for
 * preparing and managing the data for the Home UI.
 */
public class HomeViewModel extends ViewModel {

    // MutableLiveData to hold the text data for the Home fragment
    private final MutableLiveData<String> mText;

    /**
     * Constructor for HomeViewModel. Initializes the MutableLiveData with a
     * default value.
     */
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment"); // Default text for the Home fragment
    }

    /**
     * Returns a LiveData object containing the text data. LiveData is used to
     * observe changes in the data.
     *
     * @return LiveData<String> containing the text data
     */
    public LiveData<String> getText() {
        return mText;
    }
}
