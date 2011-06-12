package com.bitcoinwallet;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLDecoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet.BalanceType;

public class SendMoney extends Activity {
	
	EditText addressField;
	EditText amountField;
	EditText memoField;
	
	Address address;
	BigInteger amount;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_money);

		Bundle b = getIntent().getExtras();
		addressField = (EditText) findViewById(R.id.address);

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
				amountField = (EditText) findViewById(R.id.amount);
				amountField.setText(amount);
				amountField.requestFocus();
			}
			if (b.getString("label") != null) {
				memoField = (EditText) findViewById(R.id.memo);
				memoField.setText(URLDecoder.decode(b.getString("label")) + " ");
			}
			if (b.getString("message") != null) {
				memoField = (EditText) findViewById(R.id.memo);
				memoField.setText(memoField.getText()
						+ URLDecoder.decode(b.getString("message")));
			}
		}
		
		Button sendButton = (Button)this.findViewById(R.id.send_money_button);
		sendButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	Transaction sendTx = null;
	        	ApplicationState appState = ApplicationState.current;

	        	try {
	        		address = new Address(appState.params, addressField.getText().toString());
	        		amount = Utils.toNanoCoins(amountField.getText().toString());
					sendTx = appState.wallet.sendCoins(appState.getPeer(), address, amount);
					Log.d("Wallet", "Sending. Amount=" + amount + " Wallet="+appState.wallet.getBalance(BalanceType.ESTIMATED));
					appState.saveWallet();
					if (sendTx != null) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								SendMoney.this);
						builder.setMessage("Bitcoins successfully sent.")
								.setCancelable(false)
								.setNegativeButton("Ok",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.cancel();
												startActivity(new Intent(
														SendMoney.this,
														BitcoinWallet.class));
											}
										});
						AlertDialog alert = builder.create();
						alert.show();
					}
				} catch (IOException e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
					builder.setMessage(e.getMessage())
					       .setCancelable(false)
					       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					       });
					AlertDialog alert = builder.create();
					alert.show();
				} catch (AddressFormatException e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
					builder.setMessage("Inalid address, please try again.")
					       .setCancelable(false)
					       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					       });
					AlertDialog alert = builder.create();
					alert.show();
				}
				
				
				if (sendTx == null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
					builder.setMessage("You're payment exceeds your current balance.  Try funding your bitcoin wallet by using the 'Receive Money' button from the home screen.")
					       .setCancelable(false)
					       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					           }
					       });
					AlertDialog alert = builder.create();
					alert.show();
				}
				
	        }
	    });

	}

}
