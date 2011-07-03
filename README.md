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

For best results, install the app while connected to WiFi (it will down some data the first time you connect).

Usage
-----

Your wallet has two basic functions: Sending Money and Requesting Money.

This is primarily done by scanning and generating [QR codes](http://en.wikipedia.org/wiki/QR_code), respectively, although there are a few other options discussed below.

When you open the app for the first time you'll see your balance, a list of recent transactions, and two buttons relating to the two primary functions described above.  (A little spinner in the upper right means we're communicating with the bitcoin network to udpate your wallet - you can continue to use the app normally while this is running.)

To start off, you may want to get a few coins in your brand new wallet.  To do this, click the 'Request Money' button on the home screen.  You'll notice that a QR code has been generated and there is an optional field for an amount.  If you have a friend nearby who also has the app, they can scan your QR code to get your bitcoin receive address and send you money.  This is the fastest way to send money and will save them some typing since the address is 34 characters long!

However, in the more likely case that you don't have a friend nearby, there are a few other options.  Clicking "Send A Request..." at the bottom will bring up a menu with various options to sent a bitcoin "invoice" of sorts.  For now try emailing one to yourself.  When it shows up in your inbox you can take a look at it, and copy/paste the address into your desktop client or the bitcoin faucet to send a few pennies to your phone.  After a few seconds you should see a notification on your phone alerting you that you've received funds!  It will initially be pending (grayed out in the UI), and later verified just like all bitcoin transactions.

Once you have money in your wallet, you can send money.  Clicking "Send Money" will pull up a camera view that is designed to scan QR codes.  The scanner will recognize any QR codes that use the [bitcoin URI scheme](https://en.bitcoin.it/wiki/URI_Scheme), which could be on a wall sticker, another mobile device, or a point of sale device in a store (although there aren't [too](http://starburst.hackerfriendly.com/?p=1530) many of these yet).  If your intended recipient doesn't have a QR code you can also click the "Enter Manually" button and type an address in by hand or paste it from the clipboard.

That's it!  One final note.  Sometimes people get confused about depositing and withdrawing funds from their wallet.  In this case depositing is simply using the "Request Money" feature to send money to yourself (from your desktop client or elsewhere).  And withdrawing is simply "Sending Money" to yourself (again, your desktop client or elsewhere).  The terminology around this is still a bit confusing and will hopefully improve over time.

Technical Details
-----------------

Bitcoin Wallet is based on the excellent [bitcoinj](http://code.google.com/p/bitcoinj/) library created by Mike Hearn at Google.

It's a full bitcoin client running on your phone.  This means it keeps it's own copy of the blockchain (currently about 12MB) which we store on your phone's SD card.  Most phones have large SD cards (8GB or more is not uncommon) so storing 12MB isn't too bad.  The wallet file is stored on the phone's internal memory and is quite small compared to the blockchain.  We include a recent copy of the blockchain in the actual app package so you won't need to download and build the whole thing from scratch.  The first time you run it, it will contact nearby peers and update the included blockchain to the most recent version.  This usually takes a minute or less.  After that, updates to the blockchain and transaction notifications should be near instantaneous (a few seconds).

Because Bitcoin Wallet is a full bitcoin client, this also means that your "money" is stored on the device itself.  If you lose your phone you could potentially lose your money (just like if you lost your real wallet you'd lose the cash inside).  So you should probably never carry more money on your phone than you'd feel comfortable carrying in your real wallet.  In practice, the coins may be recoverable because we sync your wallet file to your Google Account in the cloud after each transaction.  If you did lose your phone you could login to your Google Account on another Android device, install the app, and once again have access to your coins.  (At that point you'd probably want to send them somewhere safe and get them off the phone since the thief could be doing the same thing.)

Contributing
------------

TODO
----

* require a pin code required to send money?
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
