package com.example.coderunner;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        LinearLayout card = findViewById(R.id.landing_card);
        Button getStarted = findViewById(R.id.getStartedButton);

        // Fade-in animation for the card
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        card.startAnimation(fadeIn);

        // Pulse logo
        ImageView logoIv = findViewById(R.id.logo);
        Animation pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse);
        logoIv.startAnimation(pulse);

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // login removed

    }
}
