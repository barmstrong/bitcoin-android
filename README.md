Bitcoin Android
==============

> NOTE: This app is no longer supported or under development.  Please visit [Coinbase.com](https://coinbase.com) for the latest (this is my full time project going forward).  The app below was an early experiment in bitcoin, and it had some limitations since it was operating a full bitcoin client on the mobile device (which used lots of storage, bandwidth, and CPU - took too long to sync with the blockchain).  My future mobile apps will make use of the [Coinbase API](https://coinbase.com/api/doc) so that the heavy lifting can be done in the cloud while the mobile device acts as a thin client.


Send and receive bitcoins from your Android phone!

Note that this app is still under development and may lose your coins!  Test it with small amounts.

You can [get it from the Android Market here](https://market.android.com/details?id=com.bitcoinandroid) and click "Install" to send it to your phone.

<table>
  <tr>
    <td><img src="https://github.com/barmstrong/bitcoin-android/raw/master/screenshots/btc1.png" width="200" /></td>    
    <td><img src="https://github.com/barmstrong/bitcoin-android/raw/master/screenshots/btc2.png" width="200" /></td>
    <td><img src="https://github.com/barmstrong/bitcoin-android/raw/master/screenshots/btc3.png" width="200" /></td>
    <td><img src="https://github.com/barmstrong/bitcoin-android/raw/master/screenshots/btc4.png" width="200" /></td>
  </tr>
</table>

**Checkout a [YouTube Demo Video Here](http://www.youtube.com/watch?v=W6EucS5RS24)**

Features
--------

* Send/Receive bitcoins entirely from your phone (no server component required)
* Scan and generate QR codes to share address, amount, etc between devices
* Recognizes the Bitcoin [URI format](https://en.bitcoin.it/wiki/URI_Scheme)
* Creates transactions in the absence of internet (will retry when you reconnect)
* Email invoices from your phone to request money
* Wallet file backed up in the cloud (synced to your Google account) in case you lose your phone.
* A free app, 100% open source

Installation
-------------

You can [download and install the app](https://market.android.com/details?id=com.bitcoinandroid) from the Android Market.

For best results, install the app while connected to WiFi (it will download some data the first time you launch it).

Next, get a few coins in your wallet by starting the app and tapping "Request Money".  From here you can send a request to yourself by email.  The request will contain your wallet's receive address which you can paste into your desktop client or the [bitcoin faucet](https://freebitcoins.appspot.com/) to add funds to your wallet.

Usage
-----

Check out the wiki page on [using bitcoin android](https://github.com/barmstrong/bitcoin-android/wiki/Using-Your-Bitcoin-Wallet).

Technical Details
-----------------

Check out the wiki page on [technical details](https://github.com/barmstrong/bitcoin-android/wiki/Technical-Details).

Contributing
------------

Feel free to fork and send [pull requests](http://help.github.com/fork-a-repo/).  Contributions welcome.

Donations
---------

To support ongoing development of bitcoin android try [scanning this QR code](http://qrcode.kaywa.com/img.php?s=6&d=bitcoin%3A1Gp1himAQ4jmmQJ5zZwQxKmv7yg4Drm5M4%3Fmessage%3DThank%2520you%2520for%2520supporting%2520bitcoin%2520android%21) from your phone or sending donations to 1Gp1himAQ4jmmQJ5zZwQxKmv7yg4Drm5M4

TODO
----

* optionally require a pin code to send money for additional security?
* ability to generate more than one receive address
* make backup to the cloud optional for those who want the privacy
* move to a "thin client" implementation instead of storing the entire blockchain (this will become necessary as the blockchain grows over time)
* make an easy link to the Bitcoin Faucet for people to get a few coins on the device when they first try it
* clicking a [bitcoin link](https://en.bitcoin.it/wiki/URI_Scheme) in the mobile browser should open the send money screen (with fields filled in) using Android intents

Credits
-------

Bitcoin Android is based on the [bitcoinj](http://code.google.com/p/bitcoinj/) library by Mike Hearn at Google.  He deserves some very special thanks!

It also makes use of the [zxing](http://code.google.com/p/zxing/) library for processing QR codes.  The icon was created by [visualpharm.com](http://www.visualpharm.com/).

License
-------

Bitcoin Android is open source software released under the Apache V2 license.
