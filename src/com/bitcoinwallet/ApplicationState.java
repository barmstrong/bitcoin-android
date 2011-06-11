package com.bitcoinwallet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.BlockStore;
import com.google.bitcoin.core.BlockStoreException;
import com.google.bitcoin.core.BoundedOverheadBlockStore;
import com.google.bitcoin.core.DnsDiscovery;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerDiscovery;
import com.google.bitcoin.core.PeerDiscoveryException;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;

public class ApplicationState extends Application {
	// convenient place to keep global app variables

	boolean TEST_MODE = true;
	Wallet wallet;
	String filePrefix = TEST_MODE ? "android_testnet" : "android_prodnet";
	String walletFilename = filePrefix + ".wallet";
	NetworkParameters params = TEST_MODE ? NetworkParameters.testNet()
			: NetworkParameters.prodNet();
	PeerDiscovery peerDiscovery = new DnsDiscovery(params);
	ArrayList<InetSocketAddress> peers = new ArrayList<InetSocketAddress>();
	BlockStore blockStore = null;

	public static ApplicationState current;

	// starting up
	public void setup() {
		Log.d("Wallet", "Starting app");
		ApplicationState.current = (ApplicationState) getApplicationContext();

		Log.d("Wallet", "Reading block store from disk");
		try {
			File file = new File(getExternalFilesDir(null), filePrefix + ".blockchain");
			if(!file.exists() && !TEST_MODE){
				Log.d("Wallet", "Copying initial blockchain from assets folder");
				try {
					InputStream is = getAssets().open(filePrefix + ".blockchain");
					IOUtils.copy(is,new FileOutputStream(file));
				} catch (IOException e) {
					e.printStackTrace();
					throw new Error("Couldn't find initial blockchain in assets folder");
				}
			}
			blockStore = new BoundedOverheadBlockStore(params, file);
		} catch (BlockStoreException bse) {
			throw new Error("Couldn't store block.");
		}

		// try reading file or create a new one
		try {
			wallet = Wallet.loadFromFileStream(openFileInput(walletFilename));
		} catch (IOException e) {
			wallet = new Wallet(params);
			wallet.keychain.add(new ECKey());
			saveWallet();
		}

		wallet.addEventListener(new WalletEventListener() {
			public void onCoinsReceived(Wallet w, Transaction tx,
					BigInteger prevBalance, BigInteger newBalance) {
				// Running on a peer thread.
				assert !newBalance.equals(BigInteger.ZERO);
				wallet = w;
				saveWallet();
				// startActivity(new Intent(ApplicationState.this,
				// CaptureActivity.class));
				try {
					moneyReceivedAlert(w, tx);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void moneyReceivedAlert(Wallet w, Transaction tx)
			throws ScriptException {
		TransactionInput input = tx.getInputs().get(0);
		Address from = input.getFromAddress();
		BigInteger value = tx.getValueSentToMe(w);
		System.out.println("Received "
				+ Utils.bitcoinValueToFriendlyString(value) + " from "
				+ from.toString());

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

	private void saveWallet() {
		try {
			wallet.saveToFile(new File(getExternalFilesDir(null), walletFilename));
		} catch (IOException e) {
			throw new Error("Can't save wallet file.");
		}
	}

	public BlockStore getBlockStore() {
		return blockStore;
	}

	public void setBlockStore(BlockStore bs) {
		blockStore = bs;
	}
	
	public void removeBadPeer(){
		if(peers.size() > 0)
			peers.remove(0);
	}

	public NetworkConnection getNetworkConnection() {
		if (peers.size() < 5) {
			try {
				Log.d("Wallet", "Discovering peers...");
				peers = new ArrayList<InetSocketAddress>(
						Arrays.asList(peerDiscovery.getPeers()));
				Log.d("Wallet", "Discovered " + peers.size() + " peers...");
			} catch (PeerDiscoveryException e) {
				throw new Error(
						"Couldn't connect to network.  Please try again later.");
			}
		}
		Log.d("Wallet", "Connecting to peers..."+peers.size());
		NetworkConnection conn = null;
		Iterator<InetSocketAddress> itr = peers.iterator();
		while (itr.hasNext()) {
			try {
				conn = new NetworkConnection(itr.next().getAddress(), params,
						blockStore.getChainHead().getHeight(), 5000);
				break;
			} catch (Exception e) {
				itr.remove();
				// remove it and try next one
			}
		}

		if (conn == null) {
			throw new Error(
					"Could not connect to the network.  Please try again later.");
		}
		return conn;
	}
}
