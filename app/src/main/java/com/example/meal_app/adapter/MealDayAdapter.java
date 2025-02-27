package com.example.meal_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.model.MealDay;
import com.example.meal_app.model.Recipe;
import java.util.List;

public class MealDayAdapter extends RecyclerView.Adapter<MealDayAdapter.ViewHolder> {
    private final List<MealDay> mealDays;
    private final OnMealDayListener listener;

    public interface OnMealDayListener {
        void onMealDayClick(String day);

        void onRemoveMealClick(String day, Recipe recipe);
    }

    public MealDayAdapter(List<MealDay> mealDays, OnMealDayListener listener) {
        this.mealDays = mealDays;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealDay mealDay = mealDays.get(position);
        holder.bind(mealDay);
    }

    @Override
    public int getItemCount() {
        return mealDays.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayText;
        private final RecyclerView mealsRecyclerView;
        private AssignedMealsAdapter mealsAdapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            mealsRecyclerView = itemView.findViewById(R.id.mealsRecyclerView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMealDayClick(mealDays.get(position).getDay());
                }
            });
        }

        public void bind(MealDay mealDay) {
            dayText.setText(mealDay.getDay());
            setupMealsRecyclerView(mealDay);
        }

        private void setupMealsRecyclerView(MealDay mealDay) {
            mealsAdapter = new AssignedMealsAdapter(mealDay.getRecipes(),
                    recipe -> listener.onRemoveMealClick(mealDay.getDay(), recipe));
            mealsRecyclerView.setAdapter(mealsAdapter);
            mealsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }
}