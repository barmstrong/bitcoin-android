package com.bitcoinandroid;

import java.util.ArrayList;

import com.bitcoinandroid.CalculateExchange.DownloadListener;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

// This dialog is a client interface of CalculateExchange

public class CalculateExchangeDialog extends Dialog
{
	Context context;
	public CalculateExchangeDialog(Context context)
	{
		super(context);
		this.context = context;
	}

	EditText exchange_input_edittext;
	EditText exchange_output_edittext;
	Button exchange_calculate_button;
	Button exchange_close_button;

	Spinner exchange_currency_spinner;
	ArrayAdapter<String> exchange_currency_spinner_adapter;
	ArrayList<String> exchange_currency_spinner_list = new ArrayList<String>();

	Spinner exchange_averagelength_spinner;
	ArrayAdapter<String> exchange_averagelength_spinner_adapter;
	ArrayList<String> exchange_averagelength_spinner_list = new ArrayList<String>();
	
	CalculateExchange exchange;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.exchange);
		setTitle("Currency Converter");

		exchange_input_edittext = (EditText) findViewById(R.id.exchange_input_edittext);
		exchange_output_edittext = (EditText) findViewById(R.id.exchange_output_edittext);
		exchange_averagelength_spinner = (Spinner) findViewById(R.id.exchange_averagelength_spinner);
		exchange_currency_spinner = (Spinner) findViewById(R.id.exchange_currency_spinner);
		exchange_calculate_button = (Button) findViewById(R.id.exchange_calculate_button);
		exchange_close_button = (Button) findViewById(R.id.exchange_close_button);

		// Populate the currency spinner
		exchange_currency_spinner_list.add("USD");
		exchange_currency_spinner_list.add("AUD");
		exchange_currency_spinner_list.add("RUB");
		exchange_currency_spinner_list.add("GAU");
		exchange_currency_spinner_list.add("BGN");
		exchange_currency_spinner_list.add("CNY");
		exchange_currency_spinner_list.add("SLL");
		exchange_currency_spinner_list.add("INR");
		exchange_currency_spinner_list.add("GBP");
		exchange_currency_spinner_list.add("SAR");
		exchange_currency_spinner_list.add("PLN");
		exchange_currency_spinner_list.add("EUR");
		exchange_currency_spinner_list.add("CLP");
		exchange_currency_spinner_list.add("CAD");
		exchange_currency_spinner_adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, exchange_currency_spinner_list);
		exchange_currency_spinner.setAdapter(exchange_currency_spinner_adapter);

		// Populate the weighted price time spinner
		exchange_averagelength_spinner_list.add("24 Hour Weighted");
		exchange_averagelength_spinner_list.add("7 Day Weighted");
		exchange_averagelength_spinner_list.add("30 Day Weighted");
		exchange_averagelength_spinner_adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
				exchange_averagelength_spinner_list);
		exchange_averagelength_spinner.setAdapter(exchange_averagelength_spinner_adapter);
		
		// Instantiate CalculateExchange
		exchange = new CalculateExchange();		
		// Pull data from the internet, disable calculate until completed (or something like this - maybe a progress dialog)
		exchange_calculate_button.setText("Downloading...");
		exchange_calculate_button.setEnabled(false);

		exchange.downloadExchangeData(new DownloadListener()
		{
			public void downloadSuccess()
			{
				exchange_calculate_button.setText("Calculate");
				exchange_calculate_button.setEnabled(true);				
			}

			public void downloadFailed()
			{
				exchange_calculate_button.setText("Download Failed");				
			}			
		});

		// Button listeners
		exchange_calculate_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				// Determine which average length to calculate 
				if ("24 Hour Weighted".equals((String)exchange_averagelength_spinner.getSelectedItem()))
					exchange.setExchangePriceLength(CalculateExchange.OneDayAverage);
				else if ("7 Day Weighted".equals((String)exchange_averagelength_spinner.getSelectedItem()))
					exchange.setExchangePriceLength(CalculateExchange.SevenDayAverage);
				else if ("30 Day Weighted".equals((String)exchange_averagelength_spinner.getSelectedItem()))
					exchange.setExchangePriceLength(CalculateExchange.ThirtyDayAverage);	
				
				// Determine which currency to calculate and output to edittext
				try
				{
					if ("USD".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.USDtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("AUD".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.AUDtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("RUB".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.RUBtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("GAU".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.GAUtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("BGN".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.BGNtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("SLL".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.SLLtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("INR".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.INRtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("GBP".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.GBPtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("SAR".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.SARtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("PLN".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.PLNtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("EUR".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.EURtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("CLP".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.CLPtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
					else if ("CAD".equals((String) exchange_currency_spinner.getSelectedItem()))
						exchange_output_edittext.setText(Double.toString(exchange.CADtoBTC(Double.parseDouble(exchange_input_edittext.getText().toString()))));
				}
				catch (Exception e)
				{
					Toast.makeText(context, "Please enter a valid decimal number", Toast.LENGTH_SHORT).show();					
				}				
			}
		});
		
		exchange_close_button.setOnClickListener(new View.OnClickListener()
		{			
			public void onClick(View v)
			{				
				CalculateExchangeDialog.this.dismiss();
			}
		});	
	}
}
