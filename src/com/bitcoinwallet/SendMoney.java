package com.bitcoinwallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.net.URLDecoder;

public class SendMoney extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_money);

		Bundle b = getIntent().getExtras();
		EditText addressField = (EditText) findViewById(R.id.address);

		if (b == null) {
			addressField.requestFocus();
		} else {
			if (b.getString("address") != null) {
				addressField.setText(b.getString("address"));
			}
			if (b.getString("amount") != null) {
				String amount = b.getString("amount");
				if (amount.toLowerCase().endsWith("x8")) {
					amount = amount.toLowerCase().replace("x8", "");
				}
				EditText amountField = (EditText) findViewById(R.id.amount);
				amountField.setText(amount);
				amountField.requestFocus();
			}
			if (b.getString("label") != null) {
				EditText memo = (EditText) findViewById(R.id.memo);
				memo.setText(URLDecoder.decode(b.getString("label")) + " ");
			}
			if (b.getString("message") != null) {
				EditText memo = (EditText) findViewById(R.id.memo);
				memo.setText(memo.getText()
						+ URLDecoder.decode(b.getString("message")));
			}
		}
		
		Button sendButton = (Button)this.findViewById(R.id.send_money_button);
		sendButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	
	        }
	    });

	}

}
