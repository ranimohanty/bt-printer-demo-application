package com.matm.matmsdk.Error;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import isumatm.androidsdk.equitas.R;

public class ErrorMatm1Activity extends AppCompatActivity {

    String response;
    Button closeBtn;
    TextView tvResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_matm1);

        Bundle bundle = getIntent().getExtras();

        tvResponse = findViewById(R.id.tvResponse);
        closeBtn = findViewById(R.id.closeBtn);

        if (bundle != null) {
            response = bundle.getString("response");
        }
        tvResponse.setText(response);
        closeBtn.setOnClickListener(v -> finish());
    }
}