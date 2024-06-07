package com.example.combinedpls;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class Home extends AppCompatActivity implements View.OnClickListener {

    private ImageView homeImageView, dietImageView, trainingImageView, userImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        homeImageView = findViewById(R.id.home);
        dietImageView = findViewById(R.id.diet);
        trainingImageView = findViewById(R.id.training);
        userImageView = findViewById(R.id.user);

        homeImageView.setOnClickListener(this);
        dietImageView.setOnClickListener(this);
        trainingImageView.setOnClickListener(this);
        userImageView.setOnClickListener(this);

        replaceFragment(new HomeFragment());

    }

    @Override
    public void onClick(View v) {
        Fragment fragment = null;
        if (v.getId() == R.id.home) {
            fragment = new HomeFragment();
        } else if (v.getId() == R.id.diet) {
            fragment = new DietFragment();
        } else if (v.getId() == R.id.training) {
            fragment = new TrainingFragment();
        } else if (v.getId() == R.id.user) {
            fragment = new UserFragment();
        } else {
            fragment = new HomeFragment();
        }
        replaceFragment(fragment);
    }


    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}