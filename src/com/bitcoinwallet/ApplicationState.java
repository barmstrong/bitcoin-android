package com.bitcoinwallet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerDiscovery;
import com.google.bitcoin.core.PeerDiscoveryException;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

public class ApplicationState extends Application {
	// convenient place to keep global app variables

	boolean TEST_MODE = true;
	Wallet wallet;
	String filePrefix = TEST_MODE ? "testnet" : "prodnet";

	/**
	 * Every access to walletFile must be synch'ed with this lock. Backup agent
	 * can be run at any time.
	 */
	static final Object[] walletFileLock = new Object[0];
	File walletFile;
	BackupManager backupManager;

	NetworkParameters params = TEST_MODE ? NetworkParameters.testNet()
			: NetworkParameters.prodNet();
	PeerDiscovery peerDiscovery;
	ArrayList<Peer> peers = new ArrayList<Peer>();
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
			walletFile = new File(getFilesDir(), filePrefix + ".wallet");
			try {
				wallet = Wallet.loadFromFile(walletFile);
				Log.d("Wallet", "Found wallet file to load");
			} catch (IOException e) {
				wallet = new Wallet(params);
				Log.d("Wallet", "Created new wallet");
				wallet.keychain.add(new ECKey());
				saveWallet();
			} catch (StackOverflowError e) {
				//couldn't unserialize the wallet - maybe it was saved in a previous version of bitcoinj?
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
				try {
					InputStream is = getAssets().open(
							filePrefix + ".blockchain");
					IOUtils.copy(is, new FileOutputStream(file));
				} catch (IOException e) {
					Log.d("Wallet",
							"Couldn't find initial blockchain in assets folder...starting from scratch");
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
		}
		Log.d("Wallet", "Notifying BackupManager that data has changed. Should backup soon.");
		backupManager.dataChanged();
	}

	//blocking, don't call from main activity thread
	@SuppressWarnings("unchecked")
	public synchronized ArrayList<Peer> getPeers() {
		if (peers.size() < 3) {
			discoverPeers();
		}
		// shallow clone to try and prevent concurrent modification exceptions
		// of the arraylist
		return (ArrayList<Peer>) peers.clone();
	}

	public synchronized void removeBadPeer(Peer peer) {
		Log.d("Wallet", "removing bad peer");
		peer.disconnect();
		peers.remove(peer);
	}

	private void discoverPeers() {
		try {
			ArrayList<InetSocketAddress> isas = new ArrayList<InetSocketAddress>(Arrays.asList(peerDiscovery.getPeers()));
			Collections.shuffle(isas); // try different order each time
			for (InetSocketAddress isa : isas) {
				NetworkConnection conn = null;
				try {
					conn = new NetworkConnection(isa.getAddress(), params,
							blockStore.getChainHead().getHeight(), 5000);
				} catch (IOException e) {
					Log.d("Wallet", "Discarding bad connection.");
				} catch (ProtocolException e) {
					Log.d("Wallet", "ProtocolException in discoverPeers()");
					e.printStackTrace();
				} catch (BlockStoreException e) {
					Log.d("Wallet", "BlockStoreException in discoverPeers()");
					e.printStackTrace();
				}
				if (conn != null) {
					Peer peer = new Peer(params, conn, blockChain, wallet);
					peer.start();
					if (!peers.contains(peer)) {
						peers.add(peer);
					}
					if (peers.size() >= 8) {
						return;
					}
				}
			}
			Log.d("Wallet", "Discovered " + peers.size() + " peers...");
		} catch (PeerDiscoveryException e) {
			Log.d("Wallet", "Couldn't discover peers.");
		}
	}

	public synchronized void sendTransaction(Transaction tx) {
		boolean success = false;
		for (Peer peer : getPeers()) {
			try {
				peer.broadcastTransaction(tx);
				success = true;
				Log.d("Wallet", "Broadcast succeeded");
			} catch (IOException e) {
				// should we remove bad peer here? or maybe internet just went
				// away. not sure
				Log.d("Wallet", "Broadcast failed");
			}
		}
		if (success) {
			wallet.confirmSend(tx);
			saveWallet();
		}
	}
}
