package com.aayfi.whrtigo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class LoadingFriendsActivity extends Activity {
	public static LoadingFriendsActivity instance = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading_friends);
		instance = this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.loading_friends, menu);
		return true;
	}
	
	@Override
	public void finish() {
	    super.finish();
	    instance = null;
	}


}
