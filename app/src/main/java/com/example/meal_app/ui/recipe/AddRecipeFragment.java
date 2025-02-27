package com.example.meal_app.ui.recipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import com.example.meal_app.R;
import com.example.meal_app.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AddRecipeFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private double totalPrice = 0.00;
    private List<View> ingredientViews = new ArrayList<>();
    private List<View> instructionViews = new ArrayList<>();
    private FirebaseFirestore db;
    private OkHttpClient client;
    private View saveButton;
    private Snackbar loadingSnackbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_recipe, container, false);
        setupViews(root);
        return root;
    }

    private void setupViews(View root) {
        // Setup category spinner
        Spinner categorySpinner = root.findViewById(R.id.categorySpinner);
        List<String> categories = new ArrayList<>();
        categories.add("Select Category"); // Add hint as first item
        categories.addAll(getStringArrayAsList(R.array.recipe_categories));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Setup image upload
        View imageUploadCard = root.findViewById(R.id.imageUploadCard);
        ImageButton removeImageButton = root.findViewById(R.id.removeImageButton);

        imageUploadCard.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Recipe Image"), PICK_IMAGE_REQUEST);
        });

        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            ImageView imageView = root.findViewById(R.id.recipeImageView);
            imageView.setImageDrawable(null);
            removeImageButton.setVisibility(View.GONE);
            root.findViewById(R.id.uploadPromptLayout).setVisibility(View.VISIBLE);
        });

        // Setup save button
        root.findViewById(R.id.saveButton).setOnClickListener(v -> saveRecipe(root));

        // Setup add ingredient button
        root.findViewById(R.id.addIngredientButton).setOnClickListener(v -> addIngredientView(root));

        // Setup add step button
        root.findViewById(R.id.addStepButton).setOnClickListener(v -> addInstructionStep(root));

        // Add initial ingredient and instruction
        addIngredientView(root);
        addInstructionStep(root);
    }

    private List<String> getStringArrayAsList(int arrayResourceId) {
        String[] array = requireContext().getResources().getStringArray(arrayResourceId);
        return new ArrayList<>(Arrays.asList(array));
    }

    private void addIngredientView(View root) {
        LinearLayout container = root.findViewById(R.id.ingredientsContainer);
        View ingredientView = getLayoutInflater().inflate(R.layout.item_ingredient_input, container, false);

        // Setup unit spinner
        Spinner unitSpinner = ingredientView.findViewById(R.id.unitSpinner);
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        // Setup remove button
        ingredientView.findViewById(R.id.removeIngredientButton).setOnClickListener(v -> {
            container.removeView(ingredientView);
            ingredientViews.remove(ingredientView);
            updateTotalPrice();
        });

        // Setup price change listener
        EditText priceInput = ingredientView.findViewById(R.id.priceInput);
        priceInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateTotalPrice();
            }
        });

        container.addView(ingredientView);
        ingredientViews.add(ingredientView);
    }

    private void addInstructionStep(View root) {
        LinearLayout container = root.findViewById(R.id.instructionsContainer);
        View stepView = getLayoutInflater().inflate(R.layout.item_instruction_step, container, false);

        // Set step number
        TextView stepNumber = stepView.findViewById(R.id.stepNumberText);
        stepNumber.setText(String.format("%d.", instructionViews.size() + 1));

        // Setup remove button
        stepView.findViewById(R.id.removeStepButton).setOnClickListener(v -> {
            container.removeView(stepView);
            instructionViews.remove(stepView);
            updateStepNumbers();
        });

        container.addView(stepView);
        instructionViews.add(stepView);
    }

    private void updateStepNumbers() {
        for (int i = 0; i < instructionViews.size(); i++) {
            TextView stepNumber = instructionViews.get(i).findViewById(R.id.stepNumberText);
            stepNumber.setText(String.format("%d.", i + 1));
        }
    }

    private void updateTotalPrice() {
        totalPrice = 0.00;
        for (View ingredientView : ingredientViews) {
            EditText priceInput = ingredientView.findViewById(R.id.priceInput);
            String priceText = priceInput.getText().toString();
            if (!priceText.isEmpty()) {
                try {
                    totalPrice += Double.parseDouble(priceText);
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }
        }
        TextView totalText = requireView().findViewById(R.id.ingredientsTotalText);
        totalText.setText(String.format("Total: NRS %.2f", totalPrice));
    }

    private void saveRecipe(View root) {
        // Validate inputs
        String name = ((EditText) root.findViewById(R.id.recipeNameInput)).getText().toString().trim();
        String description = ((EditText) root.findViewById(R.id.recipeDescriptionInput)).getText().toString().trim();
        String cookingTimeStr = ((EditText) root.findViewById(R.id.cookingTimeInput)).getText().toString().trim();
        String servingsStr = ((EditText) root.findViewById(R.id.servingsInput)).getText().toString().trim();
        Spinner categorySpinner = root.findViewById(R.id.categorySpinner);

        // Validation checks
        if (name.isEmpty()) {
            handleError("Please enter a recipe name");
            return;
        }
        if (description.isEmpty()) {
            handleError("Please enter a recipe description");
            return;
        }
        if (cookingTimeStr.isEmpty()) {
            handleError("Please enter cooking time");
            return;
        }
        if (servingsStr.isEmpty()) {
            handleError("Please enter number of servings");
            return;
        }
        if (categorySpinner.getSelectedItemPosition() == 0) {
            handleError("Please select a category");
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Save Recipe")
                .setMessage("Are you sure you want to save this recipe?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show loading state
                    saveButton = root.findViewById(R.id.saveButton);
                    saveButton.setEnabled(false);
                    loadingSnackbar = Snackbar.make(root, "Saving recipe...", Snackbar.LENGTH_INDEFINITE);
                    loadingSnackbar.show();

                    // Create recipe data
                    Map<String, Object> recipe = new HashMap<>();
                    recipe.put("name", name);
                    recipe.put("description", description);
                    recipe.put("cookingTime", Integer.parseInt(cookingTimeStr));
                    recipe.put("servings", Integer.parseInt(servingsStr));
                    recipe.put("category", categorySpinner.getSelectedItem().toString());
                    recipe.put("totalPrice", totalPrice);
                    recipe.put("createdAt", System.currentTimeMillis());

                    // Add userId to recipe
                    recipe.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

                    // Add ingredients and instructions
                    addIngredientsAndInstructions(recipe);

                    // If there's an image, upload it first, otherwise save directly
                    if (selectedImageUri != null) {
                        try {
                            String base64Image = getBase64FromUri(selectedImageUri);
                            uploadImageToImgBB(base64Image, recipe);
                        } catch (IOException e) {
                            handleError("Failed to process image: " + e.getMessage());
                        }
                    } else {
                        saveRecipeToFirestore(recipe);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void addIngredientsAndInstructions(Map<String, Object> recipe) {
        // Add ingredients
        List<Map<String, Object>> ingredients = new ArrayList<>();
        for (View ingredientView : ingredientViews) {
            String ingredientName = ((EditText) ingredientView.findViewById(R.id.ingredientNameInput))
                    .getText().toString().trim();
            String quantity = ((EditText) ingredientView.findViewById(R.id.quantityInput))
                    .getText().toString().trim();
            String unit = ((Spinner) ingredientView.findViewById(R.id.unitSpinner))
                    .getSelectedItem().toString();
            String price = ((EditText) ingredientView.findViewById(R.id.priceInput))
                    .getText().toString().trim();

            if (!ingredientName.isEmpty() && !quantity.isEmpty() && !price.isEmpty()) {
                Map<String, Object> ingredient = new HashMap<>();
                ingredient.put("name", ingredientName);
                ingredient.put("quantity", Double.parseDouble(quantity));
                ingredient.put("unit", unit);
                ingredient.put("price", Double.parseDouble(price));
                ingredients.add(ingredient);
            }
        }
        recipe.put("ingredients", ingredients);

        // Add instructions
        List<String> instructions = new ArrayList<>();
        for (View instructionView : instructionViews) {
            String instruction = ((EditText) instructionView.findViewById(R.id.stepInstructionInput))
                    .getText().toString().trim();
            if (!instruction.isEmpty()) {
                instructions.add(instruction);
            }
        }
        recipe.put("instructions", instructions);
    }

    private String getBase64FromUri(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void uploadImageToImgBB(String base64Image, final Map<String, Object> recipe) {
        if (loadingSnackbar != null) {
            loadingSnackbar.setText("Uploading image...");
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", Constants.IMGBB_API_KEY)
                .addFormDataPart("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    handleError("Network error: " + e.getMessage()
                            + "\nPlease check your internet connection and try again.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        String errorMsg = "Image upload failed (HTTP " + response.code() + ")";
                        if (response.code() == 400) {
                            errorMsg = "Invalid image format or size too large. Please try a different image.";
                        } else if (response.code() == 403) {
                            errorMsg = "API key error. Please contact support.";
                        }
                        handleError(errorMsg);
                    });
                    return;
                }

                String jsonData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONObject data = jsonObject.getJSONObject("data");
                    String imageUrl = data.getString("url");
                    recipe.put("imageUrl", imageUrl);
                    requireActivity().runOnUiThread(() -> {
                        if (loadingSnackbar != null) {
                            loadingSnackbar.setText("Saving recipe...");
                        }
                        saveRecipeToFirestore(recipe);
                    });
                } catch (JSONException e) {
                    requireActivity().runOnUiThread(() -> {
                        handleError("Failed to process image upload response: " + e.getMessage());
                    });
                }
            }
        });
    }

    private void saveRecipeToFirestore(Map<String, Object> recipe) {
        db.collection("recipes")
                .add(recipe)
                .addOnSuccessListener(documentReference -> {
                    if (loadingSnackbar != null) {
                        loadingSnackbar.dismiss();
                    }
                    Snackbar.make(requireView(), "Recipe saved successfully!", Snackbar.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireActivity(),
                            R.id.nav_host_fragment_activity_main);
                    navController.navigateUp();
                })
                .addOnFailureListener(e -> {
                    handleError("Failed to save recipe: " + e.getMessage() + "\nPlease try again.");
                });
    }

    private void handleError(String errorMessage) {
        if (loadingSnackbar != null) {
            loadingSnackbar.dismiss();
        }
        if (saveButton != null) {
            saveButton.setEnabled(true);
        }
        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG)
                .setAction("OK", v -> {
                })
                .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null
                && data.getData() != null) {
            selectedImageUri = data.getData();
            ImageView imageView = requireView().findViewById(R.id.recipeImageView);
            View uploadPrompt = requireView().findViewById(R.id.uploadPromptLayout);
            ImageButton removeImageButton = requireView().findViewById(R.id.removeImageButton);

            imageView.setImageURI(selectedImageUri);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            uploadPrompt.setVisibility(View.GONE);
            removeImageButton.setVisibility(View.VISIBLE);
        }
    }
}