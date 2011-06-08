package com.bitcoinwallet;

import com.google.zxing.client.android.CaptureActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class BitcoinWallet extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button sendButton = (Button)this.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	startActivity(new Intent(BitcoinWallet.this, CaptureActivity.class));
	        }
	    });
        
        Button receiveButton = (Button)this.findViewById(R.id.receive_button);
        receiveButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	startActivity(new Intent(BitcoinWallet.this, ReceiveMoney.class));
	        }
	    });
    }
    
    
}