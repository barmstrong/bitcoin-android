package com.bitcoinwallet;

import java.math.BigInteger;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.WalletEventListener;
import com.google.zxing.client.android.CaptureActivity;

public class BitcoinWallet extends Activity {

	ProgressThread progressThread;
	ProgressDialog progressDialog;
	ApplicationState appState;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button sendButton = (Button) this.findViewById(R.id.send_button);
		sendButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(BitcoinWallet.this, CaptureActivity.class));
			}
		});

		Button receiveButton = (Button) this.findViewById(R.id.receive_button);
		receiveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(BitcoinWallet.this, ReceiveMoney.class));
			}
		});

		appState = ((ApplicationState) getApplication());

		updateUI();
		updateBlockChain();

		appState.wallet.addEventListener(new WalletEventListener() {
			public void onCoinsReceived(Wallet w, final Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
				runOnUiThread(new Runnable() {
					public void run() {
						moneyReceived(tx);
						Log.d("Wallet", "COINS WE RECEIVED HAVE BEEN CONFIRMED");
					}
				});
			}
			
			public void onCoinsSent(Wallet w, final Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
				runOnUiThread(new Runnable() {
					public void run() {
						updateUI();
						appState.saveWallet();
						Log.d("Wallet", "COINS WE SENT HAVE BEEN CONFIRMED");
					}
				});
			}

			public void onPendingCoinsReceived(Wallet w, final Transaction tx) {
				runOnUiThread(new Runnable() {
					public void run() {
						moneyReceived(tx);
						Log.d("Wallet", "PENDING COINS RECEIVED ");
					}
				});
			}
		});
	}

	private void updateBlockChain() {
		progressDialog = new ProgressDialog(BitcoinWallet.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMessage("Syncing with network...");
		progressDialog.setProgress(0);

		Handler handler = new Handler() {
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

			public void handleMessage(Message msg) {
				int total = msg.arg1;
				progressDialog.setProgress(total);
				if (total < 80) {
					// progressDialog.show();
				} else if (total < 100) {
					progressBar.setVisibility(View.VISIBLE);
				} else if (total >= 100) {
					progressDialog.hide();
					progressBar.setVisibility(View.GONE);
					updateUI();
					Log.d("Wallet", "Download complete");
				}
			}
		};
		((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);

		progressThread = new ProgressThread(handler, this);
		progressThread.start();
	}

	private void updateUI() {
		TextView balance = (TextView) findViewById(R.id.balanceLabel);
		balance.setText("BTC " + Utils.bitcoinValueToFriendlyString(appState.wallet.getBalance(BalanceType.ESTIMATED)));

		TableLayout tl = (TableLayout) findViewById(R.id.transactions);
		tl.removeAllViews();

		ArrayList<Transaction> transactions = appState.wallet.getAllTransactions();
		
		// Show only the first 100 transaction
		if (transactions.size() > 100)
			transactions = (ArrayList<Transaction>)transactions.subList(0, 99);
		
		for (Transaction tx : transactions) {
			addRowForTransaction(tl, tx);
		}
	}

	private void addRowForTransaction(TableLayout tl, Transaction tx) {
		// Create a new row to be added.
		TableRow tr = new TableRow(this);
		// Create a Button to be the row-content.
		TextView description = new TextView(this);
		TextView amount = new TextView(this);
		description.setTextSize(15);
		amount.setTextSize(15);
		String text = "";

		// check if pending
		if (appState.wallet.pending.containsKey(tx.getHash())) {
			text += "(Pending) ";
			description.setTextColor(Color.GRAY);
			amount.setTextColor(Color.GRAY);
		} else {
			description.setTextColor(Color.BLACK);
			amount.setTextColor(Color.BLACK);
		}

		// check if sent or received
		try {
			if (tx.sent(appState.wallet)) {
				text += "Sent to " + tx.outputs.get(0).getScriptPubKey().getToAddress();
				amount.setText("-" + Utils.bitcoinValueToFriendlyString(tx.amount(appState.wallet)));
			} else {
				text += "Received from " + tx.getInputs().get(0).getFromAddress();
				amount.setText("+" + Utils.bitcoinValueToFriendlyString(tx.amount(appState.wallet)));
			}
		} catch (ScriptException e) {
			// don't display this transaction
			return;
		}
		if (text.length() > 30) {
			text = text.substring(0, 29) + "...";
		}
		description.setText(text);
		description.setPadding(0, 3, 0, 3);
		amount.setPadding(0, 3, 0, 3);
		amount.setGravity(Gravity.RIGHT);
		tr.addView(description);
		tr.addView(amount);
		tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh_menu_item:
			updateBlockChain();
			updateUI();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void moneyReceived(Transaction tx) {
		appState.saveWallet();
		updateUI();
		
		if (appState.notifiedUserOfTheseTransactions.contains(tx.getHash())) {
			//only call once per transaction
			//this could be called multiple times by separate peers
			return;
		} else {
			appState.notifiedUserOfTheseTransactions.add(tx.getHash());
		}
		
		TransactionInput input = tx.getInputs().get(0);
		Address from;
		try {
			from = input.getFromAddress();
		} catch (ScriptException e) {
			return;
		}
		BigInteger value = tx.getValueSentToMe(appState.wallet);
		Log.d("Wallet", "Received " + Utils.bitcoinValueToFriendlyString(value) + " from " + from.toString());

		String ticker = Utils.bitcoinValueToFriendlyString(value) + " Bitcoins Received!";
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		Notification notification = new Notification(R.drawable.my_notification_icon, ticker, System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Context context = getApplicationContext();
		CharSequence contentTitle = ticker;
		CharSequence contentText = "From " + from.toString();
		Intent notificationIntent = new Intent(this, BitcoinWallet.class);
		//don't open a new one if it's already on top
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(1, notification);
	}
}