package com.aayfi.whrtigo;



import android.app.Activity;

public class myMapFragment extends ActivityHostFragment {

	

	@Override
	protected Class<? extends Activity> getActivityClass() {
		// TODO Auto-generated method stub
		return MapsActivity.class;
	}

}
