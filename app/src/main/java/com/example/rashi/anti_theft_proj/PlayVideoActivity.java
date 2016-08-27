package com.example.rashi.anti_theft_proj;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlayVideoActivity extends Activity {
	
	private VideoView videoPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play_video);
		
		getActionBar().hide();
		
		Intent intent = getIntent();
		String filePath = (String) intent.getStringExtra("filePath");
		
		videoPlayer = (VideoView) findViewById(R.id.videoPlayer);
		videoPlayer.setVideoPath(filePath);
		videoPlayer.setMediaController(new MediaController(this));
		
		videoPlayer.start();
	}
}
