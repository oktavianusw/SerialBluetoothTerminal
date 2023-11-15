package de.kai_morich.simple_bluetooth_terminal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

public class ChooseDevice extends AppCompatActivity {



    public static String jenis_device = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device);

        MaterialCardView pentaClaimet = findViewById(R.id.card_pentaclimate);
        pentaClaimet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jenis_device = "PCL";
                Intent intent = new Intent(ChooseDevice.this, MainActivity.class);
                startActivity(intent);
            }
        });

        MaterialCardView pentaFlood = findViewById(R.id.card_pentaflood);
        pentaFlood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jenis_device = "PFL";
                Intent intent = new Intent(ChooseDevice.this, MainActivity.class);
                startActivity(intent);
            }
        });

        MaterialCardView pentaBar = findViewById(R.id.card_pentabar);
        pentaBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jenis_device = "PBL";
                Intent intent = new Intent(ChooseDevice.this, MainActivity.class);
                startActivity(intent);
            }
        });

        MaterialCardView pentaQua = findViewById(R.id.card_pentaqua);
        pentaQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jenis_device = "PQL";
                Intent intent = new Intent(ChooseDevice.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }

}

//    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//        Toast.makeText(getApplicationContext(), jenis_device[position], Toast.LENGTH_LONG).show();
//    }
//
//    @Override
//    public void onNothingSelected(AdapterView<?> parent) {
//
//    }
//
//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//        super.onPointerCaptureChanged(hasCapture);
//    }
//}