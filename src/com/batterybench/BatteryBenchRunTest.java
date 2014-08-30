package com.batterybench;

import java.io.*;
import java.net.URLEncoder;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.*;
import org.apache.http.util.ByteArrayBuffer;
import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.location.*;
import android.net.*;
import android.net.wifi.WifiManager;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.widget.*;

public class BatteryBenchRunTest extends Activity {
	private static String serverURL = "http://www.georgesdick.com/batterybench/batterybench.php";
	private static boolean stopThread = true;
	private static boolean elapsedThread = false;
	private static boolean isBackPressed = false;
	private boolean gpsEnCours = false;
	private boolean detectCheat = true;
	private boolean cheat = false;
	private boolean battCharging = false;
	private boolean getEnCours = false;
	private boolean isOnPause = false;
	private boolean isGPSonline = false;
	private boolean isCPUcomputing = false;
	private boolean isDataNetActive = false;
	private boolean isWiFiScanning = false;
	private boolean isBlueToothScanning = false;
	private boolean dataAlreadySent = false;
	private String resuHttp = "";
	private String bufResu = "";
	private String monUrl = "";
	private String bufSendHttpResu = "";
	private String bufAtf = "";
	private String bufCheat = "";
	private String versionName = "?";
	private long elapsedTime;
	private long waitForScreenOnAgain = 0l;
	private long waitForScreenOffAgain = 0l;
	private int nbDoubledgetEnCours = 0;
	private int nbTotIterations = 0;
	private int envoyerGetSuccess = 0;
	private int nbTotCarRecupHttp = 0;
	private int NbTotOnPause = 0;
	private int NbTotOnStop = 0;
	private int NbTotOnStart = 0;
	private int NbTotOnResume = 0;
	private int avgSignalStrength = 0;
	private int globalTestDuration = 0;
	private static final int screenOffDuration = 120000;
	private static final int SLEEP_DURATION = 5000;
	private static final int NB_CALC_LOOP = 5000;
	private static final int DATANETWORK_USAGE_RATE = 2;
	private static final int GPS_USAGE_RATE = 3;
	private static final int WIFI_USAGE_RATE = 4;
	private static final int BLUETOOTH_USAGE_RATE = 5;
	private static final int TICK = 0;
	private static final int TERMINE = 1;
	private static final int CHEAT = 2;
	private static final int ENVOIDATA = 3;
	private static final int CPUComputing = 4;
	private static final int CPUSleeping = 5;
	private static final int WiFiScanning = 6;
	private static final int WiFiSleeping = 7;
	private static final int DataNetActive = 8;
	private static final int DataNetWaiting = 9;
	private static final int GPSOffline = 10;
	private static final int GPSOnline = 11;
	private static final int BlueToothScanning = 12;
	private static final int BlueToothSleeping = 13;
//	private static final int DataSavedSendingOk = 14;
//	private static final int DataSavedSendingNok = 15;
	private Context monContexte;
	private TextView textEtatCPU = null;
	private ImageView imageEtatCPU = null;
	private TextView textEtatWiFi = null;
	private ImageView imageEtatWiFi = null;
	private TextView textEtatDataNet = null;
	private ImageView imageEtatDataNet = null;
	private TextView textEtatGPS = null;
	private ImageView imageEtatGPS = null;
	private TextView textEtatBlueTooth = null;
	private ImageView imageEtatBlueTooth = null;
	private TextView textElapsedTime = null;
	private TextView textSleepWake = null;
	private BluetoothAdapter myBluetoothAdapter;
	private SoundManager mSoundManager;
	private int ploufSound = 1;
	private int sonarSound = 2;
	private int hornSound = 3;
	private int alarmSound = 4;
	
	public void onPause() {
		super.onPause();
		Log.d("BatteryBench", "BatteryBenchRunTest passe par onPause");
		NbTotOnPause++;
		isOnPause = true;
		if (!elapsedThread)
			mSoundManager.playSound(hornSound);
	}
	
	public void onBackPressed () {
		elapsedThread = true;
		isBackPressed = true;
		super.onBackPressed();
		Log.d("BatteryBench", "=+=+=+=+=+=+= APPUI SUR BACK =+=+=+=+=+=+=");
	}

	public void onStop() {
		super.onStop();
		Log.d("BatteryBench", "BatteryBenchRunTest passe par onStop");
		NbTotOnStop++;
		if (!isBackPressed) {
			Log.d("BatteryBench", "BatteryBenchRunTest passe par onStop SANS APPUI SUR BACK");
			elapsedThread = true;
			if ((globalTestDuration == -2) && (getBatteryRawLevel() <= 30) && (getBatteryRawLevel() > 15)){
				Log.d("BatteryBench", "Doit-on virer /data/data/" + getPackageName() + "/benchdata ?");
				final File monFichier = new File("/data/data/" + getPackageName(),"benchdata");
				if (monFichier.exists()) {
					monFichier.delete();
					Log.d("BatteryBench", "OUI ! On doit virer /data/data/" + getPackageName() + "/benchdata !");
				}
			}
			resetOriginalConfig();
			finish();
		}
	}

