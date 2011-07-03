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
* A free app, 100% open sourced

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

Check out the wiki page on [technical details](https://github.com/barmstrong/bitcoin-wallet/wiki/Technical-Details).

Contributing
------------

Feel free to fork and send pull requests.  Contributions welcome.

TODO
----

* optionally require a pin code to send money?
* ability to generate more than one receive address
* make backup to the cloud optional for those who want the privacy
* move to a "thin client" implementation instead of storing the entire blockchain (this will become necessary as the blockchain grows over time)
* make an easy link to the Bitcoin Faucet for people to get a few coins on the device when they first try it
* clicking a [bitcoin link](https://en.bitcoin.it/wiki/URI_Scheme) in the mobile browser should open the send money screen (with fields filled in)

Credits
-------

Bitcoin Wallet is based on the [bitcoinj](http://code.google.com/p/bitcoinj/) library by Mike Hearn at Google.  He deserves some very special thanks!

It also makes use of the [zxing](http://code.google.com/p/zxing/) library for processing QR codes.  The icon was created by [visualpharm.com](http://www.visualpharm.com/).

License
-------

Bitcoin Wallet is open source software released under the Apache V2 license.
