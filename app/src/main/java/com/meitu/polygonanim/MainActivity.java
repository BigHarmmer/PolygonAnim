package com.meitu.polygonanim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.meitu.polygon.CornerPolygonImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CornerPolygonImageView cornerPolygonImageView = findViewById(R.id.my_anim_view);
        cornerPolygonImageView.startAnim();
    }
}
