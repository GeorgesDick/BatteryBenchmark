package com.batterybench;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;

public class BatteryBenchResults extends Activity {
	private static boolean isBackPressed = false;
	
	public void onPause() {
		super.onPause();
	}
	
	public void onBackPressed () {
		super.onBackPressed();
		Log.d("BatteryBench", "=+=+=+=+=+=+= APPUI SUR BACK =+=+=+=+=+=+=");
		isBackPressed = true;
	}

	public void onStop() {
		super.onStop();
		Log.d("BatteryBench", "BatteryBenchResults passe par onStop");
		if (!isBackPressed) {
			Log.d("BatteryBench", "BatteryBenchResults passe par onStop SANS APPUI SUR BACK");
			resetOriginalConfig();
			finish();
			}
		}

	public void onStart() {
		super.onStart();
		isBackPressed = false;
		Log.d("BatteryBench", "BatteryBenchResults passe par onStart");
	}

	public void onResume() {
		super.onResume();
		isBackPressed = false;
		Log.d("BatteryBench", "BatteryBenchResults passe par onResume");
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d("BatteryBench", "------------------BatteryBenchResults passe par onDestroy----------------");
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.results);
		isBackPressed = false;
		
		Log.d("BatteryBench", "------------------BatteryBenchResults onCreate----------------");
		
		WebView engine = (WebView) findViewById (R.id.webresu);
		engine.loadUrl("http://georgesdick.com/batterybench/index.php?mobile=yes&manufacturer=" + Build.MANUFACTURER + "&model=" + Build.MODEL + "&osversion=" + Build.VERSION.RELEASE);
	}
	
	public void resetOriginalConfig () {
		final Context context = getApplicationContext();

		final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (MainActivity.isWiFiInstalled())
			wm.setWifiEnabled(MainActivity.getWasWiFiEnabled());
		
		if (MainActivity.isBlueToothInstalled()) {
			if (MainActivity.getWasBlueToothEnabled() == 1) {
	    		BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				if (myBluetoothAdapter != null) {
					if (myBluetoothAdapter.isEnabled()) {
						myBluetoothAdapter.disable();
					}
				}
	    	}
		}

		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, MainActivity.getOriginalScreenTimeout());
		Log.d("BatteryBench", "BatteryBenchResults a remis le lock");
		MainActivity.getMyLock().reenableKeyguard();
	}

}
