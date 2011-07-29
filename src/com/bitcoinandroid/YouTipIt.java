package com.bitcoinandroid;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class YouTipIt {

	public static String getLink(String origLink) {
		String TipIt_JSON=web_get("http://www.youtipit.org/api/GetTipitByUrl?url="+origLink);
		try {
		    if (TipIt_JSON.length()>0) {
		        JSONObject jo=new JSONObject(TipIt_JSON);
		        return "bitcoin:"+jo.getString("BitcoinAdress");
		    } else throw new RuntimeException("Trigger reset");
		} catch (Exception e) {
		    throw new RuntimeException("Trigger reset");
        }

	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private static String web_get(String url){
		String output="";
		HttpClient  client=new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
		try {
			HttpResponse resp = client.execute(get);
			HttpEntity ent = resp.getEntity();
			InputStream is = ent.getContent();

			output=convertStreamToString(is);
		} catch (Exception e) {
			output="";
		}
		return output;
	}
}
