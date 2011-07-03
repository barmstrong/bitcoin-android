Bitcoin Wallet
==============

Send and receive bitcoins from your Android phone!

Note that this app is still under development and may lose your coins!  Test it with small amounts.

You can [download it from the Android Market here](http://example.com/) or search for "bitcoin" from your mobile device.

<table>
  <tr>
    <td><img src="http://i.imgur.com/aqF3p.png" width="200" /></td>    
    <td><img src="http://i.imgur.com/ilvNp.png" width="200" /></td>
    <td><img src="http://i.imgur.com/ObBth.png" width="200" /></td>
    <td><img src="http://i.imgur.com/TsZc7.png" width="200" /></td>
  </tr>
</table>

Features
--------

* Send/Receive bitcoins entirely from your phone (no server component required)
* Scan and generate QR codes to share address, amount, etc between devices
* Recognizes the Bitcoin [URI format](https://en.bitcoin.it/wiki/URI_Scheme)
* Creates transactions in the absence of internet (will retry when you reconnect)
* Email invoices from your phone to request money
* Wallet file backed up in the cloud (synced to your Google account) in case you lose your phone.

Installation
-------------

You can [download and install the app](http://example.com/) from the Android Market.

Or search for "bitcoin" from your mobile device.

For best results, install the app while connected to WiFi (it will download some data the first time you launch it).

Usage
-----

Check out the wiki page on [using your bitcoin wallet](https://github.com/barmstrong/bitcoin-wallet/wiki/Using-Your-Bitcoin-Wallet).

Technical Details
-----------------

Bitcoin Wallet is based on the excellent [bitcoinj](http://code.google.com/p/bitcoinj/) library created by Mike Hearn at Google.

It's a full bitcoin client running on your phone.  This means it keeps it's own copy of the blockchain (currently about 12MB) which we store on your phone's SD card, and it doesn't need to communicate with a centralized server.  Most phones have large SD cards (8GB or more is not uncommon) so storing 12MB isn't too bad.  The wallet file is stored on the phone's internal memory instead since it quite small compared to the blockchain.  We include a recent copy of the blockchain in the actual app package that you download from the Android Market so you don't need to download and build the whole thing from scratch.  The first time you run it, it will contact nearby peers and update the included blockchain to the most recent version.  This usually takes a minute or less over WiFi.  After that, updates to the blockchain and transaction notifications should be near instantaneous (a few seconds) over 3G or WiFi.

If your phone isn't connected to the internet, you can still create transactions (send money).  Your wallet will retry sending them once you regain internet access (tapping "Refresh" home screen will do this right away).  Of course, recipients will not receive the transaction until your phone reconnects to the internet, so if they are expecting to receive confirmation right away then this wouldn't work without internet access on your phone.

Because Bitcoin Wallet is a full bitcoin client, this also means that your "money" is stored on the device itself.  If you lose your phone you could potentially lose your money (just like if you lost your real wallet you'd lose the cash inside).  So you should probably never carry more money on your phone than you'd feel comfortable carrying in your real wallet.  In practice, the coins may be recoverable because we sync your wallet file to your Google Account in the cloud after each transaction.  If you did lose your phone you could login to your Google Account on another Android device, install the app, and once again have access to your coins.  (At that point you'd probably want to send them somewhere safe and get them off the phone since the thief could be doing the same thing.)

Note that since this is beta software you probably shouldn't store ANY amount of bitcoins you are unwilling to lose at this point.  Or if you do, do so at your own risk.

Contributing
------------

Feel free to fork and send pull requests.  Contributions welcome.  This is free, open source software!

TODO
----

* optionally require a pin code to send money?
* ability to generate more than one receive address
* make backup to the cloud optional for those who want the privacy
* move to a "thin client" implementation instead of storing the entire blockchain (this will become necessary as the blockchain grows over time)
* make an easy link to the Bitcoin Faucet for people to get a few coins on the device when they first try it
* clicking a [Bitcoin link](https://en.bitcoin.it/wiki/URI_Scheme) in the mobile browser should open the send money screen (with fields filled in)

Credits
-------

Bitcoin Wallet is based on the [bitcoinj](http://code.google.com/p/bitcoinj/) library by Mike Hearn at Google.  He deserves some very special thanks!

It also makes use of the [zxing](http://code.google.com/p/zxing/) library for processing QR codes.  The icon was created by the folks at [visualpharm.com](http://www.visualpharm.com/).

License
-------

Bitcoin Wallet is open source software released under the Apache V2 license.
