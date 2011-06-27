package com.bitcoinwallet;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;

public class ProgressThread extends Thread {
	Peer peer;
	CountDownLatch progress;
	Handler mHandler;
	BitcoinWallet parent;
	final static int STATE_DONE = 0;
	final static int STATE_RUNNING = 1;
	int mState;
	int total;

	public ProgressThread(Handler h, BitcoinWallet p) {
		mHandler = h;
		parent = p;
		mState = STATE_RUNNING;
	}
	
	public void run() {
		ApplicationState appState = ApplicationState.current;
		
		long max;
		long current;

		for(Peer peer : appState.getPeers()){
			try {
				progress = peer.startBlockChainDownload();
				max = progress.getCount();
				current = max;
				int no_change_count = 0;
				while (current > 0) {
				    double pct = 100.0 - (100.0 * (current / (double)max));
				    System.out.println(String.format("Chain download %d%% done", (int)pct));
				    progress.await(1, TimeUnit.SECONDS);
				    long tmp = progress.getCount();
				    if (tmp == current && no_change_count++ > 2){
				    	appState.removeBadPeer(peer);
				    	break;
				    } else {
				    	current = tmp;
				    	no_change_count = 0;
				    	Message msg = mHandler.obtainMessage();
						msg.arg1 = (int) pct;
						mHandler.sendMessage(msg);
				    }
				}
				if(current == 0){
					break;
				}
			} catch (IOException e){
				//try next peer
			} catch (InterruptedException e){
				//try next peer
			} catch (IllegalArgumentException e){
				//try next peer
			}
		}
		
		//take a second look at any pending transactions, we may need to rebroadcast them
		for(Transaction tx : appState.wallet.pending.values()){
			if (tx.sent(appState.wallet)) {
				Log.d("Wallet", "resending");
				appState.sendTransaction(tx);
			}
		}
		
		// ensure dialog closes if we catch an exception
		hideDialog();
	}
	
	public void hideDialog(){
		Message msg = mHandler.obtainMessage();
		msg.arg1 = 100;
		mHandler.sendMessage(msg);
	}

	/*
	 * sets the current state for the thread, used to stop the thread
	 */
	public void setState(int state) {
		mState = state;
	}
}