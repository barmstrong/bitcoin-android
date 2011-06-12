package com.bitcoinwallet;

import java.math.BigInteger;
import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.core.Wallet.BalanceType;
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
				startActivity(new Intent(BitcoinWallet.this,
						CaptureActivity.class));
			}
		});

		Button receiveButton = (Button) this.findViewById(R.id.receive_button);
		receiveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(BitcoinWallet.this, ReceiveMoney.class));
			}
		});

		appState = ((ApplicationState) getApplicationContext());
		appState.setup();
		
		updateUIBalance();
		updateBlockChain();
		
		appState.wallet.addEventListener(new WalletEventListener() {
			public void onCoinsReceived(Wallet w, final Transaction tx,
					BigInteger prevBalance, BigInteger newBalance) {
				Log.d("Wallet", "COINS RECEIVED. not yet in ui thread");
				runOnUiThread(new Runnable() {
					public void run() {
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
					progressDialog.show();
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
		((ProgressBar) findViewById(R.id.progressBar))
				.setVisibility(View.VISIBLE);

		progressThread = new ProgressThread(handler, this);
		progressThread.start();
	}

	private void updateUIBalance() {
		TextView balance = (TextView) findViewById(R.id.balanceLabel);
		balance.setText(Utils.bitcoinValueToFriendlyString(appState.wallet.getBalance(BalanceType.ESTIMATED)));
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

	private void moneyReceivedAlert(Transaction tx)
			throws ScriptException {
		TransactionInput input = tx.getInputs().get(0);
		Address from = input.getFromAddress();
		BigInteger value = tx.getValueSentToMe(appState.wallet);
		Log.d("Wallet", "Received " + Utils.bitcoinValueToFriendlyString(value)
				+ " from " + from.toString());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Received " + Utils.bitcoinValueToFriendlyString(value)
						+ " from " + from.toString()).setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

}