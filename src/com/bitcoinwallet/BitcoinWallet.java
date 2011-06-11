package com.bitcoinwallet;

import android.app.Activity;
import android.app.ProgressDialog;
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

import com.google.zxing.client.android.CaptureActivity;

public class BitcoinWallet extends Activity {

	ProgressThread progressThread;
	ProgressDialog progressDialog;

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

		((ApplicationState) getApplicationContext()).setup();

		updateBlockChain();
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
				} else if (total >= 100.0) {
					progressDialog.hide();
					progressBar.setVisibility(View.GONE);
					Log.d("Wallet", "Download complete");
				}
				
				//peer dropped connection, restarting
				if (msg.arg2 == 1){
					progressThread.start();
				}
			}
		};
		((ProgressBar) findViewById(R.id.progressBar))
				.setVisibility(View.VISIBLE);

		progressThread = new ProgressThread(handler, this);
		progressThread.start();
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

}