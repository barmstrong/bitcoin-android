package com.bitcoinandroid;

import java.io.IOException;
import java.math.BigInteger;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * @author Brandon To request a backup, call dataChanged() To request a restore,
 *         call requestRestore()
 */
public class WalletBackupAgent extends BackupAgentHelper {
	static final String FILES_BACKUP_KEY = "wallet_files";

	@Override
	public void onCreate() {
		FileBackupHelper helper = new FileBackupHelper(this,
				ApplicationState.current.walletFile.getName());
		addHelper(FILES_BACKUP_KEY, helper);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		// Hold the lock while the FileBackupHelper performs backup
		synchronized (ApplicationState.walletFileLock) {
			// Right now, we only keep one backup file in the cloud. The
			// "authoritative" file is the one on the phone. So we don't do any
			// checking to see what we're replacing in the cloud backup.
			Log.d("Wallet", "Backing up wallet file to cloud.");
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		// Hold the lock while the FileBackupHelper restores the file
		synchronized (ApplicationState.walletFileLock) {
			// Ensure that the wallet on the phone has zero balance.
			// We don't want to over-write a wallet file on the phone that has
			// BTC in it.
			if (ApplicationState.current.wallet.getBalance().compareTo(
					BigInteger.ZERO) > 0) {
				Log.d("Wallet",
						"Wallet on phone has a balance. Skipping restore.");
				return;
			}
			Log.d("Wallet", "Restoring wallet file from backup.");
			super.onRestore(data, appVersionCode, newState);
		}
	}
}
