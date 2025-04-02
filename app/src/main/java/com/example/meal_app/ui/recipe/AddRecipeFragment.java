package com.example.meal_app.ui.recipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
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
    protected List<View> ingredientViews = new ArrayList<>();
    protected List<View> instructionViews = new ArrayList<>();
    protected FirebaseFirestore db;
    private OkHttpClient client;
    private View saveButton;
    private Snackbar loadingSnackbar;
    protected EditText recipeNameInput;
    protected EditText descriptionInput;
    protected EditText cookingTimeInput;
    protected EditText servingsInput;
    protected Spinner categorySpinner;
    protected ArrayAdapter<String> categoryAdapter;
    protected List<String> ingredientsList = new ArrayList<>();
    protected List<String> instructionsList = new ArrayList<>();
    private TextView totalPriceText;
    private double totalPrice = 0.0;

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

        // Initialize views
        recipeNameInput = root.findViewById(R.id.recipeNameInput);
        descriptionInput = root.findViewById(R.id.recipeDescriptionInput);
        cookingTimeInput = root.findViewById(R.id.cookingTimeInput);
        servingsInput = root.findViewById(R.id.servingsInput);
        categorySpinner = root.findViewById(R.id.categorySpinner);

        // Setup back button
        ImageButton backButton = root.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        setupViews(root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        totalPriceText = view.findViewById(R.id.totalPriceText);
        updateTotalPrice();
    }

    // Getter methods for EditRecipeFragment
    protected EditText getRecipeNameInput() {
        return requireView().findViewById(R.id.recipeNameInput);
    }

    protected EditText getDescriptionInput() {
        return requireView().findViewById(R.id.recipeDescriptionInput);
    }

    protected EditText getCookingTimeInput() {
        return requireView().findViewById(R.id.cookingTimeInput);
    }

    protected EditText getServingsInput() {
        return requireView().findViewById(R.id.servingsInput);
    }

    protected Spinner getCategorySpinner() {
        return requireView().findViewById(R.id.categorySpinner);
    }

    protected ArrayAdapter<CharSequence> getCategoryAdapter() {
        return (ArrayAdapter<CharSequence>) getCategorySpinner().getAdapter();
    }

    protected List<String> getIngredientsList() {
        return ingredientsList;
    }

    protected List<String> getInstructionsList() {
        return instructionsList;
    }

    protected FirebaseFirestore getDb() {
        return db;
    }

    protected void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    protected void loadImageFromUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty() && getView() != null) {
            ImageView recipeImageView = getView().findViewById(R.id.recipeImageView);
            Glide.with(this)
                    .load(imageUrl)
                    .into(recipeImageView);
        }
    }

    protected boolean validateInputs() {
        if (recipeNameInput.getText().toString().trim().isEmpty()) {
            showError("Please enter a recipe name");
            return false;
        }
        if (descriptionInput.getText().toString().trim().isEmpty()) {
            showError("Please enter a recipe description");
            return false;
        }
        if (cookingTimeInput.getText().toString().trim().isEmpty()) {
            showError("Please enter cooking time");
            return false;
        }
        if (servingsInput.getText().toString().trim().isEmpty()) {
            showError("Please enter number of servings");
            return false;
        }
        if (categorySpinner.getSelectedItemPosition() == 0) {
            showError("Please select a category");
            return false;
        }
        return true;
    }

    protected Map<String, Object> createRecipeData() {
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("name", recipeNameInput.getText().toString().trim());
        recipe.put("description", descriptionInput.getText().toString().trim());
        recipe.put("cookingTime", Integer.parseInt(cookingTimeInput.getText().toString().trim()));
        recipe.put("servings", Integer.parseInt(servingsInput.getText().toString().trim()));
        recipe.put("category", categorySpinner.getSelectedItem().toString());
        recipe.put("createdAt", System.currentTimeMillis());
        recipe.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        // Collect ingredients as maps with all properties
        List<Map<String, Object>> ingredients = new ArrayList<>();
        for (View ingredientView : ingredientViews) {
            EditText nameInput = ingredientView.findViewById(R.id.ingredientNameInput);
            EditText quantityInput = ingredientView.findViewById(R.id.quantityInput);
            EditText priceInput = ingredientView.findViewById(R.id.priceInput);
            Spinner unitSpinner = ingredientView.findViewById(R.id.unitSpinner);
            Spinner typeSpinner = ingredientView.findViewById(R.id.typeSpinner);

            String name = nameInput.getText().toString().trim();
            String quantityStr = quantityInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String unit = unitSpinner.getSelectedItem().toString();
            String type = typeSpinner.getSelectedItem().toString();

            if (!name.isEmpty()) {
                Map<String, Object> ingredient = new HashMap<>();
                ingredient.put("name", name);
                ingredient.put("quantity", quantityStr.isEmpty() ? 0 : Integer.parseInt(quantityStr));
                ingredient.put("price", priceStr.isEmpty() ? 0 : Integer.parseInt(priceStr));
                ingredient.put("unit", unit);
                ingredient.put("type", type);
                ingredients.add(ingredient);
            }
        }
        recipe.put("ingredients", ingredients);

        // Collect instructions
        instructionsList.clear();
        for (View instructionView : instructionViews) {
            EditText stepInput = instructionView.findViewById(R.id.stepInput);
            String step = stepInput.getText().toString().trim();
            if (!step.isEmpty()) {
                instructionsList.add(step);
            }
        }
        recipe.put("instructions", instructionsList);

        // Debug log to show the data structure
        android.util.Log.d("AddRecipeFragment", "Recipe Data Structure:");
        android.util.Log.d("AddRecipeFragment", "------------------------");
        android.util.Log.d("AddRecipeFragment", "name: " + recipe.get("name"));
        android.util.Log.d("AddRecipeFragment", "description: " + recipe.get("description"));
        android.util.Log.d("AddRecipeFragment", "cookingTime: " + recipe.get("cookingTime"));
        android.util.Log.d("AddRecipeFragment", "servings: " + recipe.get("servings"));
        android.util.Log.d("AddRecipeFragment", "category: " + recipe.get("category"));
        android.util.Log.d("AddRecipeFragment", "createdAt: " + recipe.get("createdAt"));
        android.util.Log.d("AddRecipeFragment", "userId: " + recipe.get("userId"));
        android.util.Log.d("AddRecipeFragment", "ingredients: [");
        for (Map<String, Object> ingredient : ingredients) {
            android.util.Log.d("AddRecipeFragment", "  {");
            android.util.Log.d("AddRecipeFragment", "    name: " + ingredient.get("name"));
            android.util.Log.d("AddRecipeFragment", "    quantity: " + ingredient.get("quantity"));
            android.util.Log.d("AddRecipeFragment", "    unit: " + ingredient.get("unit"));
            android.util.Log.d("AddRecipeFragment", "    price: " + ingredient.get("price"));
            android.util.Log.d("AddRecipeFragment", "    type: " + ingredient.get("type"));
            android.util.Log.d("AddRecipeFragment", "  }");
        }
        android.util.Log.d("AddRecipeFragment", "]");
        android.util.Log.d("AddRecipeFragment", "instructions: [");
        for (String instruction : instructionsList) {
            android.util.Log.d("AddRecipeFragment", "  " + instruction);
        }
        android.util.Log.d("AddRecipeFragment", "]");
        android.util.Log.d("AddRecipeFragment", "------------------------");

        return recipe;
    }

    private void setupViews(View root) {
        // Setup category spinner
        categoryAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, getStringArrayAsList(R.array.recipe_categories));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Setup image upload
        View imageUploadCard = root.findViewById(R.id.imageUploadCard);
        ImageButton removeImageButton = root.findViewById(R.id.removeImageButton);

        imageUploadCard.setOnClickListener(v -> openImagePicker());

        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            ImageView imageView = root.findViewById(R.id.recipeImageView);
            imageView.setImageDrawable(null);
            root.findViewById(R.id.uploadPromptLayout).setVisibility(View.VISIBLE);
        });

        // Setup save button
        root.findViewById(R.id.saveButton).setOnClickListener(v -> saveRecipe());

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

    protected void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Recipe Image"), PICK_IMAGE_REQUEST);
    }

    protected Uri getSelectedImageUri() {
        return selectedImageUri;
    }

    protected void setSelectedImageUri(Uri uri) {
        this.selectedImageUri = uri;
        if (uri != null && getView() != null) {
            ImageView imageView = getView().findViewById(R.id.recipeImageView);
            View uploadPrompt = getView().findViewById(R.id.uploadPromptLayout);
            if (imageView != null && uploadPrompt != null) {
                Glide.with(this)
                    .load(uri)
                    .into(imageView);
                uploadPrompt.setVisibility(View.GONE);
            }
        }
    }

    protected String getBase64FromUri(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    protected void uploadImageToImgBB(String base64Image, Map<String, Object> recipeData) {
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
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showError("Failed to upload image: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONObject data = jsonObject.getJSONObject("data");
                    String imageUrl = data.getString("url");
                    recipeData.put("imageUrl", imageUrl);
                    requireActivity().runOnUiThread(() -> {
                        if (AddRecipeFragment.this instanceof EditRecipeFragment) {
                            ((EditRecipeFragment) AddRecipeFragment.this).updateRecipeInFirestore(recipeData);
                        } else {
                            saveRecipeToFirestore(recipeData);
                        }
                    });
                } catch (JSONException e) {
                    requireActivity().runOnUiThread(() -> {
                        showError("Failed to process image upload response: " + e.getMessage());
                    });
                }
            }
        });
    }

    protected void addIngredientView(View root) {
        View ingredientView = getLayoutInflater().inflate(R.layout.item_ingredient_input, null);
        setupIngredientView(ingredientView);

        // Add margin to the ingredient view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32); // 32dp bottom margin
        ingredientView.setLayoutParams(params);

        LinearLayout container = root.findViewById(R.id.ingredientsContainer);
        container.addView(ingredientView);
        ingredientViews.add(ingredientView);
    }

    protected void addInstructionStep(View root) {
        View stepView = getLayoutInflater().inflate(R.layout.item_instruction_step, null);
        
        // Set step number
        TextView stepNumber = stepView.findViewById(R.id.stepNumberText);
        stepNumber.setText(String.format("%d.", instructionViews.size() + 1));

        // Add margin to the step view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32); // 32dp bottom margin
        stepView.setLayoutParams(params);

        setupInstructionView(stepView);
        LinearLayout container = root.findViewById(R.id.instructionsContainer);
        container.addView(stepView);
        instructionViews.add(stepView);
    }

    private void setupIngredientView(View ingredientView) {
        // Setup spinners
        Spinner unitSpinner = ingredientView.findViewById(R.id.unitSpinner);
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        Spinner typeSpinner = ingredientView.findViewById(R.id.typeSpinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.ingredient_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Setup remove button
        ImageButton removeButton = ingredientView.findViewById(R.id.removeIngredientButton);
        removeButton.setOnClickListener(v -> {
            ((ViewGroup) ingredientView.getParent()).removeView(ingredientView);
            ingredientViews.remove(ingredientView);
            updateTotalPrice();
        });

        // Setup price change listener
        EditText priceInput = ingredientView.findViewById(R.id.priceInput);
        priceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateTotalPrice();
            }
        });
    }

    private void setupInstructionView(View stepView) {
        // Setup remove button
        ImageButton removeButton = stepView.findViewById(R.id.removeStepButton);
        removeButton.setOnClickListener(v -> {
            ((ViewGroup) stepView.getParent()).removeView(stepView);
            instructionViews.remove(stepView);
            updateStepNumbers();
        });
    }

    private void updateStepNumbers() {
        for (int i = 0; i < instructionViews.size(); i++) {
            TextView stepNumber = instructionViews.get(i).findViewById(R.id.stepNumberText);
            stepNumber.setText(String.format("%d.", i + 1));
        }
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (View ingredientView : ingredientViews) {
            EditText priceInput = ingredientView.findViewById(R.id.priceInput);
            String priceStr = priceInput.getText().toString().trim();
            if (!priceStr.isEmpty()) {
                try {
                    totalPrice += Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
        }
        if (totalPriceText != null) {
            totalPriceText.setText(String.format("Total: NRS %.2f", totalPrice));
        }
    }

    protected void saveRecipe() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Save Recipe")
                .setMessage("Are you sure you want to save this recipe?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show loading state
                    saveButton = requireView().findViewById(R.id.saveButton);
                    saveButton.setEnabled(false);
                    loadingSnackbar = Snackbar.make(requireView(), "Saving recipe...", Snackbar.LENGTH_INDEFINITE);
                    loadingSnackbar.show();

                    // Create recipe data
                    Map<String, Object> recipe = createRecipeData();

                    // If there's an image, upload it first, otherwise save directly
                    if (selectedImageUri != null) {
                        try {
                            String base64Image = getBase64FromUri(selectedImageUri);
                            uploadImageToImgBB(base64Image, recipe);
                        } catch (IOException e) {
                            showError("Failed to process image: " + e.getMessage());
                        }
                    } else {
                        saveRecipeToFirestore(recipe);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveRecipeToFirestore(Map<String, Object> recipe) {
        // Save the recipe directly
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
                showError("Failed to save recipe: " + e.getMessage() + "\nPlease try again.");
            });
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