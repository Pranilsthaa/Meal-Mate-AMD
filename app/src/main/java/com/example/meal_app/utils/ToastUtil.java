package com.example.meal_app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.meal_app.R;

public class ToastUtil {
    private static Toast currentToast;
    private static final long TOAST_DURATION = 3000; // 3 seconds

    public static void showToast(Context context, String message, boolean isError) {
        // Cancel any existing toast
        if (currentToast != null) {
            currentToast.cancel();
        }

        // Create new toast
        currentToast = new Toast(context);
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        
        // Set icon based on type
        ImageView iconView = view.findViewById(R.id.toastIcon);
        iconView.setImageResource(isError ? R.drawable.ic_error : R.drawable.ic_info);
        
        // Set message
        TextView messageView = view.findViewById(R.id.toastMessage);
        messageView.setText(message);
        
        // Set close button
        view.findViewById(R.id.toastClose).setOnClickListener(v -> currentToast.cancel());
        
        currentToast.setView(view);
        currentToast.setDuration(Toast.LENGTH_LONG);
        
        // Position at top
        currentToast.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL, 0, 100);
        
        // Show toast
        currentToast.show();
    }

    public static void showSuccessToast(Context context, String message) {
        showToast(context, message, false);
    }

    public static void showErrorToast(Context context, String message) {
        showToast(context, message, true);
    }
} 