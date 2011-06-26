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
import com.google.bitcoin.core.Wallet;

public class ApplicationState extends Application {
	// convenient place to keep global app variables

	boolean TEST_MODE = false;
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

	@SuppressWarnings("unchecked")
	public ArrayList<Peer> getPeers() {
		if (peers.size() == 0) {
			discoverPeers();
		}
		// shallow clone to try and prevent concurrent modification exceptions
		// of the ArrayList
		return (ArrayList<Peer>) peers.clone();
	}

	public void removeBadPeer(Peer peer) {
		Log.d("Wallet", "removing bad peer");
		peer.disconnect();
		peers.remove(0);
	}

	public void discoverPeers() {
		try {
			InetSocketAddress[] isas = peerDiscovery.getPeers();
			ArrayList<InetSocketAddress> isas2 = new ArrayList<InetSocketAddress>(
					Arrays.asList(isas));
			Collections.shuffle(isas2); // try different order each time
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
					peers.add(peer);
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
}
