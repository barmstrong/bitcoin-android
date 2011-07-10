package com.bitcoinandroid;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;

/* Thread to download the block chain.
 * It downloads from one peer at a time, but will cycle through if one dies or stops talking to us.
 * Once it finishes downloading the blockchain it will make sure we're connected to 8 nearby peers to
 * get new blocks and messages. 
 */

public class ProgressThread extends Thread {
	ApplicationState appState = ApplicationState.current;
	
	private Peer peer;
	private CountDownLatch progress;
	private Handler mHandler;
	private final static int STATE_DONE = 0;
	private final static int STATE_RUNNING = 1;
	private int mState;
	private int total;

	public ProgressThread(Handler h) {
		mHandler = h;
		mState = STATE_RUNNING;
	}

	public void run() {
		rebuildWallet();
		downloadBlockChain();
		hideDialog();
		connectToLocalPeers();
		resendPendingTransactions();
	}
	
	// this runs through the entire blockchain again to check for missed transactions
	private void rebuildWallet(){
		if (appState.walletShouldBeRebuilt) {
			Log.d("Wallet", "Rebuilding wallet");
			appState.walletShouldBeRebuilt = false;
			try {
				//appState.wallet.checkForNewTransactions(appState.blockChain);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Error("couldn't rebuild wallet");
			}
			appState.saveWallet();
		}
	}
	
	private void downloadBlockChain() {
		Log.d("Wallet", "Downloading block chain");
		boolean done = false;
		while (!done) {
			ArrayList<InetSocketAddress> isas = appState.discoverPeers();
			if (isas.size() == 0) {
				// not connected to internet, could show a dialog to the user here?
				done = true;
			} else {
				for (InetSocketAddress isa : isas) {
					if (blockChainDownloadSuccessful(isa)) {
						done = true;
						break;
					} else {
						// remove and try next one
						appState.removeBadPeer(isa);
					}
				}
			}
		}
		hideDialog();
	}
	
	private boolean blockChainDownloadSuccessful(InetSocketAddress isa){
		NetworkConnection conn = createNetworkConnection(isa);
		if (conn == null)
			return false;
		
		long max;
		long current;
		long last_current = -1;
		
		Peer peer = new Peer(appState.params, conn, appState.blockChain, appState.wallet);
		peer.start();
		try {
			Log.d("Wallet", "Starting download from new peer");
			progress = peer.startBlockChainDownload();
			max = progress.getCount();
			current = max;
			int no_change_count = 0;
			while (true) {
				double pct = 100.0 - (100.0 * (current / (double) max));
				System.out.println(String.format("Chain download %d%% done", (int) pct));
				progress.await(1, TimeUnit.SECONDS);
				current = progress.getCount();
				if (last_current == -1)
					last_current = current;
				Log.d("Wallet", "no change count is "+no_change_count+" and current is "+current);
				if (current == 0) {
					// we're done!
					return true;
				} else if (current > (last_current - 10)) {
					no_change_count++;
					// if peer stopped talking to us for 8 seconds, or wasn't fast enough, lets break out and try next one
					if (no_change_count >= 8)
						return false;
				} else {
					// we're making progress, keep going!
					last_current = current;
					no_change_count = 0;
					Message msg = mHandler.obtainMessage();
					msg.arg1 = (int) pct;
					mHandler.sendMessage(msg);
				}
			}
		} catch (Exception e) {
			// disconnect and try next peer
			Log.d("Wallet", "exception in ProgressThread downloadBlockChainSuccessful");
			e.printStackTrace();
		} finally {
			// always calls this even if we return above
			peer.disconnect();
		}
		
		return false;
	}
	
	/**
	 * Wrapped "new NetworkConnection" in a giant executor service since it would sometimes never return, even with the connectTimeout param
	 * Copied from http://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking-method-with-a-timeout-in-java
	 */
	private NetworkConnection createNetworkConnection(final InetSocketAddress isa) {
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<NetworkConnection> task = new Callable<NetworkConnection>() {
		   public NetworkConnection call() {
			   NetworkConnection conn = null;
			   try {
				   conn = new NetworkConnection(isa.getAddress(), appState.params, appState.blockStore.getChainHead().getHeight(), 5000);
			   } catch (Exception e) {
			   }
			   return conn;
		   }
		};
		Future<NetworkConnection> future = executor.submit(task);
		NetworkConnection result = null;
		try {
			result = future.get(10, TimeUnit.SECONDS); 
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		   future.cancel(true);
		}
		return result;
	}
	
	/* connect to local peers (minimum of 3, maximum of 8) */
	private synchronized void connectToLocalPeers(){
		Log.d("Wallet", "Connecting to local peers");
		synchronized(appState.connectedPeersLock) {
			//clear out any which have disconnected
			for (Peer peer : appState.connectedPeers) {
				if (!peer.isRunning())
					appState.connectedPeers.remove(peer);
			}

			if (appState.connectedPeers.size() < 3) {
				for (InetSocketAddress isa : appState.discoverPeers()) {
					NetworkConnection conn = createNetworkConnection(isa);
					if (conn == null) {
						appState.removeBadPeer(isa);
					} else {
						Peer peer = new Peer(appState.params, conn, appState.blockChain, appState.wallet);
						peer.start();
						appState.connectedPeers.add(peer);
						if (appState.connectedPeers.size() >= 8)
							break;
					}
				}
			}
		}
	}
	
	private void resendPendingTransactions(){
		Log.d("Wallet", "Resending pendings transactions");
		for (Transaction tx : appState.wallet.pending.values()) {
			if (tx.sent(appState.wallet)) {
				Log.d("Wallet", "resending");
				appState.sendTransaction(tx);
			}
		}
	}

	private void hideDialog() {
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