package com.bitcoinwallet;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;


public class SendMoney extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.send_money);
	    
	    Bundle b = getIntent().getExtras();
	    EditText address = (EditText) findViewById(R.id.address);
	    address.setText(b.getString("address"));
	    
	    if(b.getString("amount") != null){
	    	String amount = b.getString("amount");
	    	if(amount.toLowerCase().endsWith("x8")){
	    		amount = amount.toLowerCase().replace("x8", "");
	    	}
	    	EditText amountField = (EditText) findViewById(R.id.amount);
	    	amountField.setText(amount);
	    }
	    if(b.getString("label") != null){
	    	EditText memo = (EditText) findViewById(R.id.memo);
	    	memo.setText(b.getString("label") + " ");
	    }
	    if(b.getString("message") != null){
	    	EditText memo = (EditText) findViewById(R.id.memo);
	    	memo.setText(memo.getText() + b.getString("message"));
	    }
	    
	}

}
