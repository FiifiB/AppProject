package com.Topspot;



import android.app.Activity;

public class MapFragment extends ActivityHostFragment {

	

	@Override
	protected Class<? extends Activity> getActivityClass() {
		// TODO Auto-generated method stub
		return MapsActivity.class;
	}

}