	public void onStart() {
		super.onStart();
		Log.d("BatteryBench", "BatteryBenchRunTest passe par onStart");
		NbTotOnStart++;
		isBackPressed = false;
	}

	public void onResume() {
		super.onResume();
		Log.d("BatteryBench", "BatteryBenchRunTest passe par onResume");
		NbTotOnResume++;
		isOnPause = false;
		if (isGPSonline)
			changeGpsStatus(R.drawable.working, R.string.OnLine);
		else
			changeGpsStatus(R.drawable.sleeping, R.string.Sleeping);
		if (isCPUcomputing)
			changeCpuStatus(R.drawable.working, R.string.Working);
		else
			changeCpuStatus(R.drawable.sleeping, R.string.Sleeping);
		if (isDataNetActive)
			changeDataNetStatus(R.drawable.working, R.string.Working);
		else
			changeDataNetStatus(R.drawable.sleeping, R.string.Waiting);
		if (isWiFiScanning)
			changeWiFiStatus(R.drawable.working, R.string.Scanning);
		else
			changeWiFiStatus(R.drawable.sleeping, R.string.Sleeping);
		if (isBlueToothScanning)
			changeBlueToothStatus(R.drawable.working, R.string.Scanning);
		else
			changeBlueToothStatus(R.drawable.sleeping, R.string.Sleeping);
		isBackPressed = false;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d("BatteryBench", "------------------BatteryBenchRunTest passe par onDestroy----------------");
		elapsedThread = true;
		resetOriginalConfig();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.action);
		isBackPressed = false;
		
		Log.d("BatteryBench", "------------------BatteryBenchRunTest onCreate----------------");
		
		final long monTimeStampDeDepart = (new Date()).getTime();

		monContexte = getApplicationContext();
		final int testDuration = MainActivity.getTestDuration();
		globalTestDuration = testDuration;
		final TextView monTexte = (TextView)findViewById(R.id.ActionAutoText);
		final TextView httpResuText = (TextView)findViewById(R.id.HttpResuText);
		final TextView textSignalStrength = (TextView)findViewById(R.id.TextSignalStrength);
		final TextView textBatteryLevel = (TextView)findViewById(R.id.TextBatteryLevel);
		
