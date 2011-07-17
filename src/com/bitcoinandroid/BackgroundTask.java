package com.bitcoinandroid;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;

public class BackgroundTask extends AsyncTask<String, Integer, Boolean> {
	ApplicationState appState = ApplicationState.current;
	BitcoinWallet activity;
	public ProgressDialog downloadingProgress;
	private CountDownLatch progress;
	private long remaining = 0;

	public BackgroundTask(BitcoinWallet parent) {
		this.activity = parent;

		downloadingProgress = new ProgressDialog(activity);
		downloadingProgress.setMessage(activity.getString(R.string.connecting_to_peers));
		downloadingProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		downloadingProgress.setButton(activity.getString(R.string.run_in_background), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (activity.settings.getBoolean("firstRun", true)) {
					SharedPreferences.Editor editor = activity.settings.edit();
					editor.putBoolean("firstRun", false);
					editor.commit();
				}
				downloadingProgress.dismiss();
			}
		});
		if (activity.settings.getBoolean("firstRun", true)) {
			downloadingProgress.show();
		}
	}

	protected void onPreExecute() {
	}

	protected void onProgressUpdate(Integer... progress) {
		int current = (int) progress[0];
		int max = (int) progress[1];
		downloadingProgress.setMessage(activity.getString((int) progress[2]));
		if (current >= max) {
			downloadingProgress.dismiss();
			activity.hideSpinner();
		} else {
			downloadingProgress.setMax(max);
			downloadingProgress.setProgress(max - current);
		}
	}

	protected void onPostExecute(final Boolean success) {
	}

	protected Boolean doInBackground(final String... args) {
		downloadBlockChain();
		connectToLocalPeers();
		resendPendingTransactions();
		Log.d("Wallet", "Ending background task");
		hideDialog();
		return true;
	}

	private void downloadBlockChain() {
		Log.d("Wallet", "Downloading block chain");

		remaining = 0;
		boolean done = false;
		while (!done) {
			ArrayList<InetSocketAddress> isas = appState.discoverPeers();
			if (isas.size() == 0) {
				// not connected to internet, could show a dialog to the user
				// here?
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

	private boolean blockChainDownloadSuccessful(InetSocketAddress isa) {
		NetworkConnection conn = createNetworkConnection(isa);
		if (conn == null)
			return false;

		long current;
		long last_current;
		int no_change_count;

		publishProgress(-1, 100, R.string.syncing_with_network);

		Peer peer = new Peer(appState.params, conn, appState.blockChain, appState.wallet);
		peer.start();
		try {
			Log.d("Wallet", "Starting download from new peer");
			progress = peer.startBlockChainDownload();
			current = progress.getCount();
			last_current = current;
			if (current > remaining)
				remaining = current; // new max

			no_change_count = 0;
			while (true) {
				double pct = 100.0 - (100.0 * (current / (double) remaining));
				System.out.println(String.format("Chain download %d%% done", (int) pct));
				progress.await(1, TimeUnit.SECONDS);
				current = progress.getCount();

				Log.d("Wallet", "no change count is " + no_change_count + " and current is " + current);
				if (current == 0) {
					// we're done!
					return true;
				} else if (current > (last_current - 10)) {
					no_change_count++;
					// if peer stopped talking to us for 8 seconds, or wasn't
					// fast enough, lets break out and try next one
					if (no_change_count >= 8)
						return false;
				} else {
					// we're making progress, keep going!
					last_current = current;
					no_change_count = 0;

					publishProgress((int) current, (int) remaining, R.string.syncing_with_network);
				}
			}
		} catch (InterruptedException e) {
			Log.d("Wallet", "InterruptedException in blockChainDownloadSuccessful");
			e.printStackTrace();
		} catch (IOException e) {
			Log.d("Wallet", "IOException in blockChainDownloadSuccessful");
			e.printStackTrace();
		} finally {
			// always calls this even if we return above
			peer.disconnect();
		}

		return false;
	}
	
	private void hideDialog() {
		publishProgress((int) remaining, (int) remaining, R.string.syncing_with_network);
	}

	/**
	 * Wrapped "new NetworkConnection" in a giant executor service since it
	 * would sometimes never return, even with the connectTimeout param Copied
	 * from
	 * http://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking
	 * -method-with-a-timeout-in-java
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
	private synchronized void connectToLocalPeers() {
		Log.d("Wallet", "Connecting to local peers");
		synchronized (appState.connectedPeersLock) {
			// clear out any which have disconnected
			for (Peer peer : appState.connectedPeers) {
				if (!peer.isRunning())
					appState.connectedPeers.remove(peer);
			}

			if (appState.connectedPeers.size() < 3) {
				for (InetSocketAddress isa : appState.discoverPeers()) {
					publishProgress(8 - appState.connectedPeers.size(), 8, R.string.connecting_to_peers);
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

	private void resendPendingTransactions() {
		Log.d("Wallet", "Resending pendings transactions");
		for (Transaction tx : appState.wallet.pending.values()) {
			if (tx.sent(appState.wallet)) {
				Log.d("Wallet", "resending");
				appState.sendTransaction(tx);
			}
		}
	}
}
