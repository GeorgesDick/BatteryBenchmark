package com.batterybench;

import java.io.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.*;
import org.apache.http.util.ByteArrayBuffer;
import com.monacodevdroid.myotherapps.ListMyApps;
import android.app.*;
import android.app.KeyguardManager.KeyguardLock;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.*;
import android.net.wifi.WifiManager;
import android.os.*;
import android.provider.Settings;
import android.telephony.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
	
	private boolean stopThread = true;
	private static int nbAbout = 0;
	private static int testDuration = 30;
	private static int startBatLevel = -1;
	private int myBrightness = 70;
	private int testDurationToSend = 0;
	private static int myScreenTimeout = 120000;
	private static int originalScreenTimeout = 0;
	private static int wasBlueToothEnabled = 0;	// 0 = on ne sait pas encore si il est en route ou pas
	private static boolean wasWiFiEnabled = false;
	private static boolean currentWiFiEnabled = true;
	private static boolean isBlueToothEnabled = false;
	private static boolean isBlueToothInstalled = true;
	private static boolean isBlueToothAvailableInSleepMode = true;
	private static boolean isGPSInstalled = true;
	private static boolean is3GInstalled = true;
	private static boolean isWiFiInstalled = true;
	private static boolean isBenchLaunched = false;
	private static int dialogButtonPressed = 0;
	private static int idTestDuration = 1;
	private static final int DataSavedSendingOk = 14;
	private static final int DataSavedSendingNok = 15;
	private static int currentSignalStrength = 0;
	private static int minSignalStrength = 999;
	private static int maxSignalStrength = 0;
	private String versionName = "?";
	private String monUrl = "";
	private TelephonyManager telManager;
    private PhoneStateListener signalListener;
    private KeyguardManager keyguardManager;
    private static KeyguardLock lock;
	
	public void onDestroy() {
		super.onDestroy();
		Log.d("BatteryBench", "----------------MainActivity passe par onDestroy------------------");
		resetOriginalConfig();
		BatteryBenchRunTest.elapsedThread(true);
	}
	
	public void onPause() {
		super.onPause();
		Log.d("BatteryBench", "BatteryBench MainActivity passe par onPause");
/*
		if (!isBenchLaunched) {
			Log.d("BatteryBench", "...onPause qui stoppe tout");
			resetOriginalConfig();
			BatteryBenchRunTest.elapsedThread(true);
			finish();
		}
*/
		isBenchLaunched = false;
	}

/*
	public void onStop() {
		super.onStop();
		Log.d("BatteryBench", "BatteryBenchResults MainActivity passe par onStop");
		resetOriginalConfig();
		BatteryBenchRunTest.elapsedThread(true);
		finish();
		}
*/
	
	public void onBackPressed () {
		isBenchLaunched = false;
		super.onBackPressed();
		Log.d("BatteryBench", "=+=+=+=+=+=+= APPUI SUR BACK dans MainActivity =+=+=+=+=+=+=");
	}
	
	public void onResume() {
		super.onResume();
		isBenchLaunched = false;
		resetMyConfig();
		Log.d("BatteryBench", "MainActivity passe par onResume");
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_principal, menu);
	    return true;
	}
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BatteryBench", "Start");
        setContentView(R.layout.main);
    	final Context context = getApplicationContext();
    	
    	isBenchLaunched = false;

    	new ListMyApps(this, "com.batterybench").show();
    	
    	final RadioButton bouttonTrenteSecondes = (RadioButton) findViewById(R.id.trente_secondes);
    	final RadioButton bouttonDixMinutes = (RadioButton) findViewById(R.id.dix_minutes);
    	
    	if (nbAbout < 3) {
    		bouttonTrenteSecondes.setVisibility(View.GONE);
    		bouttonDixMinutes.setVisibility(View.GONE);
    	}
    	else {
    		bouttonTrenteSecondes.setVisibility(View.VISIBLE);
    		bouttonDixMinutes.setVisibility(View.VISIBLE);
    	}

    	final File monFichier = new File("/data/data/" + getPackageName(),"benchdata");
    	Log.d("BatteryBench", "AV test fichier data (" + "/data/data/" + getPackageName() + "/benchdata" + ")existe");
    	if (monFichier.exists()) {
	    	try {
	    		BufferedReader fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(monFichier)));
				monUrl = fileInput.readLine();
				fileInput.close();
				Log.d("BatteryBench", "Données à envoyer => monUrl : " + monUrl);
				String lesVariables[] = monUrl.split("&");
				for (int i = 0; i < lesVariables.length; i++)
					if (lesVariables[i].startsWith("elapsedtime")) {
						Log.d("BatteryBench", "lesVariables[" + i + "] = " + lesVariables[i]);
						testDurationToSend = Integer.parseInt(lesVariables[i].substring(12));
						testDurationToSend /= 1000;
						Log.d("BatteryBench", "testDurationToSend = " + testDurationToSend);
						}
				final ProgressDialog dialog = ProgressDialog.show(this, (String)getText(R.string.app_name), (String)getText(R.string.send_results_attempt), true);
				final Handler handler = new Handler() {
		            public void handleMessage(Message msgRecu) {
		                dialog.dismiss();
		                switch (msgRecu.what) {
		                	case DataSavedSendingOk :
		        				monFichier.delete(); // app_name
		                		boitealerte((String)getText(R.string.app_name), (String)getText(R.string.sent_results) + " (" + testDurationToSend + " sec.)");
		                		break;
		                	default :
		                		boitealerte((String)getText(R.string.app_name), (String)getText(R.string.saved_results));
		                		break;
		                }
		            }
		        };
		        class asyncUseNetwork extends AsyncTask<String, Void, Void> {
				    protected Void doInBackground(String... urls) {
				    	Message msg = new Message();
		        		if (envoyerget(context, monUrl))
		        			msg.what = DataSavedSendingOk;
		        		else
		        			msg.what = DataSavedSendingNok;
						handler.sendMessage(msg);
				        return null;
				    }
				}
				new asyncUseNetwork().execute();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
    	}

    	Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    	int rawlevel = batteryIntent.getIntExtra("level", -1);