		final PackageManager manager = this.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
	        versionName = info.versionName;
		} catch (NameNotFoundException e) {
			versionName = "unknown";
		}
		
		mSoundManager = new SoundManager();
		mSoundManager.initSounds(getBaseContext());
		mSoundManager.addSound(ploufSound, R.raw.plouf);
		mSoundManager.addSound(sonarSound, R.raw.sonar);
		mSoundManager.addSound(hornSound, R.raw.divehorn);
		mSoundManager.addSound(alarmSound, R.raw.alarm);
		for(int i = 0; i < 1000000; i++) {}
		
		if ((testDuration == 30) || (testDuration == 600)) {
			Log.d("BatteryBench", "!!!!!!!!!!!!!ATTENTION !!!!!!!!!!!! detectCheat passe à false");
			detectCheat = false;
			}
		else
			detectCheat = true;
		
		textEtatCPU = (TextView)findViewById(R.id.textEtatCPU);
		imageEtatCPU = (ImageView)findViewById(R.id.imageEtatCPU);
		textEtatWiFi = (TextView)findViewById(R.id.textEtatWiFi);
		imageEtatWiFi = (ImageView)findViewById(R.id.imageEtatWiFi);
		textEtatDataNet = (TextView)findViewById(R.id.textEtatDataNet);
		imageEtatDataNet = (ImageView)findViewById(R.id.imageEtatDataNet);
		textEtatGPS = (TextView)findViewById(R.id.textEtatGPS);
		imageEtatGPS = (ImageView)findViewById(R.id.imageEtatGPS);
		textEtatBlueTooth = (TextView)findViewById(R.id.textEtatBlueTooth);
		imageEtatBlueTooth = (ImageView)findViewById(R.id.imageEtatBlueTooth);
		textElapsedTime = (TextView)findViewById(R.id.TextElapsedTime);
		textSleepWake = (TextView)findViewById(R.id.TextSleepWake);
		
		if (MainActivity.isBlueToothInstalled())
			myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		TextView monTexteDuree = (TextView)findViewById(R.id.TestDuration);
		Log.d("BatteryBench", "R.string.TestDuration : " + R.string.TestDuration + " et getIdTestDuration : " + MainActivity.getIdTestDuration());
		monTexteDuree.setText((String)getText(R.string.TestDuration) + " : " + (String)getText(MainActivity.getIdTestDuration()));
		textBatteryLevel.setText((String)getText(R.string.battery_level) + " " + (String)getText(R.string.start) + MainActivity.getStartBatLevel() + "%");
		resetActivityIcons();
		
		Log.d("BatteryBench", "Dans RunTest");
		final Handler handler = new Handler() {
			int rawlevel;
			int startLevel;

			public void handleMessage(Message msgRecu) {
				rawlevel = getBatteryRawLevel();
				if ((testDuration != -2) && (rawlevel <= 30)) { // On ne sort sous 30% que si ce n'est pas un test a fond
					elapsedThread = true;
					if ((testDuration < 0) || (testDuration == 3600)) {
						Log.d("BatteryBench", "Batterie sous les 30%, msg.what corrigé en : " + ENVOIDATA);
						msgRecu.what = ENVOIDATA;
					}
					else {
						Log.d("BatteryBench", "Plus de batterie, msg.what corrigé en : " + TERMINE);
						msgRecu.what = TERMINE;
					}
				}
				
				switch (msgRecu.what) {
				case GPSOffline :
					Log.d("BatteryBench", "Action : GPSOffline");
					isGPSonline = true;
					if (!isOnPause)
						changeGpsStatus(R.drawable.sleeping, R.string.Sleeping);
					break;
				case GPSOnline :
					Log.d("BatteryBench", "Action : GPSOnline");
					isGPSonline = false;
					if (!isOnPause)
						changeGpsStatus(R.drawable.working, R.string.OnLine);
					break;
				case CPUSleeping :
					Log.d("BatteryBench", "Action : CPUSleeping");
					isCPUcomputing = false;
					if (!isOnPause)
						changeCpuStatus(R.drawable.sleeping, R.string.Sleeping);
					break;
				case CPUComputing :
					Log.d("BatteryBench", "Action : CPUComputing");
					isCPUcomputing = true;
					if (!isOnPause)
						changeCpuStatus(R.drawable.working, R.string.Working);
					break;
				case DataNetActive :
					Log.d("BatteryBench", "Action : DataNetActive");
					isDataNetActive = true;
					if (!isOnPause)
						changeDataNetStatus(R.drawable.working, R.string.Working);
					break;
				case DataNetWaiting :
					Log.d("BatteryBench", "Action : DataNetWaiting");
					isDataNetActive = false;
					if (!isOnPause)
						changeDataNetStatus(R.drawable.sleeping, R.string.Waiting);
					break;
				case WiFiScanning :
					Log.d("BatteryBench", "Action : WiFiScanning");
					isWiFiScanning = true;
					if (!isOnPause)
						changeWiFiStatus(R.drawable.working, R.string.Scanning);
					break;
				case WiFiSleeping :
					Log.d("BatteryBench", "Action : WiFiSleeping");
					isWiFiScanning = false;
					if (!isOnPause)
						changeWiFiStatus(R.drawable.sleeping, R.string.Sleeping);
					break;
				case BlueToothScanning :
					Log.d("BatteryBench", "Action : BlueToothScanning");
					isBlueToothScanning = true;
					if (!isOnPause)
						changeBlueToothStatus(R.drawable.working, R.string.Scanning);
					break;
				case BlueToothSleeping :
					Log.d("BatteryBench", "Action : BlueToothSleeping");
					isBlueToothScanning = false;
					if (!isOnPause)
						changeBlueToothStatus(R.drawable.sleeping, R.string.Sleeping);
					break;
				case TICK :
					Log.d("BatteryBench", "Action : tick");
					monTexte.setText ((String)getText(R.string.iteration) + " " + nbTotIterations);
					textBatteryLevel.setText((String)getText(R.string.battery_level) + " " + (String)getText(R.string.start) + MainActivity.getStartBatLevel() + "%, " + (String)getText(R.string.current) + " " + rawlevel + "%");
					if (MainActivity.is3GInstalled())
						textSignalStrength.setText ((String)getText(R.string.signal_strength) + " " + MainActivity.getCurrentSignalStrength());
					else
						textSignalStrength.setText ((String)getText(R.string.signal_strength) + " " + (String)getText(R.string.no_3G));
					if (nbDoubledgetEnCours == 0)
						httpResuText.setText(nbTotCarRecupHttp + " " + (String)getText(R.string.downloaded_chars));
					else
						httpResuText.setText(nbDoubledgetEnCours + " fail GET " + nbTotCarRecupHttp + " " + (String)getText(R.string.downloaded_chars));
					elapsedTime = (new Date()).getTime() - monTimeStampDeDepart;
					textElapsedTime.setText((String)getText(R.string.elapsed_time) + " " + (elapsedTime / 1000) + "s ");
					textSleepWake.setText((String)getText(R.string.sleep_delay) + " " + ((waitForScreenOffAgain - elapsedTime) / 1000) + "s, " + (String)getText(R.string.will_wake_in) + " " + ((waitForScreenOnAgain - elapsedTime) / 1000) + "s");
					avgSignalStrength += MainActivity.getCurrentSignalStrength();
					
					if ((waitForScreenOnAgain - elapsedTime) <= 0)
						mSoundManager.playSound(alarmSound);
					else if (isOnPause)
						mSoundManager.playSound(sonarSound);
					else
						mSoundManager.playSound(ploufSound);
					if ((testDuration == -2) && (rawlevel <= 20)){ // On ne commence à enregistrer que sous les 20%
						createURL(testDuration, MainActivity.getStartBatLevel(), rawlevel);
						CharSequence texteResultatSave = "";
						texteResultatSave = saveURL();
						Log.d("BatteryBench", "saveURL rend" + texteResultatSave);
						Log.d("BatteryBench", "Données à envoyer => monUrl : " + monUrl);
						String lesVariables[] = monUrl.split("&");
						for (int i = 0; i < lesVariables.length; i++)
							if (lesVariables[i].startsWith("elapsedtime"))
								Log.d("BatteryBench", "lesVariables[" + i + "] = " + lesVariables[i]);
						}
					break;
				case CHEAT :
					Log.d("BatteryBench", "Action : cheat");
					if (!detectCheat) {
						Log.d("BatteryBench", "ATTENTION !!!!!!!!!!! detectCheat à false");
					}
					else {
						LinearLayout leFullLayout = (LinearLayout)findViewById(R.id.ActionFullLayout);
						leFullLayout.setBackgroundColor(Color.RED);
						monTexte.setText ("");
						httpResuText.setText((String)getText(R.string.cheat_detected) + "\n" + bufCheat + " " + (String)getText(R.string.cancelled_test));
						elapsedThread = true;
					}
					break;
				case ENVOIDATA :
					Log.d("BatteryBench", "Action : envoidata");
					if (!dataAlreadySent) {
						elapsedTime = (new Date()).getTime() - monTimeStampDeDepart;
						if (MainActivity.is3GInstalled() && MainActivity.isWiFiInstalled()) {
							final Context context = getApplicationContext();
							final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					        wm.setWifiEnabled(false);
					        Log.d("BatteryBench", "On a 3G et WiFi => on coupe le WiFi");
						}
						else if (!MainActivity.is3GInstalled() && MainActivity.isWiFiInstalled()) {
							final Context context = getApplicationContext();
							final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					        wm.setWifiEnabled(true);
					        Log.d("BatteryBench", "On a pas de 3G mais on a WiFi => on met le WiFi");
						}
						startLevel = MainActivity.getStartBatLevel();
						if (nbTotIterations > 0)
							avgSignalStrength /= nbTotIterations;
	
						createURL(testDuration, startLevel, rawlevel);
						boolean resuget = false;
						for (int boucleGet = 0; boucleGet < 5; boucleGet++) { // 5 essais, on ne sait jamais...
							resuget = envoyerget(monContexte, monUrl);
							if (resuget) break;
							Log.d("BatteryBench", "resuget vaut " + resuget);
						}
						Log.d("BatteryBench", "resuget vaut " + resuget);
	//					resuget = false; // !!!
						CharSequence texteResultat = "";
						if (resuget)
							texteResultat = getText(R.string.sent_results);
						else
							texteResultat = saveURL();
						httpResuText.setText(texteResultat);
					}
					else {
						Log.d("BatteryBench", "Action : envoidata MAIS DATA DEJA ENVOYEES");
					}
					dataAlreadySent = true;
				case TERMINE :
					Log.d("BatteryBench", "Action : termine");
					resetActivityIcons();
					bufResu = (String)getText(R.string.completed_test)+ ", " + nbTotIterations + " " + (String)getText(R.string.iteration) + ", " + envoyerGetSuccess + " GET";
					LinearLayout leFullLayout = (LinearLayout)findViewById(R.id.ActionFullLayout);
					leFullLayout.setBackgroundColor(0xFF004400);
					monTexte.setText (bufResu);
					resetOriginalConfig();
					break;
				default :
					Log.d("BatteryBench", "Action INCONNUE msg.what reÃ§u : " + msgRecu.what);
					break;
				}
//			msgRecu.recycle();
			}

		};
		
        class asyncUseNetwork extends AsyncTask<String, Void, Void> {
		    protected Void doInBackground(String... urls) {
        		if (getEnCours) {
        			Log.d("BatteryBench", "+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=Demande d'un envoyerget avec un autre encore en cours");
        			nbDoubledgetEnCours++;
        			return null;
        		}
        		getEnCours = true;
        		if (!envoyerget(monContexte, "http://www.google.com")) {
					Log.d("BatteryBench", "PB dans un envoyerget");
				}
				else {
					envoyerGetSuccess++;
					nbTotCarRecupHttp += resuHttp.length();
				}
				Message msg = new Message();
				msg.what = DataNetWaiting;
				handler.sendMessage(msg);
				getEnCours = false;
		        return null;
		    }

		}

		Thread checkUpdate = new Thread() {
			int i,j,nbIterations,iterationStopGPS,iterationLigthScreenEnd;
			double fac;
			
			final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
			final WifiManager wm = (WifiManager) monContexte.getSystemService(Context.WIFI_SERVICE);
			LocationListener myListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					// Called when a new location is found by the network location provider.
					//makeUseOfNewLocation(location);
				}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};
			
			public void run() {
				Looper.prepare();
				PowerManager pm = (PowerManager)monContexte.getSystemService(Context.POWER_SERVICE);
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP, "BatteryBench");
				if (testDuration > 0)
					nbIterations = testDuration / 5;
				else
					nbIterations = 10;

				int startLevel = MainActivity.getStartBatLevel();
				elapsedThread = false;
				dataAlreadySent = false;
				
				if (MainActivity.isWiFiInstalled()) {
					// WiFi BroadcastReceiver implementation
					IntentFilter wiFiIntent = new IntentFilter();
					wiFiIntent.addAction (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
					registerReceiver(new BroadcastReceiver(){
						public void onReceive(Context c, Intent i){
							// Code to execute when SCAN_RESULTS_AVAILABLE_ACTION event	occurs
							WifiManager w = (WifiManager) c.getSystemService (Context.WIFI_SERVICE);
							w.getScanResults(); // Returns a <list> of scanResults
							Message msg = new Message();
							msg.what = WiFiSleeping;
							handler.sendMessage(msg);
						}
					}, wiFiIntent );
				}
				
				if (MainActivity.isBlueToothInstalled()) {
					// BlueTooth BroadcastReceiver implementation
					IntentFilter blueToothIntent = new IntentFilter();
					blueToothIntent.addAction (BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
					registerReceiver(new BroadcastReceiver(){
						public void onReceive(Context c, Intent i){
							Log.d("BatteryBench", "Réception de la demande BlueTooth");
							Message msg = new Message();
							msg.what = BlueToothSleeping;
							handler.sendMessage(msg);
						}
					}, blueToothIntent );
				}

				// Battery BroadcastReceiver implementation
				IntentFilter battIntent = new IntentFilter();
				battIntent.addAction (Intent.ACTION_BATTERY_CHANGED);
				registerReceiver(new BroadcastReceiver(){
					public void onReceive(Context c, Intent batteryIntent){
						int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
						Log.d("BatteryBench", "@ @ @ @ @ @ @ @ @ @ @  @ @ @ Dans Battery BroadcastReceiver avec status : " + status + " BATTERY_PLUGGED_AC vaut " + BatteryManager.BATTERY_PLUGGED_AC + " BATTERY_PLUGGED_USB vaut " + BatteryManager.BATTERY_PLUGGED_USB);
						// http://developer.android.com on BatteryManager (public static final String EXTRA_PLUGGED):
						// 0 means it is on battery, other constants are different types of power sources
						if (status != 0) // API 
							battCharging = true;
					}
				}, battIntent );

				waitForScreenOnAgain = MainActivity.getMyScreenTimeout() + screenOffDuration;
				waitForScreenOffAgain = MainActivity.getMyScreenTimeout();
				Log.d("BatteryBench", "AV entrée dans la boucle de 0 à " + nbIterations);
				for (i = 0; i < nbIterations;i++) {
					elapsedTime = (new Date()).getTime() - monTimeStampDeDepart;
					if ((testDuration == 30) && (elapsedTime > 30000)) break; // Break après 30 secondes
					if ((testDuration == 600) && (elapsedTime > 600000)) break; // Break après dix minutes
					if ((testDuration == 3600) && (elapsedTime > 3600000)) break; // Break après une heure

					if (elapsedThread) return;
					Log.d("BatteryBench", "AV sleep 5 No " + (nbTotIterations+1));
					try {
						Thread.sleep(SLEEP_DURATION);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
//						Log.d("BatteryBench", "sleep interrompu");
					}
					Log.d("BatteryBench", "AP sleep 5 No " + (nbTotIterations+1));
					
					int rawlevel = getBatteryRawLevel();

					{
						Message msg = new Message();
						msg.what = CPUComputing;
						handler.sendMessage(msg);
					}
					for (j = 0; j < NB_CALC_LOOP; j++) {
						fac = calcFactorielle(150);
					}
					{
						Message msg = new Message();
						msg.what = CPUSleeping;
						handler.sendMessage(msg);
					}
					
					Log.d("BatteryBench", "AP " + NB_CALC_LOOP + " x calcFactorielle(150) : " + fac);
					if(stopThread) {
						Log.d("BatteryBench", "Une interruption dans la figure et i vaut " + i);
						i--;
						continue;
					}

					nbTotIterations++;

					Log.d("BatteryBench", "()()()()()()()()()()()()() elapsedTime " + elapsedTime + " waitForScreenOnAgain " + waitForScreenOnAgain);
					if (elapsedTime > waitForScreenOnAgain) {
						waitForScreenOnAgain = elapsedTime + MainActivity.getMyScreenTimeout() + screenOffDuration;
						waitForScreenOffAgain = elapsedTime + MainActivity.getMyScreenTimeout();
						Log.d("BatteryBench", "Reallumage écran debut");
						wl.acquire();
						iterationLigthScreenEnd = nbTotIterations + 1;
					}
					
					if (nbTotIterations == iterationLigthScreenEnd) {
						Log.d("BatteryBench", "Fin reallumage écran");
						wl.release();
					}

					if (MainActivity.isGPSInstalled()) {
						if ((nbTotIterations % GPS_USAGE_RATE) == 0) {
							Log.d("BatteryBench", "Debut demande GPS");
							lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myListener);
							Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							iterationStopGPS = nbTotIterations + 1;
							gpsEnCours = true;
							Message msg = new Message();
							msg.what = GPSOnline;
							handler.sendMessage(msg);
						}
						if (nbTotIterations == iterationStopGPS) {
							Log.d("BatteryBench", "Fin demande GPS");
							lm.removeUpdates(myListener);
							gpsEnCours = false;
							Message msg = new Message();
							msg.what = GPSOffline;
							handler.sendMessage(msg);
						}
					}
					
					if (MainActivity.isWiFiInstalled()) {
						if ((nbTotIterations % WIFI_USAGE_RATE) == 0) {
							Log.d("BatteryBench", "Lancement de la demande WiFi");
							WifiManager wm = (WifiManager) getSystemService (Context.WIFI_SERVICE);
							wm.startScan();
							Log.d("BatteryBench", "Fin du lancement de la demande WiFi");
							Message msg = new Message();
							msg.what = WiFiScanning;
							handler.sendMessage(msg);
						}
					}
					
					if (MainActivity.isBlueToothInstalled()) {
						if (!MainActivity.isBlueToothEnabled()) {
		            		Log.d("BatteryBench", "Cheat : BlueTooth disabled !");
							bufCheat += (String)getText(R.string.bluetooth_must) + "\n";
							cheat = true;
		            	}
						else {
							if (!isOnPause || MainActivity.isBlueToothAvailableInSleepMode()) {
								if ((nbTotIterations % BLUETOOTH_USAGE_RATE) == 0) {
									Log.d("BatteryBench", "Lancement de la demande BlueTooth");
									myBluetoothAdapter.startDiscovery();
									Message msg = new Message();
									msg.what = BlueToothScanning;
									handler.sendMessage(msg);
								}
							}
						}
					}

					if (MainActivity.isWiFiInstalled() || (MainActivity.is3GInstalled())) {
						if ((nbTotIterations % DATANETWORK_USAGE_RATE) == 0) {
							Message msg = new Message();
							msg.what = DataNetActive;
							handler.sendMessage(msg);
							new asyncUseNetwork().execute();
						}
					}
					
					if (elapsedThread) return;

					if (isChargerPlugged()) {
						Log.d("BatteryBench", "Cheat : charger plugged !");
						bufCheat += (String)getText(R.string.charger_plugged) + "\n";
						cheat = true;
					}

					if (MainActivity.isGPSInstalled()) {
						if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
							Log.d("BatteryBench", "Cheat : GPS disabled !");
							bufCheat += (String)getText(R.string.gps_disabled) + "\n";
							cheat = true;
						}
					}

					if (MainActivity.isWiFiInstalled()) {
						if (MainActivity.getCurrentWiFiEnabled() != wm.isWifiEnabled()) {
							bufAtf = "WiFi changed expected ";
							if (MainActivity.getCurrentWiFiEnabled())
								bufAtf += "true";
							else
								bufAtf += "false";
							bufAtf += " got ";
							if (wm.isWifiEnabled())
								bufAtf += "true";
							else
								bufAtf += "false";
							Log.d("BatteryBench", "Cheat : " + bufAtf);
							bufCheat += bufAtf + "\n";
							cheat = true;
						}
					}

					Message msg = new Message();
					if (cheat && detectCheat) {
						msg.what = CHEAT;
						Log.d("BatteryBench", "Reallumage écran pour cause de cheat");
						wl.acquire();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
//							Log.d("BatteryBench", "sleep interrompu");
						}
						wl.release();
					}
					else
						msg.what = TICK;
					handler.sendMessage(msg);
					if (cheat  && detectCheat)
						elapsedThread = true;
					if (testDuration < 0) {
						Log.d("BatteryBench", "On est dans le cas sur % de charge de batterie, et i vaut " + i);
						i--;
						rawlevel = getBatteryRawLevel();
						Log.d("BatteryBench", "Batterie à " + rawlevel + "%");
						if ((testDuration == -5) && (rawlevel <= (startLevel - 5))) {
							Log.d("BatteryBench", "Baisse de 5% de la batterie : on sort !");
							break;
						}
						if ((testDuration == -1) && (rawlevel <= (startLevel - 50))) {
							Log.d("BatteryBench", "Baisse de 50% de la batterie : on sort !");
							break;
						}
						if ((testDuration != -2) && (rawlevel <= 30)) { // On ne quitte jamais si c'est un test a fond
							Log.d("BatteryBench", "Batterie sous les 30% : on sort !");
							break;
						}
					}
				}
				
				elapsedThread = true;
				Log.d("BatteryBench", "Mission accomplie");
				if (gpsEnCours == true) {
					Log.d("BatteryBench", "Fin demande GPS");
					lm.removeUpdates(myListener);
					gpsEnCours = false;
					Message msg = new Message();
					msg.what = GPSOffline;
					handler.sendMessage(msg);
				}
				Log.d("BatteryBench", "Reset original config");
				resetOriginalConfig();
				Log.d("BatteryBench", "Reallumage écran pour cause de fin de test");
				wl.acquire();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
