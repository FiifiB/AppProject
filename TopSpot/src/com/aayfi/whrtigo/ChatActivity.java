package com.aayfi.whrtigo;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jivesoftware.smack.packet.Message;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aayfi.whrtigo.R;
import com.aayfi.whrtigo.quickblox.MyChatController;
import com.quickblox.module.chat.QBChat;
import com.quickblox.module.users.model.QBUser;

public class ChatActivity extends Activity {
	private EditText messageText;
    private TextView meLabel;
    private TextView friendLabel;
    private ViewGroup messagesContainer;
    private ScrollView scrollContainer;
    private ActionBar actionBar;
    private Bundle extras;
    private Bitmap friendpic;
    
    private MyChatController myChatController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		extras = getIntent().getExtras();
		actionBar = getActionBar();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				friendpic = getFBpic(extras.getString("FriendID"));
				
				runOnUiThread(new Runnable() {
					public void run() {
						actionBar.setIcon(new BitmapDrawable(friendpic));
					}
				});
				
			}
		}).start();;
		
		
		actionBar.setTitle(extras.getString("FriendFName"));
		
		
		QBUser me = new QBUser();
		me.setId(Integer.valueOf(extras.getString("MyID")));
		me.setLogin("00"+extras.getString("MyID")+"11");
		me.setPassword(extras.getString("MyID")+extras.getString("MyLName"));
		
		QBUser friend = new QBUser();
		friend.setId(Integer.valueOf(extras.getString("FriendID")));
		friend.setLogin("00"+extras.getString("FriendID")+"11");
		friend.setPassword(extras.getString("FriendID")+ extras.getString("FriendLName"));
		
		 // UI stuff
        messagesContainer = (ViewGroup) findViewById(R.id.messagesContainer);
        scrollContainer = (ScrollView) findViewById(R.id.scrollContainer);

        Button sendMessageButton = (Button) findViewById(R.id.sendButton);
        sendMessageButton.setOnClickListener(onSendMessageClickListener);

        messageText = (EditText) findViewById(R.id.messageEdit);
//        meLabel = (TextView) findViewById(R.id.meLabel);
//        friendLabel = (TextView) findViewById(R.id.friendLabel);
//        meLabel.setText(me.getLogin() + " (me)");
//        friendLabel.setText(friend.getLogin());

        // ================= QuickBlox ===== Step 5 =================
        // Get chat login based on QuickBlox user account.
        // Note, that to start chat you should use only short login,
        // that looks like '17744-1028' (<qb_user_id>-<qb_app_id>).
        String chatLogin = QBChat.getChatLoginShort(me);

        // Our current (me) user's password.
        String password = me.getPassword();

        if (me != null && friend != null) {


            // ================= QuickBlox ===== Step 6 =================
            // All chat logic can be implemented by yourself using
            // ASMACK library (https://github.com/Flowdalic/asmack/downloads)
            // -- Android wrapper for Java XMPP library (http://www.igniterealtime.org/projects/smack/).
            myChatController = new MyChatController(chatLogin, password);
            myChatController.setOnMessageReceivedListener(onMessageReceivedListener);

            // ================= QuickBlox ===== Step 7 =================
            // Get friend's login based on QuickBlox user account.
            // Note, that for your companion you should use full chat login,
            // that looks like '17792-1028@chat.quickblox.com' (<qb_user_id>-<qb_app_id>@chat.quickblox.com).
            // Don't use short login, it
            String friendLogin = QBChat.getChatLoginFull(friend);

            myChatController.startChat(friendLogin);
        }
	}
	
    private void sendMessage() {
        if (messageText != null) {
            String messageString = messageText.getText().toString();
            myChatController.sendMessage(messageString);
            messageText.setText("");
            showMessage(messageString, true);
        }
    }

    private MyChatController.OnMessageReceivedListener onMessageReceivedListener = new MyChatController.OnMessageReceivedListener() {
        @Override
        public void onMessageReceived(final Message message) {
            String messageString = message.getBody();
            showMessage(messageString, false);
        }
    };

    private void showMessage(String message, boolean leftSide) {
        final TextView textView = new TextView(ChatActivity.this);
        textView.setTextColor(Color.BLACK);
        textView.setText(message);

        int bgRes = R.drawable.left_message_bg;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if (!leftSide) {
            bgRes = R.drawable.right_message_bg;
            params.gravity = Gravity.RIGHT;
        }

        textView.setLayoutParams(params);

        textView.setBackgroundResource(bgRes);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messagesContainer.addView(textView);

                // Scroll to bottom
                if (scrollContainer.getChildAt(0) != null) {
                    scrollContainer.scrollTo(scrollContainer.getScrollX(), scrollContainer.getChildAt(0).getHeight());
                }
                scrollContainer.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private View.OnClickListener onSendMessageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            sendMessage();
        }
    };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
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


}
