package com.bitcoinandroid;

// This class uses http://bitcoincharts.com/t/weighted_prices.json to calculate currency exchanges to btc.

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;


public class CalculateExchange
{
	
	public static interface DownloadListener
	{
		void downloadSuccess();
		void downloadFailed();
	}

	public static final String BitcoinChartsPrices = "http://bitcoincharts.com/t/weighted_prices.json";
	public static final String ThirtyDayAverage = "30d";
	public static final String SevenDayAverage = "7d";
	public static final String OneDayAverage = "24h";

	private JSONObject marketData = null;
	private String priceLength = OneDayAverage;
	
	public void downloadExchangeData(DownloadListener listener)
	{
		new GetExchangeData(listener).execute();
	}

	public void setExchangePriceLength(String priceLength)
	{
		this.priceLength = priceLength;
	}

	public Double USDtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("USD").getString(priceLength)) * d;
	}

	public Double AUDtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("AUD").getString(priceLength)) * d;
	}

	public Double RUBtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("RUB").getString(priceLength)) * d;
	}

	public Double GAUtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("GAU").getString(priceLength)) * d;
	}

	public Double BGNtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("BGN").getString(priceLength)) * d;
	}

	public Double CNYtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("CNY").getString(priceLength)) * d;
	}

	public Double SLLtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("SLL").getString(priceLength)) * d;
	}

	public Double INRtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("INR").getString(priceLength)) * d;
	}

	public Double GBPtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("GBP").getString(priceLength)) * d;
	}

	public Double SARtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("SAR").getString(priceLength)) * d;
	}

	public Double PLNtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("PLN").getString(priceLength)) * d;
	}

	public Double EURtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("EUR").getString(priceLength)) * d;
	}

	public Double CLPtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("CLP").getString(priceLength)) * d;
	}

	public Double CADtoBTC(Double d) throws Exception
	{
		return Double.parseDouble(marketData.getJSONObject("CAD").getString(priceLength)) * d;
	}

	private class GetExchangeData extends AsyncTask<Void, Void, Void>
	{
		DownloadListener listener;		
		GetExchangeData(DownloadListener listener)
		{
			this.listener = listener;
		}
		

		@Override
		protected void onPreExecute()
		{

		}

		@Override
		protected Void doInBackground(Void... uri)
		{
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			String responseString = null;
			try
			{
				// create a connection with the server
				response = httpclient.execute(new HttpGet(BitcoinChartsPrices));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK)
				{
					// Download the data to a string
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();

					// Convert to JSONObject
					marketData = new JSONObject(responseString);
				}

				else
				{
					// Closes the connection.
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			}

			catch (Exception e)
			{
				Log.d("Exchange", "exception in CalculateExchange getExchangeData");
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (marketData == null)
				listener.downloadFailed();				
			else
				listener.downloadSuccess();
				
			
				
				
			
		}
	}

}
