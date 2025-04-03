package com.example.meal_app.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel class for the Dashboard fragment. This class is responsible for
 * preparing and managing the data for the Dashboard UI.
 */
public class DashboardViewModel extends ViewModel {

    // MutableLiveData to hold the text data for the Dashboard fragment
    private final MutableLiveData<String> mText;

    /**
     * Constructor for DashboardViewModel. Initializes the MutableLiveData with
     * a default value.
     */
    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment"); // Default text value
    }

    /**
     * Returns a LiveData object that can be observed for changes in the text
     * data.
     *
     * @return LiveData<String> containing the text data.
     */
    public LiveData<String> getText() {
        return mText;
    }
}