//    	double scale = batteryIntent.getIntExtra("scale", -1);
//    	int status = batteryIntent.getIntExtra("status", -1);
    	startBatLevel = rawlevel;

    	final TextView affBatt = (TextView) findViewById(R.id.AutoText);
    	final TextView affType = (TextView) findViewById(R.id.TypeText);
    	final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
    	
    	final PackageManager manager = this.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
	        versionName = info.versionName;
		} catch (NameNotFoundException e) {
			versionName = "unknown";
		}

    	affBatt.setText ((String)getText(R.string.battery_level) + " " + rawlevel + "%");

    	originalScreenTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, myScreenTimeout);
    	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, myScreenTimeout);
    	Log.d("BatteryBench", "Screen timeout passé à " + myScreenTimeout);

    	affType.setText ("\n" + (String)getText(R.string.manufacturer) + Build.MANUFACTURER + "\n" + (String)getText(R.string.model) + Build.MODEL + "\n" + (String)getText(R.string.board) + Build.BOARD + "\n" + (String)getText(R.string.brand) + Build.BRAND + "\n" + (String)getText(R.string.android_version) + Build.VERSION.RELEASE + "\n");
        setBrightness (myBrightness);
        
        testHardwareComponents();
        
        if (MainActivity.is3GInstalled()) {
        	Log.d("BatteryBench", "Il y a la 3G sur cette machine...");
        	signalListener=new PhoneStateListener() {
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                	currentSignalStrength = signalStrength.getGsmSignalStrength();
                	setMaxSignalStrength(currentSignalStrength);
                	setMinSignalStrength(currentSignalStrength);
//                	Log.d("BatteryBench","-----------------------------------------");
//                    Log.d("BatteryBench","Force du signal est passé à : " + currentSignalStrength);
                }
             };
             telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
             telManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
             Log.d("BatteryBench","-----------------------------------------");
             Log.d("BatteryBench","telManager : "+telManager);
        }
        else {
        	Log.d("BatteryBench", "Pas de 3G sur cette machine...");
        }
        
        final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (MainActivity.isWiFiInstalled()) {
	        wasWiFiEnabled = wm.isWifiEnabled();
	        Log.d("BatteryBench", "WiFi Enabled : " + ((wasWiFiEnabled == true) ? "Yes" : "No"));
	        currentWiFiEnabled = true;
	        wm.setWifiEnabled(currentWiFiEnabled);
	        Log.d("BatteryBench", "Activation du WiFi...");
        }
        else {
        	wasWiFiEnabled = false;
        	currentWiFiEnabled = false;
        	Log.d("BatteryBench", "Pas de WiFi sur cette machine...");
        }
        
        if (MainActivity.isGPSInstalled()) {
	        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	    		Log.d("BatteryBench", "GPS disabled !");
	    		boitealerte ("GPS inactif", (String)getText(R.string.gps_must));
//	    		resetOriginalConfig();
	    	}
        }
        else
        	Log.d("BatteryBench", "Pas de GPS sur cette machine...");
        
        Log.d("BatteryBench", "Avant le test si le Bluetooth existe ici");
        if (MainActivity.isBlueToothInstalled()) {
			BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			Log.d("BatteryBench", "Le Bluetooth existe ici....");
			if (myBluetoothAdapter != null) {
				if (!myBluetoothAdapter.isEnabled()) {
					Log.d("BatteryBench", "On met wasBlueToothEnabled a 1");
					wasBlueToothEnabled = 1;	// 1 = il n'était pas en route
					myBluetoothAdapter.enable();
		            setBlueToothEnabled(true);
		        }
				else {
					Log.d("BatteryBench", "On met wasBlueToothEnabled a 2");
					wasBlueToothEnabled = 2;	// 2 = il était en route
				}
	        }
        }
        else {
        	Log.d("BatteryBench", "Pas de BlueTooth sur cette machine...");
        	setBlueToothEnabled(false);
        }
		
		keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
		lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);

        final Button button = (Button) findViewById(R.id.Bouton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Log.d("BatteryBench", "OH ! you touched my tralala !");
            	if (MainActivity.isGPSInstalled()) {
	            	if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	            		Log.d("BatteryBench", "GPS disabled !");
	            		boitealerte ("GPS inactif", "Le GPS doit être activé pour les tests");
//	            		resetOriginalConfig();
	            		return;
	            	}
            	}
            	if (MainActivity.isBlueToothInstalled()) {
	            	BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        		if ((myBluetoothAdapter != null) && (myBluetoothAdapter.isEnabled()))
	        	        setBlueToothEnabled(true);
	        		else
	        			setBlueToothEnabled(false);
	            	if (!isBlueToothEnabled()) {
	            		Log.d("BatteryBench", "BlueTooth disabled !");
	            		boitealerte ("BlueTooth inactif", "Le BlueTooth doit être activé pour les tests");
//	            		resetOriginalConfig();
	            		return;
	            	}
            	}
            	final RadioGroup monPremierGroupe = (RadioGroup) findViewById(R.id.dureeRadioGroup);
            	int quoiFaire = monPremierGroupe.getCheckedRadioButtonId();
            	
            	Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int rawlevel = batteryIntent.getIntExtra("level", -1);
                if (rawlevel <= 40) {
            		boitealerte ("<= 40%", (String)getText(R.string.load_min_40_pcent));
//            		resetOriginalConfig();
            		return;
            	}
                if (quoiFaire == R.id.epuisement) {
                	if (rawlevel < 90) {
                		boitealerte ("< 90%", (String)getText(R.string.long_test_need_90_pcent));
//	            		resetOriginalConfig();
	            		return;
                	}
            	}
                if (quoiFaire == R.id.afond) {
                	if (rawlevel < 99) {
                		boitealerte ("Not 100%", (String)getText(R.string.full_test_need_100_pcent));
//	            		resetOriginalConfig();
	            		return;
                	}
            	}
            	if (MainActivity.isWiFiInstalled()) {
	            	if (!currentWiFiEnabled) {
	            		currentWiFiEnabled = true;
	            		wm.setWifiEnabled(currentWiFiEnabled);
	            	}
            	}
            	originalScreenTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, myScreenTimeout);
            	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, myScreenTimeout);
            	Log.d("BatteryBench", "Lancement : confirmation du screen timeout passé à " + myScreenTimeout);
