package com.bitcoinwallet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.util.Log;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.BlockStore;
import com.google.bitcoin.core.BlockStoreException;
import com.google.bitcoin.core.BoundedOverheadBlockStore;
import com.google.bitcoin.core.DnsDiscovery;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerDiscovery;
import com.google.bitcoin.core.PeerDiscoveryException;
import com.google.bitcoin.core.Wallet;

public class ApplicationState extends Application {
	// convenient place to keep global app variables

	boolean TEST_MODE = true;
	Wallet wallet;
	String filePrefix = TEST_MODE ? "testnet" : "prodnet";
	File walletFile;
	NetworkParameters params = TEST_MODE ? NetworkParameters.testNet()
			: NetworkParameters.prodNet();
	PeerDiscovery peerDiscovery;
	ArrayList<InetSocketAddress> peers = new ArrayList<InetSocketAddress>();
	BlockStore blockStore = null;
	BlockChain blockChain;
	Peer currentPeer;

	public static ApplicationState current;
	
	@Override
	public void onCreate(){
		Log.d("Wallet", "Starting app");
		ApplicationState.current = (ApplicationState) this;
		
		
		// read or create wallet
		walletFile = new File(getFilesDir(), filePrefix + ".wallet");
		try {
			wallet = Wallet.loadFromFile(walletFile);
			Log.d("Wallet", "Found wallet file to load");
		} catch (IOException e){
			wallet = new Wallet(params);
			Log.d("Wallet", "Created new wallet");
			wallet.keychain.add(new ECKey());
			saveWallet();
		}
		
		peerDiscovery = new DnsDiscovery(params);

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
			blockChain = new BlockChain(params, wallet, blockStore);
		} catch (BlockStoreException bse) {
			throw new Error("Couldn't store block.");
		}
	}
	
	public void saveWallet() {
		Log.d("Wallet", "Saving wallet");
		try {
			wallet.saveToFile(walletFile);
		} catch (IOException e) {
			throw new Error("Can't save wallet file.");
		}
	}
	
	public void removeBadPeer() {
		Log.d("Wallet", "removing bad peer");
		if (peers.size() > 0)
			peers.remove(0);
		else {
			discoverPeers();
		}
	}
	
	public void discoverPeers(){
		try {
			peers = new ArrayList<InetSocketAddress>(
						Arrays.asList(peerDiscovery.getPeers()));
			Log.d("Wallet", "Discovered " + peers.size() + " peers...");
		} catch (PeerDiscoveryException e) {
			Log.d("Wallet", "Couldn't discover peers.");
		}
	}

	public Peer getPeer() {
		if (peers.size() == 0) {
			discoverPeers();
		}
		Log.d("Wallet", "Connecting to peers...");
		NetworkConnection conn = null;
		
		while (!peers.isEmpty() && conn == null) {
			try {
				conn = new NetworkConnection(peers.get(0).getAddress(), params,
						blockStore.getChainHead().getHeight(), 5000);
			} catch (Exception e) {
				try {
					removeBadPeer();
				}
				catch(IndexOutOfBoundsException e2) {
					// already at 0...
				}
			}
		}
		
		if (conn == null)
			return null;
		
		currentPeer = new Peer(params, conn, blockChain);
		
		return currentPeer;
	}
}
