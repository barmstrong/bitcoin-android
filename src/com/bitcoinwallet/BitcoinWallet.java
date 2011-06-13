package com.bitcoinwallet;

import java.math.BigInteger;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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

		updateUIBalance();
		updateBlockChain();

		appState.wallet.addEventListener(new WalletEventListener() {
			public void onCoinsReceived(Wallet w, final Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
				runOnUiThread(new Runnable() {
					public void run() {
						appState.saveWallet();
						Log.d("Wallet", "COINS RECEIVED!");
						try {
							moneyReceivedAlert(tx);
						} catch (ScriptException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});		
	}

	private void updateBlockChain() {
		for(Transaction tran : appState.wallet.getPendingTransactions()){
			final TransactionInput input = tran.getInputs().get(0);
            Address from = null;
			try {
				from = input.getFromAddress();
			} catch (ScriptException e) {
				e.printStackTrace();
			}
            final BigInteger value = tran.getValueSentToMe(appState.wallet);
            Log.d("Wallet", "====> Got "+value+" from "+from);
		}
		
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
					updateUIBalance();
					Log.d("Wallet", "Download complete");
				}
			}
		};
		((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);

		progressThread = new ProgressThread(handler, this);
		progressThread.start();
		updateUIBalance();
	}

	private void updateUIBalance() {
		TextView balance = (TextView) findViewById(R.id.balanceLabel);
		balance.setText(Utils.bitcoinValueToFriendlyString(appState.wallet.getBalance(BalanceType.ESTIMATED)) + " BTC");
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
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void moneyReceivedAlert(Transaction tx) throws ScriptException {
		updateUIBalance();
		TransactionInput input = tx.getInputs().get(0);
		Address from = input.getFromAddress();
		BigInteger value = tx.getValueSentToMe(appState.wallet);
		Log.d("Wallet", "Received " + Utils.bitcoinValueToFriendlyString(value) + " from " + from.toString());
		
		
		String ticker = "You just received " + Utils.bitcoinValueToFriendlyString(value) + " BTC from " + from.toString();
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		Notification notification = new Notification(R.drawable.my_notification_icon, ticker, System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Context context = getApplicationContext();
		CharSequence contentTitle = Utils.bitcoinValueToFriendlyString(value)+" Bitcoins Received!";
		CharSequence contentText = "From "+from.toString();
		Intent notificationIntent = new Intent(this, BitcoinWallet.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(1, notification);
	}
}