package com.Topspot;

import java.util.ArrayList;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;


public class VenuesOverlay extends ItemizedOverlay {
	private ArrayList<OverlayItem> myOverlays = new ArrayList<OverlayItem>();
	Context mContext;	
/**
 * This construct sets the centre-point at the
 *  bottom of the image to be the point at which it's attached to the 
 *  map coordinates
 * @param defaultMarker
 */
	public VenuesOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));		
	}	
/**
 * This passes the defaultMarker up to the default constructor to bound its
 *  coordinates and then initialises mContext with the given Context
 * @param defaultMarker
 * @param context
 */
	public VenuesOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;	
		populate();
	}
/**
 * When the populate() method executes, this method is called to retrieve a 
 * specified OverlayItem from the ArrayList containing Overlay Items.
 */
	@Override
	protected OverlayItem createItem(int i) {		
		return myOverlays.get(i);
	}
/**
* Returns the current number of items in the ArrayList of Overlay Items
*/
	@Override
	public int size() {		
		return myOverlays.size();
	}
/**
* This method adds new OverlayItem objects to the ArrayList
* @param overlay
*/
	public void addOverlay(OverlayItem overlay){
		myOverlays.add(overlay);
		populate();
	}
/**
 * This uses the member android.content.Context to create an AlertDialog.Builder 
 * and uses the tapped OverlayItem's title and snippet for the dialog's title 
 * and message text.	
 */
	@Override
	protected boolean onTap(int index){
		OverlayItem item = myOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);	
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;		
	}

}
