package com.Topspot;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;

import com.example.topspot.R;

/**
 * This Activity is used to contain activities in the app such as the main maps
 * activity or friends activity used to find which venues your friends are in
 * @author Fiifi
 *
 */
public class ContainActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contain);
		
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.whrt2golabel);
		
		ActionBar.Tab Maps = actionBar.newTab().setText("Venues near me");
		ActionBar.Tab Friends = actionBar.newTab().setText("Friends");
		
		Fragment MapsFrag = new MapFragment();
//		Fragment FriendsFrag = new FriendsActivity();
		
		Maps.setTabListener(new myTabListener(MapsFrag));
//		Friends.setTabListener(new myTabListener(FriendsFrag));
		
		actionBar.addTab(Maps);
		actionBar.addTab(Friends);
		
//		/** TabHost will have Tabs */
//		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
//		tabHost.setup();
//
//		/** TabSpec used to create a new tab.
//		* By using TabSpec only we can able to setContent to the tab.
//		* By using TabSpec setIndicator() we can set name to tab. */
//
//		/** tid1 is firstTabSpec Id. Its used to access outside. */
//		TabSpec firstTabSpec = tabHost.newTabSpec("tid1");
//		TabSpec secondTabSpec = tabHost.newTabSpec("tid1");
//
//		/** TabSpec setIndicator() is used to set name for the tab. */
//		/** TabSpec setContent() is used to set content for a particular tab. */
//		firstTabSpec.setIndicator("Places").setContent(new Intent(this,MapsActivity.class));
//		secondTabSpec.setIndicator("Friends").setContent(new Intent(this,FriendsActivity.class));
//
//		/** Add tabSpec to the TabHost to display. */
//		tabHost.addTab(firstTabSpec);
//		tabHost.addTab(secondTabSpec);
		
	}
	
	class myTabListener implements ActionBar.TabListener{
		public Fragment fragment;
		
		public myTabListener(Fragment _fragment){
			fragment = _fragment;
		}
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.add(R.id.realtabcontent, fragment);
			
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.container, menu);
		return true;
	}


}
