package com.bitcoinandroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.app.backup.BackupManager;
import android.util.Log;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.BlockStore;
import com.google.bitcoin.core.BlockStoreException;
import com.google.bitcoin.core.BoundedOverheadBlockStore;
import com.google.bitcoin.core.DnsDiscovery;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.IrcDiscovery;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerDiscovery;
import com.google.bitcoin.core.PeerDiscoveryException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

public class ApplicationState extends Application {
	// convenient place to keep global app variables

	private boolean TEST_MODE = true;
	Wallet wallet;
	private String filePrefix = TEST_MODE ? "testnet" : "prodnet";

	/**
	 * Every access to walletFile must be synch'ed with this lock. Backup agent
	 * can be run at any time.
	 */
	static final Object[] walletFileLock = new Object[0];
	File walletFile;
	File keychainFile;
	boolean walletShouldBeRebuilt = false;
	private BackupManager backupManager;

	NetworkParameters params =
		TEST_MODE ? NetworkParameters.testNet() : NetworkParameters.prodNet();
	private PeerDiscovery peerDiscovery;
	private ArrayList<InetSocketAddress> isas = new ArrayList<InetSocketAddress>();

	static final Object[] connectedPeersLock = new Object[0];
	ArrayList<Peer> connectedPeers = new ArrayList<Peer>(); // protected by above lock

	BlockStore blockStore = null;
	BlockChain blockChain;
	
	//tracks which transactions we've shown a notification for so we couldn't duplicate them
	//could probably persist this between start/top of the app, but fine for now
	ArrayList<Sha256Hash> notifiedUserOfTheseTransactions = new ArrayList<Sha256Hash>();

	public static ApplicationState current;

	@Override
	public void onCreate() {
		Log.d("Wallet", "Starting app");
		ApplicationState.current = (ApplicationState) this;
		backupManager = new BackupManager(this);

		// read or create wallet
		synchronized (ApplicationState.walletFileLock) {
			keychainFile = new File(getFilesDir(), filePrefix+".keychain");
			walletFile = new File(getFilesDir(), filePrefix + ".wallet");
			try {
				//throw new RuntimeException("asd");
				wallet = Wallet.loadFromFile(walletFile);
				Log.d("Wallet", "Found wallet file to load");
			} catch (Exception e) {
				e.printStackTrace();
				wallet = new Wallet(params);
				Log.d("Wallet", "Created new wallet...now attempting to reset prior keys");
				
				//load any previous keys in case this IOException was due to a serialization change of a previous wallet
				try {
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(keychainFile));
					@SuppressWarnings("unchecked")
					ArrayList<ECKey> keys = (ArrayList<ECKey>) ois.readObject();
					for (ECKey key : keys) {
						wallet.keychain.add(key);
					}
					walletShouldBeRebuilt = true;
				} catch (Exception e2) {
					Log.d("Wallet", "No prior keys found, a brand new wallet!");
					wallet.keychain.add(new ECKey());
				}
				saveWallet();
			} catch (StackOverflowError e) {
				//couldn't deserialize the wallet - maybe it was saved in a previous version of bitcoinj?
				e.printStackTrace();
			}
		}
		
		if (TEST_MODE) {
			peerDiscovery = new IrcDiscovery("#bitcoinTEST");
		} else {
			peerDiscovery = new DnsDiscovery(params);
		}
		
		Log.d("Wallet", "Reading block store from disk");
		try {
			File file = new File(getExternalFilesDir(null), filePrefix
					+ ".blockchain");
			if (!file.exists()) {
				Log.d("Wallet", "Copying initial blockchain from assets folder");
				InputStream is = null;
				try {
					is = getAssets().open(
							filePrefix + ".blockchain");
					IOUtils.copy(is, new FileOutputStream(file));
				} catch (IOException e) {
					Log.d("Wallet",
							"Couldn't find initial blockchain in assets folder...starting from scratch");
				}
				finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			blockStore = new BoundedOverheadBlockStore(params, file);
			blockChain = new BlockChain(params, wallet, blockStore);
		} catch (BlockStoreException bse) {
			throw new Error("Couldn't store block.");
		}
	}

	public void saveWallet() {
		synchronized (ApplicationState.walletFileLock) {
			Log.d("Wallet", "Saving wallet");
			try {
				wallet.saveToFile(walletFile);
			} catch (IOException e) {
				throw new Error("Can't save wallet file.");
			}
			
			// save keys also in case we need to rebuild wallet later (serialization changes)
			ObjectOutputStream keychain;
			try {
				keychain = new ObjectOutputStream(new FileOutputStream(keychainFile));
				keychain.writeObject(wallet.keychain);
				keychain.flush();
				keychain.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Can't save keychain file.");
			}
		}
		Log.d("Wallet", "Notifying BackupManager that data has changed. Should backup soon.");
		backupManager.dataChanged();
	}

	public synchronized void removeBadPeer(InetSocketAddress isa) {
		Log.d("Wallet", "removing bad peer");
		isas.remove(isa);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<InetSocketAddress> discoverPeers() {
		if (isas.size() == 0) {
			try {
				isas.addAll(Arrays.asList(peerDiscovery.getPeers()));
				Collections.shuffle(isas); // try different order each time
			} catch (PeerDiscoveryException e) {
				Log.d("Wallet", "Couldn't discover peers.");
			}
		}
		Log.d("Wallet", "discoverPeers returning "+isas.size()+" peers");
		// shallow clone to prevent concurrent modification exceptions
		return (ArrayList<InetSocketAddress>) isas.clone();
	}

	/* rebroadcast pending transactions to all connected peers */
	public void sendTransaction(Transaction tx) {
		boolean success = false;
		synchronized (connectedPeersLock) {
			for (Peer peer : connectedPeers) {
				try {
					peer.broadcastTransaction(tx);
					success = true;
					Log.d("Wallet", "Broadcast succeeded");
				} catch (IOException e) {
					peer.disconnect();
					connectedPeers.remove(peer);
					Log.d("Wallet", "Broadcast failed");
				}
			}
		}
		if (success) {
			wallet.confirmSend(tx);
			saveWallet();
		}
	}
}
