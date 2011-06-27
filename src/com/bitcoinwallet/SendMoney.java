package com.bitcoinwallet;

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
		amountField = (EditText) findViewById(R.id.amount);
		memoField = (EditText) findViewById(R.id.memo);

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
				amountField.setText(amount);
				amountField.requestFocus();
			}
			if (b.getString("label") != null) {
				memoField.setText(URLDecoder.decode(b.getString("label")) + " ");
			}
			if (b.getString("message") != null) {
				memoField.setText(memoField.getText() + URLDecoder.decode(b.getString("message")));
			}
		}

		Button sendButton = (Button) this.findViewById(R.id.send_money_button);
		sendButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Transaction sendTx = null;
				ApplicationState appState = ApplicationState.current;

				try {
					address = new Address(appState.params, addressField.getText().toString());
					amount = Utils.toNanoCoins(amountField.getText().toString());
					sendTx = appState.wallet.createSend(address, amount);
					
					if (sendTx != null) {
						appState.sendTransaction(sendTx);
						Log.d("Wallet", "Sent " + Utils.bitcoinValueToFriendlyString(amount) + " to " + address.toString());
						
						AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
						builder.setMessage("Payment successful!").setCancelable(false).setNegativeButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								startActivity(new Intent(SendMoney.this, BitcoinWallet.class));
							}
						});
						AlertDialog alert = builder.create();
						alert.show();
					} else {
						AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
						String msg = "You're payment exceeds your current balance. \n\n";
						if (appState.wallet.getPendingTransactions().size() > 0){
							msg += " Please wait for your pending transactions to be confirmed, or add additional funds to your wallet using the 'Receive Money' button on the home screen.";
						} else {
							msg += " Try funding your bitcoin wallet by using the 'Receive Money' button on the home screen.";
						}
						builder.setMessage(msg)
								.setCancelable(false).setNegativeButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
										startActivity(new Intent(SendMoney.this, BitcoinWallet.class));
									}
								});
						AlertDialog alert = builder.create();
						alert.show();
					}
				} catch (AddressFormatException e) {
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
					builder.setMessage("Inalid address, please try again.").setCancelable(false).setNegativeButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				} catch (Exception e) {
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(SendMoney.this);
					builder.setMessage("Sorry we couldn't send that.  Please check your values and try again.").setCancelable(false)
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
