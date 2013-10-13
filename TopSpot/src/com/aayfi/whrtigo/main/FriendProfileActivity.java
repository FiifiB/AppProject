package com.aayfi.whrtigo.main;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aayfi.whrtigo.R;

public class FriendProfileActivity extends Activity {
	private String UserID;
	private String UserLName;
	private String FriendID;
	private String FriendName;
	private String FriendFName;
	private String FriendLName;
	private String FriendLocation;
	private ImageView profilepic;
	private TextView name;
	private TextView location;
	private TextView status;
	private ImageButton notify;
	private ImageButton IMfriend;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friend_profile);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.whrt2golabel);
		
		Bundle bundle = getIntent().getExtras();
		if(bundle != null){
			FriendID = bundle.getString("ID");
			FriendName = bundle.getString("Name");
			FriendLocation = bundle.getString("Location");
			FriendFName = bundle.getString("FriendFName");
			FriendLName = bundle.getString("FriendLName");
			UserID = bundle.getString("MyID");
			UserLName = bundle.getString("MyLName");
					
		}
		
		profilepic = (ImageView)findViewById(R.id.imageView1);
		name =(TextView)findViewById(R.id.friendNametxt);
		location = (TextView)findViewById(R.id.FriendLocationtxt);
//		status = (TextView)findViewById(R.id.profilestatusView);
		notify = (ImageButton)findViewById(R.id.meetupButton);
		IMfriend = (ImageButton)findViewById(R.id.startIMButton);
		
		
		new Thread(new Runnable() {
	        public void run() {
	            final Bitmap bitmap = getFBpic(FriendID);
	            profilepic.post(new Runnable() {
	                public void run() {
	                	Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
	                	BitmapShader shader = new BitmapShader (bitmap,  TileMode.CLAMP, TileMode.CLAMP);
	                	Paint paint = new Paint();
	                	        paint.setShader(shader);
	                	        Canvas c = new Canvas(circleBitmap);
	                	        c.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth()/2, paint);
	                	        
	                	profilepic.setImageBitmap(addBorder(bitmap, 3));
	                }
	            });
	        }
	    }).start();
		
		
		name.setText(FriendName);
		location.setText(FriendFName+ " is currently at " + FriendLocation);
		
		ImageButton StartIM = (ImageButton)findViewById(R.id.startIMButton);
		ImageButton SendNotification = (ImageButton)findViewById(R.id.meetupButton);
		
		StartIM.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(FriendProfileActivity.this,ChatActivity.class);

				intent.putExtra("FriendID", FriendID);
				intent.putExtra("FriendFName", FriendFName);
				intent.putExtra("FriendLName", FriendLName);
				intent.putExtra("MyID", UserID);
				intent.putExtra("MyLName", UserLName);
				
				startActivity(intent);
				
				
			}
		});
		
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.friend_profile, menu);
		return true;
	}
	
	private Bitmap getFBpic(String id){
		Bitmap img = null;
		String uri = 
				"https://graph.facebook.com/"+id+"/picture?type=large";
			     HttpClient httpclient = new DefaultHttpClient();			     
			     HttpGet httpget = new HttpGet(uri);
			     org.apache.http.HttpResponse response = null;				     
				try {
					response = httpclient.execute(httpget);						
					HttpEntity entity = response.getEntity();
					img = BitmapFactory.decodeStream(entity.getContent());
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		return img;
	}
	
	private Bitmap addBorder(Bitmap bmp, int borderSize) {
	    Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
	    Canvas canvas = new Canvas(bmpWithBorder);
//	    canvas.drawColor(Color.LTGRAY);
	    

        int color = 0xff424242;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bmpWithBorder.getWidth(), bmpWithBorder.getHeight());
        RectF rectF = new RectF(rect);
        float roundPx = 25;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#c4c2c7"));
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    
	    canvas.drawBitmap(bmp, borderSize, borderSize, null);
	    return bmpWithBorder;
	}
	
public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, FriendsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            intent.putExtra("already fb logged", "false");
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