//					Log.d("BatteryBench", "sleep interrompu");
				}
				wl.release();
				Message msg = new Message();
				if (!detectCheat || !cheat) {
					if ((testDuration < 0) || (testDuration == 3600)) {
						Log.d("BatteryBench", "Message envoi des data");
						msg.what = ENVOIDATA;
					}
					else {
						Log.d("BatteryBench", "Message terminé (sans envoi des data)");
						msg.what = TERMINE;
					}
					Log.d("BatteryBench", "Send end message");
					handler.sendMessage(msg);
				}
			}
		};
		Log.d("BatteryBench", "BatteryBenchRunTest lancement du thread !!!!!!!!!");
		checkUpdate.start();
	}

	private void resetActivityIcons() {
		if (MainActivity.isGPSInstalled())
			changeGpsStatus(R.drawable.sleeping, R.string.Sleeping);
		else
			changeGpsStatus(R.drawable.unknown, R.string.not_installed);
		if (MainActivity.isWiFiInstalled())
			changeWiFiStatus(R.drawable.sleeping, R.string.Sleeping);
		else
			changeWiFiStatus(R.drawable.unknown, R.string.not_installed);
		if (MainActivity.isWiFiInstalled() || (MainActivity.is3GInstalled()))
			changeDataNetStatus(R.drawable.sleeping, R.string.Sleeping);
		else
			changeDataNetStatus(R.drawable.unknown, R.string.not_installed);
		
		changeCpuStatus(R.drawable.sleeping, R.string.Sleeping);
		
		if (MainActivity.isBlueToothInstalled())
			changeBlueToothStatus(R.drawable.sleeping, R.string.Sleeping);
		else
			changeBlueToothStatus(R.drawable.unknown, R.string.not_installed);
	}

	public static void modifyThreadBoolStatus (boolean boolValue) {
		stopThread = boolValue;
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
			resuHttp = new String(baf.toByteArray());
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

	private double calcFactorielle(double num) {
		if (num == 0) return (double)1;
		return num * calcFactorielle (num - 1);
	}

	public void resetOriginalConfig () {
		final Context context = getApplicationContext();
		final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		if (MainActivity.isWiFiInstalled())
			wm.setWifiEnabled(MainActivity.getWasWiFiEnabled());
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, MainActivity.getOriginalScreenTimeout());

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

		Log.d("BatteryBench", "BatteryBenchRunTest a remis le lock");
		MainActivity.getMyLock().reenableKeyguard();
	}

	public static void elapsedThread (boolean status) {
		elapsedThread = status;
	}

	private void changeCpuStatus (int image, int etat) {
		imageEtatCPU.setImageResource(image);
		textEtatCPU.setText(getText(etat));
	}
	
	private void changeWiFiStatus (int image, int etat) {
		imageEtatWiFi.setImageResource(image);
		textEtatWiFi.setText(getText(etat));
	}
	
	private void changeDataNetStatus (int image, int etat) {
		imageEtatDataNet.setImageResource(image);
		textEtatDataNet.setText(getText(etat));
	}
	
	private void changeGpsStatus (int image, int etat) {
		imageEtatGPS.setImageResource(image);
		textEtatGPS.setText(getText(etat));
	}

	private void changeBlueToothStatus (int image, int etat) {
		imageEtatBlueTooth.setImageResource(image);
		textEtatBlueTooth.setText(getText(etat));
	}
	
	private int getBatteryRawLevel () {
		Intent batteryIntent = monContexte.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int rawlevel = batteryIntent.getIntExtra("level", -1);
		return rawlevel;
	}

	private boolean isChargerPlugged () {
		if (battCharging) {
			Log.d("BatteryBench", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Dans isChargerPlugged avec le flag battCharging vrai");
			return true;
		}
		Intent batteryIntent = monContexte.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryIntent.getIntExtra("status", -1);
		Log.d("BatteryBench", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Dans isChargerPlugged avec status : " + status + " et BATTERY_STATUS_CHARGING vaut " + BatteryManager.BATTERY_STATUS_CHARGING);
		if (status == BatteryManager.BATTERY_STATUS_CHARGING)
			return true;
		else
			return false;
	}

	private void createURL(final int testDuration, final int startLevel, final int rawlevel) {
		bufSendHttpResu = "?OStype=Android";
		bufSendHttpResu += "&manufacturer=";
		bufSendHttpResu += URLEncoder.encode(Build.MANUFACTURER);
		bufSendHttpResu += "&model=";
		bufSendHttpResu += URLEncoder.encode(Build.MODEL);
		bufSendHttpResu += "&board=";
		bufSendHttpResu += URLEncoder.encode(Build.BOARD);
		bufSendHttpResu += "&brand=";
		bufSendHttpResu += URLEncoder.encode(Build.BRAND);
		bufSendHttpResu += "&bbversion=";
		bufSendHttpResu += URLEncoder.encode(versionName);
		bufSendHttpResu += "&osversion=";
		bufSendHttpResu += URLEncoder.encode(Build.VERSION.RELEASE);
		bufSendHttpResu += "&typebench=";
		switch (testDuration) {
			case -5 :
				bufSendHttpResu += URLEncoder.encode("cinq_pourcent");
				break;
			case -1 :
				bufSendHttpResu += URLEncoder.encode("epuisement");
				break;
			case -2 :
				bufSendHttpResu += URLEncoder.encode("afond");
				break;
			case 3600 :
				bufSendHttpResu += URLEncoder.encode("une_heure");
				break;
			default :
				bufSendHttpResu += URLEncoder.encode("unknown");
				break;
		}
		bufSendHttpResu += "&iterations=";
		bufSendHttpResu += nbTotIterations;
		bufSendHttpResu += "&getsuccess=";
		bufSendHttpResu += envoyerGetSuccess;
		bufSendHttpResu += "&totcarget=";
		bufSendHttpResu += nbTotCarRecupHttp;
		bufSendHttpResu += "&startbatt=";
		bufSendHttpResu += startLevel;
		bufSendHttpResu += "&endbatt=";
		bufSendHttpResu += rawlevel;
		bufSendHttpResu += "&nbonpause=";
		bufSendHttpResu += NbTotOnPause;
		bufSendHttpResu += "&nbonstop=";
		bufSendHttpResu += NbTotOnStop;
		bufSendHttpResu += "&nbonstart=";
		bufSendHttpResu += NbTotOnStart;
		bufSendHttpResu += "&nbonresume=";
		bufSendHttpResu += NbTotOnResume;
		bufSendHttpResu += "&nbfailedget=";
		bufSendHttpResu += nbDoubledgetEnCours;
		bufSendHttpResu += "&avgSignalStrength=";
		bufSendHttpResu += avgSignalStrength;
		bufSendHttpResu += "&minSignalStrength=";
		bufSendHttpResu += MainActivity.getMinSignalStrength();
		bufSendHttpResu += "&maxSignalStrength=";
		bufSendHttpResu += MainActivity.getMaxSignalStrength();
		bufSendHttpResu += "&elapsedtime=";
		bufSendHttpResu += elapsedTime; // 
		monUrl = serverURL + bufSendHttpResu;
		Log.d("BatteryBench", "monUrl : " + monUrl);
	}

	private CharSequence saveURL() {
		CharSequence texteResultat;
		File fichierData = new File("/data/data/" + getPackageName(),"benchdata");
		if (fichierData.exists()) {
			fichierData.delete();
			Log.d("BatteryBench", "saveURL vire le précédent /data/data/" + getPackageName() + "/benchdata !");
		}
		try {
			FileOutputStream fileOutput = new FileOutputStream(fichierData);
			fileOutput.write(monUrl.getBytes());
			fileOutput.close();
			texteResultat = getText(R.string.saved_results);
		} catch (FileNotFoundException e) {
			texteResultat = getText(R.string.not_sent_results);
		} catch (IOException e) {
			texteResultat = getText(R.string.not_sent_results);
		}
		return texteResultat;
	}

}
