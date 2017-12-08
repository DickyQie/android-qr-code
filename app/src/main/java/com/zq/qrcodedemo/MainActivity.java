package com.zq.qrcodedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.zq.qrcodedemo.ui.BusinessCardActivity;
import com.zq.qrcodedemo.util.ScanCodeActivity;

public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn1:
                startActivity(new Intent(MainActivity.this, ScanCodeActivity.class));
                break;
            case R.id.btn3:
                startActivity(new Intent(MainActivity.this, BusinessCardActivity.class));
                break;
        }
    }
}