//            	stopThread = true;
            	if (stopThread) {
//            		final RadioGroup monGroupe = (RadioGroup) findViewById(R.id.dureeRadioGroup);
                    
                    if (quoiFaire == R.id.cinq_pourcent) {
                    	idTestDuration = R.string.cinq_pourcent;
                    	Log.d("BatteryBench", "5pcent : " + idTestDuration);
                    	testDuration = -5;
                    }
                    else if (quoiFaire == R.id.trente_secondes) {
                    	idTestDuration = R.string.trente_secondes;
                    	testDuration = 30;
                    }
                    else if (quoiFaire == R.id.dix_minutes) {
                    	idTestDuration = R.string.dix_minutes;
                    	testDuration = 600;
                    }
/*
                    else if (quoiFaire == R.id.une_heure) {
                    	idTestDuration = R.string.une_heure;
                    	testDuration = 3600;
                    }
*/
                    else if (quoiFaire == R.id.epuisement) {
                    	idTestDuration = R.string.epuisement;
                    	testDuration = -1;
                    }
                    else if (quoiFaire == R.id.afond) {
                    	idTestDuration = R.string.afond;
                    	testDuration = -2;
                    }
                    else {
                    	boitealerte ("Parametre invalide", "Erreur : parametre non reconnu");
                    	return;
                    }
            		button.setText(R.string.do_stop_test);
                	stopThread = false;
                	BatteryBenchRunTest.modifyThreadBoolStatus(stopThread);
                	Log.d("BatteryBench", "DIM 50 percent");
                	lock.disableKeyguard();
                	Log.d("BatteryBench", "?????????????disableKeyguard?????????????");
                    Intent myIntent = new Intent(MainActivity.this, BatteryBenchRunTest.class);
//                    Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//                	int rawlevel = batteryIntent.getIntExtra("level", -1);
//                	double scale = batteryIntent.getIntExtra("scale", -1);
//                	int status = batteryIntent.getIntExtra("status", -1);
                	startBatLevel = rawlevel;
                	isBenchLaunched = true;
                    MainActivity.this.startActivity(myIntent);
                    Log.d("BatteryBench", "Retour de DIM 50 percent");
            	}
            	else {
//            		button.setText(R.string.do_restart_text);
            		button.setText(R.string.launch_text);
                	stopThread = true;
                	BatteryBenchRunTest.modifyThreadBoolStatus(stopThread);
            	}
            }
        });

        final ImageButton boutonLesNumeriques = (ImageButton) findViewById(R.id.imgLesNumeriques);
        boutonLesNumeriques.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Log.d("BatteryBench", "Go to Les Numeriques");
            	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lesnumeriques.com"));
            	startActivity(browserIntent);
            }
        });

        Log.d("BatteryBench", "End");
    }

    public static int getMyScreenTimeout() {
		return myScreenTimeout;
	}

	// Set screen brightness
    private void setBrightness (int percentage) {
    	float myBrightness;

    	Window mywindow = getWindow();
    	
    	if (percentage < 0)
    		myBrightness = -1.0f;
    	else {
    		if (percentage > 90) percentage = 90;
    		if (percentage < 10) percentage = 10;
    		myBrightness = (float)percentage;
        	myBrightness /= 100f;
    	}
    	WindowManager.LayoutParams lp = mywindow.getAttributes();
    	Log.d("BatteryBench", "myBrightness vaut " + myBrightness);
        lp.screenBrightness = myBrightness;
        mywindow.setAttributes(lp);
        return;
    }

    public static int getTestDuration () {
    	return testDuration;
    }
    
    public static int getStartBatLevel () {
    	return startBatLevel;
    }
    
    public static int getOriginalScreenTimeout () {
    	return originalScreenTimeout;
    }
    
    public static boolean getWasWiFiEnabled () {
    	return wasWiFiEnabled;
    }
    
    public static boolean getCurrentWiFiEnabled () {
    	return currentWiFiEnabled;
    }
    
    public static void setCurrentWiFiEnabled (boolean status) {
    	currentWiFiEnabled = status;
    }
    
    public static int getIdTestDuration () {
    	return idTestDuration;
    }

    public void resetOriginalConfig () {
    	final Context context = getApplicationContext();
    	final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

    	if (MainActivity.isWiFiInstalled()) {
    		wm.setWifiEnabled(wasWiFiEnabled);
    		currentWiFiEnabled = wasWiFiEnabled;
    	}
    	Log.d("BatteryBench", "Dans resetOriginalConfig avant test du Bluetooth et wasBlueToothEnabled vaut " + wasBlueToothEnabled);
    	if (wasBlueToothEnabled == 1) {
    		Log.d("BatteryBench", "Dans resetOriginalConfig le Bluetooth n'était pas actif");
    		BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (myBluetoothAdapter != null) {
				if (myBluetoothAdapter.isEnabled()) {
					Log.d("BatteryBench", "Dans resetOriginalConfig on coupe le Bluetooth");
					myBluetoothAdapter.disable();
				}
			}
    	}
    	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, originalScreenTimeout);
		lock.reenableKeyguard();
		Log.d("BatteryBench", "MainActivity a remis le lock");
    }
    
    public void resetMyConfig () {
    	final Context context = getApplicationContext();
    	final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

    	if (MainActivity.isWiFiInstalled()) {
    		if (!currentWiFiEnabled) {
        		currentWiFiEnabled = true;
        		wm.setWifiEnabled(currentWiFiEnabled);
        	}
    	}
//    	if (wasBlueToothEnabled == 1) {
    	if (MainActivity.isBlueToothInstalled()) {
    		BluetoothAdapter myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		if (myBluetoothAdapter != null) {
				if (!myBluetoothAdapter.isEnabled()) {
					myBluetoothAdapter.enable();
		            setBlueToothEnabled(true);
		        }
	        }
    	}
    	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, originalScreenTimeout);
    	lock.disableKeyguard();
		Log.d("BatteryBench", "MainActivity a viré le lock");
    }

	public static void setBlueToothEnabled(boolean isBlueToothEnabled) {
		MainActivity.isBlueToothEnabled = isBlueToothEnabled;
	}


	public static boolean isBlueToothEnabled() {
		return isBlueToothEnabled;
	}


	public static void setBlueToothInstalled(boolean isBlueToothInstalled) {
		MainActivity.isBlueToothInstalled = isBlueToothInstalled;
	}


	public static boolean isBlueToothInstalled() {
		return isBlueToothInstalled;
	}


	public static boolean isBlueToothAvailableInSleepMode() {
		return isBlueToothAvailableInSleepMode;
	}

	public static void setBlueToothAvailableInSleepMode(
			boolean isBlueToothAvailableInSleepMode) {
		MainActivity.isBlueToothAvailableInSleepMode = isBlueToothAvailableInSleepMode;
	}

	public static void setGPSInstalled(boolean isGPSInstalled) {
		MainActivity.isGPSInstalled = isGPSInstalled;
	}


	public static boolean isGPSInstalled() {
		return isGPSInstalled;
	}


	public static void set3GInstalled(boolean is3GInstalled) {
		MainActivity.is3GInstalled = is3GInstalled;
	}


	public static boolean is3GInstalled() {
		return MainActivity.is3GInstalled;
	}
	
	public static void setWiFiInstalled(boolean isWiFiInstalled) {
		MainActivity.isWiFiInstalled = isWiFiInstalled;
	}


	public static boolean isWiFiInstalled() {
		return isWiFiInstalled;
		}
	
	private void testHardwareComponents() {
		// Les tablettes Archos n'ont ni GPS ni 3G
		if (Build.MANUFACTURER.equals("archos")) {
			Log.d("BatteryBench", "Manufacturer Archos : pas de GPS ni de 3G");
			MainActivity.setGPSInstalled(false);
			MainActivity.set3GInstalled(false);
			// ... et le bluetooth se coupe au passage en sleep mode !
			MainActivity.setBlueToothAvailableInSleepMode(false);
			}
		// L'émulateur n'a ni GPS, ni WiFi, ni BlueTooth
		else if (Build.MANUFACTURER.equals("unknown") && Build.MODEL.equals("google_sdk")) {
			Log.d("BatteryBench", "Model google_sdk = pas de GPS ni WiFi ni BlueTooth");
			MainActivity.setGPSInstalled(false);
			MainActivity.setWiFiInstalled(false);
			MainActivity.setBlueToothInstalled(false);
			}
		// Présence du GPS sur les autres appareils
		else {
			PackageManager pm = this.getPackageManager();
			Context context = getApplicationContext();
			String hasFeatureOrNot;
			
		    if (!(pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))) {
		    	MainActivity.setGPSInstalled(false);
		    	hasFeatureOrNot = (String)context.getText(R.string.GPS) + " " + (String)context.getText(R.string.not_installed);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    else {
		    	hasFeatureOrNot = (String)context.getText(R.string.GPS) + " " + (String)context.getText(R.string.detected);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    if (!(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))) {
		    	MainActivity.setBlueToothInstalled(false);
		    	hasFeatureOrNot = (String)context.getText(R.string.BlueTooth) + " " + (String)context.getText(R.string.not_installed);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    else {
		    	hasFeatureOrNot = (String)context.getText(R.string.BlueTooth) + " " + (String)context.getText(R.string.detected);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    if (!(pm.hasSystemFeature(PackageManager.FEATURE_WIFI))) {
		    	MainActivity.setWiFiInstalled(false);
		    	hasFeatureOrNot = (String)context.getText(R.string.WiFi) + " " + (String)context.getText(R.string.not_installed);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    else {
		    	hasFeatureOrNot = (String)context.getText(R.string.WiFi) + " " + (String)context.getText(R.string.detected);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    if (!(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))) {
		    	MainActivity.set3GInstalled(false);
		    	hasFeatureOrNot = (String)context.getText(R.string.phone_module) + " " + (String)context.getText(R.string.not_installed);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
		    else {
		    	hasFeatureOrNot = (String)context.getText(R.string.phone_module) + " " + (String)context.getText(R.string.detected);
		    	Toast.makeText(this, hasFeatureOrNot, Toast.LENGTH_SHORT).show();
		    	}
			}
		}
	
	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.resu: // Affichage de la page de resultats
//	    	boitealerte("Demande","Resultats");
	    	Intent myResuIntent = new Intent(MainActivity.this, BatteryBenchResults.class);
	    	isBenchLaunched = true;
	    	MainActivity.this.startActivity(myResuIntent);
	    	return true;
	    case R.id.about: // Demande du "A propos"
	    	nbAbout++;
	    	if (nbAbout >= 3) {
	    		final RadioButton bouttonTrenteSecondes = (RadioButton) findViewById(R.id.trente_secondes);
	        	final RadioButton bouttonDixMinutes = (RadioButton) findViewById(R.id.dix_minutes);
	    		bouttonTrenteSecondes.setVisibility(View.VISIBLE);
	    		bouttonDixMinutes.setVisibility(View.VISIBLE);
	    	}
	        String texteApropos = this.getString(R.string.about_text) + "\nv" + versionName;
	    	boitealerte(this.getString(R.string.about),texteApropos);
	        return true;
	    case R.id.quit: // Quitter BatteryBench
	    	Log.d("BatteryBench", "Appui sur QUIT");
	    	resetOriginalConfig();
	        finish();
	        return true;
	    }
	    return false;
	}
	
	public void boitealerte(String titre, String monmessage)
	{
     // Display an alert
     new AlertDialog.Builder(this)
        .setMessage(monmessage)
        .setTitle(titre)
        .setCancelable(true)
        .setNeutralButton(android.R.string.cancel,
        	new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton){}
        	})
        .show();
	}
	
	public void boitedialogue(String titre, String monmessage)
	{
	 dialogButtonPressed = 0;
     // Display an alert
     new AlertDialog.Builder(this)
        .setMessage(monmessage)
        .setTitle(titre)
        .setCancelable(true)
        .setPositiveButton(android.R.string.ok,
        	new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton){
        		dialogButtonPressed = 1;
        		Log.d("BatteryBench", "Bouton OK");
        		}
        	})
        .setNeutralButton(android.R.string.cancel,
        	new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int whichButton){
        		dialogButtonPressed = 2;
        		Log.d("BatteryBench", "Bouton Cancel");
        		}
        	})
        .show();
	}
	
	protected boolean envoyerget(Context context, String url) {
		if (isNetworkAvailable(context) == false)
			return false;
//		HttpGet httpget = new HttpGet(url);
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		int timeoutConnection = 1000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 1500;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		try {
			HttpResponse response = httpclient.execute(httpget);
			InputStream content = response.getEntity().getContent();
			BufferedInputStream bis = new BufferedInputStream(content);  
			ByteArrayBuffer baf = new ByteArrayBuffer(50);  

			int current = 0;  
			while((current = bis.read()) != -1){  
				baf.append((byte)current);
			}
			/* Convert the Bytes read to a String. */  
			String resuHttp = new String(baf.toByteArray());
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
		.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}

			}
		}
		return false;
	}
	
	public static int getCurrentSignalStrength() {
		return currentSignalStrength;
	}

	public static int getMaxSignalStrength() {
		return maxSignalStrength;
	}

	public static void setMaxSignalStrength(int maxSignalStrength) {
		if (maxSignalStrength > MainActivity.maxSignalStrength)
			MainActivity.maxSignalStrength = maxSignalStrength;
	}

	public static int getMinSignalStrength() {
		return minSignalStrength;
	}

	public static void setMinSignalStrength(int minSignalStrength) {
		if (minSignalStrength < MainActivity.minSignalStrength)
			MainActivity.minSignalStrength = minSignalStrength;
	}
	
	public static KeyguardLock getMyLock() {
		return lock;
	}
	
	public static int getWasBlueToothEnabled() {
		return wasBlueToothEnabled;
	}
	
	
}