package com.example.trackback;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ReportlostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reportlost);

        // Set up system bar insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button logic
        LinearLayout backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        // Set up the AutoCompleteTextView
        String[] categories = {
                "Electronics",
                "Documents",
                "Clothing",
                "Accessories",
                "Bags",
                "Wallets",
                "Keys",
                "Jewelry",
                "Books",
                "School Supplies",
                "ID Cards",
                "Mobile Phones",
                "Umbrellas",
                "Eyeglasses",
                "Others"
        };

        AutoCompleteTextView autoComplete = findViewById(R.id.category);

        // Use the custom dropdown item layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categories);
        autoComplete.setAdapter(adapter);

        // Set OnItemClickListener to display Toast on item selection (if needed)
        autoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBrand = (String) parent.getItemAtPosition(position);
            Toast.makeText(this, "Selected: " + selectedBrand, Toast.LENGTH_SHORT).show();
        });
    }
}
