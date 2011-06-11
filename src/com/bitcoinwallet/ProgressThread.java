package com.bitcoinwallet;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.Peer;

public class ProgressThread extends Thread {
	Peer peer;
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

		BlockChain chain = new BlockChain(appState.params, appState.wallet,
				appState.blockStore);
		peer = new Peer(appState.params, appState.getNetworkConnection(), chain);
		peer.start();

		Log.d("Wallet", "Starting download.");
		CountDownLatch progress;
		try {
			progress = peer.startBlockChainDownload();
			long max = progress.getCount();  // Racy but no big deal.
	        if (max > 0) {
	        	System.out.println("Downloading " + max + " blocks. "
						+ (max > 1000 ? "This may take a while." : ""));
	        	
	        	//hack since it's not disconnecting cleanly
	        	int retries_since_last_change = 0;
	        	long tmp;
	        	
	            long current = max;
	            while (current > 0) {
	                double pct = 100.0 - (100.0 * (current / (double)max));
	                System.out.println(String.format("Chain download %d%% done", (int)pct));
	                Message msg = mHandler.obtainMessage();
					msg.arg1 = (int) pct;
					mHandler.sendMessage(msg);
	                progress.await(1, TimeUnit.SECONDS);
	                
	                tmp = progress.getCount();
	                if(tmp == current){
	                	retries_since_last_change += 1;
	                } else {
	                	retries_since_last_change = 0;
	                	current = tmp;
	                }
	                
	                if (retries_since_last_change > 10){
	                	Log.d("Wallet", "removing bad peer and restarting");
	                	appState.removeBadPeer();
	                	//restart thread
	                	Message restartMsg = mHandler.obtainMessage();
	                	restartMsg.arg2 = 1;
	            		mHandler.sendMessage(restartMsg);
	            		break;
	                }
	            }
	        }
		} catch (IllegalArgumentException e) {
			//zero blocks to download
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			Log.e("ERROR", "Thread Interrupted");
		}
		
		// ensure dialog closes if we catch an exception
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