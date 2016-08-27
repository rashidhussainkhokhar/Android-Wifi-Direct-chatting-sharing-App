package com.example.rashi.anti_theft_proj;



import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;


public class ViewImageActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_image);
		
		Intent intent = getIntent();
		String fileName = intent.getStringExtra("fileName");
		setTitle(fileName);


		
		//ImageView to display the image
		ImageView contentView = (ImageView) findViewById(R.id.fullscreen_content);
		contentView.setImageBitmap(ChatAdapter.bitmap);
		
		//Back button
		Button button = (Button) findViewById(R.id.back_button);
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}




}
