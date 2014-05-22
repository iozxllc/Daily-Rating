package com.iozxllc.movementcompetition;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.location.DetectedActivity;
import com.iozxllc.movementcompetition.MovementHistory.Movement;
import com.iozxllc.movementcompetition.MovementHistory.MovementBreakdown;
import com.iozxllc.movementcompetition.MovementHistory.MovementBreakdown.MovementBreakdownItem;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    
    private BreakdownChartFragment breakdownChartFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // Store the current request type (ADD or REMOVE)
    private REQUEST_TYPE mRequestType;
    // Used to track what type of request is in process
    private enum REQUEST_TYPE {ADD, REMOVE}
    
    /**
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    IntentFilter mBroadcastFilter;

    // Instance of a local broadcast manager
    private LocalBroadcastManager mBroadcastManager;

    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activity recognition update removal object
    private DetectionRemover mDetectionRemover;

    public MovementHistory movementHistory;
    public static String SHARED_PREFERENCES_RECORD_EVENTS = "SHARED_PREFERENCES_RECORD_EVENTS";
    public static String SHARED_PREFERENCES_USE_GOOGLE_PLUS = "SHARED_PREFERENCES_USE_GOOGLE_PLUS";
    public static int RC_RESOLVE = 9001;
    public static int REQUEST_ACHIEVEMENTS = 1238;
    public static int REQUEST_LEADERBOARD = 3232;
    public static Map<Integer, Integer> leaderboardsMap = new HashMap<>();
    static {
    	leaderboardsMap.put(R.string.leaderboard_title_most_biking_hours_in_a_day, R.string.leaderboard_most_biking_hours_in_a_day);
    	leaderboardsMap.put(R.string.leaderboard_title_most_biking_hours_in_a_month, R.string.leaderboard_most_biking_hours_in_a_month);
    	leaderboardsMap.put(R.string.leaderboard_title_most_driving_hours_in_a_day, R.string.leaderboard_most_driving_hours_in_a_day);
    	leaderboardsMap.put(R.string.leaderboard_title_most_driving_hours_in_a_month, R.string.leaderboard_most_driving_hours_in_a_month);
    	leaderboardsMap.put(R.string.leaderboard_title_most_sitting_hours_in_a_day, R.string.leaderboard_most_sitting_hours_in_a_day);
    	leaderboardsMap.put(R.string.leaderboard_title_most_sitting_hours_in_a_month, R.string.leaderboard_most_tilting_hours_in_a_month);
    	leaderboardsMap.put(R.string.leaderboard_title_most_tilting_hours_in_a_day, R.string.leaderboard_most_tilting_hours_in_a_day);
    	leaderboardsMap.put(R.string.leaderboard_title_most_tilting_hours_in_a_month, R.string.leaderboard_most_tilting_hours_in_a_month);
    	leaderboardsMap.put(R.string.leaderboard_title_most_walkingrunning_hours_in_a_day, R.string.leaderboard_most_walkingrunning_hours_in_a_day);
    	leaderboardsMap.put(R.string.leaderboard_title_most_walkingrunning_hours_in_a_month, R.string.leaderboard_most_walkingrunning_hours_in_a_month);
    }
    
    public boolean isEventRecording = true;
    public GoogleApiClient gamesClient;
    public boolean savedWantsToConnectToGooglePlus = false;
    private Timer chartPulseTimer = new Timer();
    private Timer achievementsLeaderboardsPulseTimer = new Timer();
    private boolean wasConnected = false;
    private boolean gamesInSignInFlow = false;
    private boolean gamesExplicitSignOut = false;
    private MainActivity thisMainActivity = this;
    private boolean isTryingToShowLeaderboards = false;
    private boolean isTryingToShowAchievements = false;
    //private boolean savedWantsUpdates = true;
    
    public void enableAchievements() {
    	if (gamesClient != null) {
			wasConnected = true;
			savedWantsToConnectToGooglePlus = true;
	    	System.out.println("trying g+ connect");
			new Thread(new Runnable() {
				@Override
				public void run() {
		            gamesClient.connect();
				}
			}).start();
    	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.DarkRed)));
        
        movementHistory = new MovementHistory(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
        // Set the broadcast receiver intent filer
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Create a new Intent filter for the broadcast receiver
        mBroadcastFilter = new IntentFilter(ActivityUtils.ACTION_REFRESH_STATUS_LIST);
        mBroadcastFilter.addCategory(ActivityUtils.CATEGORY_LOCATION_SERVICES);

        // Get detection requester and remover objects
        mDetectionRequester = new DetectionRequester(this);
        mDetectionRemover = new DetectionRemover(this);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		isEventRecording = sharedPref.getBoolean(SHARED_PREFERENCES_RECORD_EVENTS, true);
		//savedWantsUpdates = sharedPref.getBoolean(SHARED_PREFERENCES_RECORD_EVENTS, true);
		
		savedWantsToConnectToGooglePlus = sharedPref.getBoolean(SHARED_PREFERENCES_USE_GOOGLE_PLUS, false);

		// create an instance of Google API client and specify the Play services 
	    // and scopes to use. In this example, we specify that the app wants 
	    // access to the Games, Plus, and Cloud Save services and scopes.
	    GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
	    builder.addApi(Games.API)
	           /*.addApi(Plus.API)*/ 
	           /*.addApi(AppStateManager.API)*/
	           .addScope(Games.SCOPE_GAMES)
	           /*.addScope(Plus.SCOPE_PLUS_LOGIN)*/
	           .addConnectionCallbacks(new ConnectionCallbacks() {
		   			@Override
					public void onConnected(Bundle connectionHint) {
						System.out.println("HEY!!!!!!!!!!! TESTING FOR LEADERBOARS "+isTryingToShowLeaderboards+" "+isTryingToShowAchievements);
						gamesInSignInFlow = false;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mNavigationDrawerFragment.entryAdapter.notifyDataSetChanged();
								
								achievementsLeaderboardsPulseTimer = new Timer();
								achievementsLeaderboardsPulseTimer.scheduleAtFixedRate(new TimerTask() {
									@Override
									public void run() {
										updateAchievementsAndLeaderboardScores();
									}									
								}, 1000*30, 1000*30);
								
								if (isTryingToShowLeaderboards) {
									tryShowLeaderboards();
								}
								if (isTryingToShowAchievements) {
									tryShowAchievements();
								}
							}					
						});
						System.out.println("we connected");
						
			    		
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = sharedPref.edit();
			    		editor.putBoolean(SHARED_PREFERENCES_USE_GOOGLE_PLUS, true);
			    		editor.commit();	
					}
	
					@Override
					public void onConnectionSuspended(int cause) {
						System.out.println("connection suspended"+cause);
					}	    	
			    })
			    .addOnConnectionFailedListener(new OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(ConnectionResult result) {
						System.out.println("connect failed");
						if (result.hasResolution()) {
				            try {
				                // launch appropriate UI flow (which might, for example, be the
				                // sign-in flow)
				                result.startResolutionForResult(thisMainActivity, MainActivity.RC_RESOLVE);
				            } catch (SendIntentException e) {
				                // Try connecting again
				            	if (!gamesClient.isConnected()) {
				            		gamesInSignInFlow = true;
				            		gamesClient.connect();
				            	}
				            }
						} else {
							SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(Context.MODE_PRIVATE);
							SharedPreferences.Editor editor = sharedPref.edit();
				    		editor.putBoolean(MainActivity.SHARED_PREFERENCES_USE_GOOGLE_PLUS, false);
				    		editor.commit();
							
				    		AlertDialog.Builder builder = new AlertDialog.Builder(thisMainActivity);
				            builder.setMessage(R.string.unable_to_connect_to_google_plus)
				            	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							    	public void onClick(DialogInterface dialog, int id) {
							        	
							    	}
				            	});
				            builder.create(); 
				            builder.show();
						}
			
					}	    	
			    });
	    gamesClient = builder.build();
	    tryConnectToGooglePlus();
		
        startUpdates();
    }
    
    private void updateAchievementsAndLeaderboardScores() {
    	if (movementHistory != null && gamesClient != null && gamesClient.isConnected()) {
    		if (movementHistory.isNeedToReloadHistory()) {
    			movementHistory.loadHistory(this);
    		}
    		MovementBreakdown allTimeBreakdown = movementHistory.getBreakdown(this, null, null);
        	Calendar ce = Calendar.getInstance();
        	ce.set(ce.get(Calendar.YEAR), ce.get(Calendar.MONTH), ce.get(Calendar.DAY_OF_MONTH), 0, 0);
        	Calendar cs = Calendar.getInstance();
        	cs.set(cs.get(Calendar.YEAR), cs.get(Calendar.MONTH), cs.get(Calendar.DAY_OF_MONTH), 23, 59);
    		MovementBreakdown todaysTimeBreakdown = movementHistory.getBreakdown(this, ce, cs);
        	ce = Calendar.getInstance();
        	ce.set(ce.get(Calendar.YEAR), ce.get(Calendar.MONTH), 0, 0, 0);
        	cs = Calendar.getInstance();
        	cs.set(cs.get(Calendar.YEAR), cs.get(Calendar.MONTH), cs.get(Calendar.DAY_OF_MONTH), 23, 59);
    		MovementBreakdown thisMonthsTimeBreakdown = movementHistory.getBreakdown(this, ce, cs);
    		
    		//Achievement: couch potato (still for 4 straight hours)
    		long thresholdMillis = 1000 * 60 * 60 * 4;
    		int i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.STILL || movement.movementTypeID == DetectedActivity.UNKNOWN)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_couch_potato));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: tilt (tilt for 1 minute straight)
    		thresholdMillis = 1000 * 60 * 1;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.TILTING)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_tilt));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: stop and go (drive for 5 or less minutes)
    		thresholdMillis = 1000 * 60 * 5;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.IN_VEHICLE)
						&& timeTaken <= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_stop_and_go));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: road trip (drive for 3 hours straight)
    		thresholdMillis = 1000 * 60 * 60 * 3;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.IN_VEHICLE)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_road_trip));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: shortcut (walk for 5 minutes straight)
    		thresholdMillis = 1000 * 60 * 5;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.ON_FOOT)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_shortcut));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: the long way (Walk for 20 minutes straight)
    		thresholdMillis = 1000 * 60 * 20;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.ON_FOOT)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_the_long_way));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: journey (Walk for 1 hour straight)
    		thresholdMillis = 1000 * 60 * 60 * 1;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.ON_FOOT)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_journey));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: marathon (Walk for 3 hours straight)
    		thresholdMillis = 1000 * 60 * 60 * 3;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				long timeTaken = 0;
				if (allTimeBreakdown.countableMovementsList.size()-1 > i) {
					timeTaken = allTimeBreakdown.countableMovementsList.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				
				if ((movement.movementTypeID == DetectedActivity.ON_FOOT)
						&& timeTaken >= thresholdMillis) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_marathon));
					break;
				}
				
				i++;
    		}
    		
    		//Achievement: wheel spinner (Walk for 3 hours straight)
    		thresholdMillis = 1000 * 60 * 60 * 3;
    		i = 0;
    		for (Movement movement : allTimeBreakdown.countableMovementsList) {
				if ((movement.movementTypeID == DetectedActivity.ON_BICYCLE)) {
					Games.Achievements.unlock(gamesClient, getResources().getString(R.string.achievement_wheel_spinner));
					break;
				}
				
				i++;
    		}
    		
    		//Leaderboard: walking/running in a day
    		for (MovementBreakdownItem item : todaysTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.ON_FOOT) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_walkingrunning_hours_in_a_day), hours);
    			}
    		}
    		
    		//Leaderboard: sitting in a day
    		for (MovementBreakdownItem item : todaysTimeBreakdown.breakdownList) {
    			long hours = 0;
    			if (item.movementTypeID == DetectedActivity.STILL || item.movementTypeID == DetectedActivity.UNKNOWN) {
    				hours += item.millisSpent / (1000L * 60L * 60L);
    			}
				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_sitting_hours_in_a_day), hours);
    		}
    		
    		//Leaderboard: driving in a day
    		for (MovementBreakdownItem item : todaysTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.IN_VEHICLE) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_driving_hours_in_a_day), hours);
    			}
    		}
    		
    		//Leaderboard: tilting in a day
    		for (MovementBreakdownItem item : todaysTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.TILTING) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_tilting_hours_in_a_day), hours);
    			}
    		}
    		
    		//Leaderboard: biking in a day
    		for (MovementBreakdownItem item : todaysTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.TILTING) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_biking_hours_in_a_day), hours);
    			}
    		}
    		
    		/////

    		
    		//Leaderboard: walking/running in a month
    		for (MovementBreakdownItem item : thisMonthsTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.ON_FOOT) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_walkingrunning_hours_in_a_month), hours);
    			}
    		}
    		
    		//Leaderboard: sitting in a month
    		for (MovementBreakdownItem item : thisMonthsTimeBreakdown.breakdownList) {
    			long hours = 0;
    			if (item.movementTypeID == DetectedActivity.STILL || item.movementTypeID == DetectedActivity.UNKNOWN) {
    				hours += item.millisSpent / (1000L * 60L * 60L);
    			}
				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_sitting_hours_in_a_month), hours);
    		}
    		
    		//Leaderboard: driving in a month
    		for (MovementBreakdownItem item : thisMonthsTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.IN_VEHICLE) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_driving_hours_in_a_month), hours);
    			}
    		}
    		
    		//Leaderboard: tilting in a month
    		for (MovementBreakdownItem item : thisMonthsTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.TILTING) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_tilting_hours_in_a_month), hours);
    			}
    		}
    		
    		//Leaderboard: biking in a month
    		for (MovementBreakdownItem item : thisMonthsTimeBreakdown.breakdownList) {
    			if (item.movementTypeID == DetectedActivity.TILTING) {
    				long hours = item.millisSpent / (1000L * 60L * 60L);
    				Games.Leaderboards.submitScore(gamesClient, getResources().getString(R.string.leaderboard_most_biking_hours_in_a_month), hours);
    			}
    		}
    	}
    }
    
    public void tryShowAchievements() {
    	System.out.println("Trying to show achievements");
    	if (gamesClient != null) {
        	System.out.println("not null");
    		if (gamesClient.isConnected()) {
            	System.out.println("connected");
    			isTryingToShowAchievements = false;
    			startActivityForResult(Games.Achievements.getAchievementsIntent(gamesClient), REQUEST_ACHIEVEMENTS);
    		} else {
    			isTryingToShowAchievements = true;
        		new Thread(new Runnable() {
    				@Override
    				public void run() {
    		            gamesClient.connect();
    				}
        		}).start();
    		}
    	}
    }
    
    public void tryShowLeaderboards() {
    	if (gamesClient != null) {
    		if (gamesClient.isConnected()) {
    			isTryingToShowLeaderboards = false;
    			String[] titles = new String[leaderboardsMap.keySet().size()];
    			int index = 0;
    			final Integer[] keySet = leaderboardsMap.keySet().toArray(new Integer[leaderboardsMap.keySet().size()]);
    			for (Integer id : keySet) {
    				titles[index] = getResources().getString(id);
    				
    				index++;
    			}
    			
    			ListView listView = new ListView(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                  android.R.layout.simple_list_item_1, android.R.id.text1, titles);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						int keyID = keySet[position];
						
						startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gamesClient, getResources().getString(leaderboardsMap.get(keyID))), REQUEST_LEADERBOARD);
					}
				});
    			
	    		AlertDialog.Builder builder = new AlertDialog.Builder(thisMainActivity);
	            builder.setView(listView)
	            	.setTitle(R.string.leaderboards_choice_title);
	            builder.create(); 
	            builder.show();
    		} else {
    			isTryingToShowLeaderboards = true;
        		new Thread(new Runnable() {
    				@Override
    				public void run() {
    		            gamesClient.connect();
    				}
        		}).start();
    		}
    	}
    }
    
    public void tryConnectToGooglePlus() {
	    if (savedWantsToConnectToGooglePlus) {
    		new Thread(new Runnable() {
				@Override
				public void run() {
		            gamesClient.connect();
				}
    		}).start();
	    }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
    	breakdownChartFragment = BreakdownChartFragment.newInstance(null, null);
    	
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, breakdownChartFragment)
                .commit();
        
        chartPulseTimer.cancel();
        chartPulseTimer = new Timer();
        chartPulseTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (breakdownChartFragment != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							cautiousUpdateActivityHistory();
						}						
					});
				}
			}        	
        }, 1000*5, 1000*5);
    }

    public void onSectionAttached(String title) {
    	mTitle = title;
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(mTitle);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * DetectionRemover and DetectionRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        // Choose what to do based on the request code
        if (requestCode == ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST) {

            switch (resultCode) {
                // If Google Play services resolved the problem
                case Activity.RESULT_OK:

                    // If the request was to start activity recognition updates
                    if (REQUEST_TYPE.ADD == mRequestType) {

                        // Restart the process of requesting activity recognition updates
                        mDetectionRequester.requestUpdates();

                    // If the request was to remove activity recognition updates
                    } else if (REQUEST_TYPE.REMOVE == mRequestType ){

                            /*
                             * Restart the removal of all activity recognition updates for the 
                             * PendingIntent.
                             */
                            mDetectionRemover.removeUpdates(
                                mDetectionRequester.getRequestPendingIntent());

                    }
                break;

                // If any other result was returned by Google Play services
                default:

                    // Report that Google Play services was unable to resolve the problem.
                    Log.d(ActivityUtils.APPTAG, getString(R.string.no_resolution));
            }
    	} else if (requestCode == MainActivity.RC_RESOLVE) {
    		new Thread(new Runnable() {
    			@Override
    			public void run() {
    		    	if (resultCode == Activity.RESULT_OK) {
    		            // Ready to try to connect again.
    		            gamesClient.connect();
    		        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
    		        	gamesClient.connect();
    		        } else if (resultCode == Activity.RESULT_CANCELED) {
    		            // User cancelled.
    		        	gamesClient.disconnect();
    		    		gamesInSignInFlow = false;
    		        }
    			}
    		}).start();
        } else {
           // Report that this Activity received an unknown requestCode
           Log.d(ActivityUtils.APPTAG,
                   getString(R.string.unknown_activity_request_code, requestCode));
        }
    }

    /*
     * Register the broadcast receiver and update the log of activity updates
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Register the broadcast receiver
        mBroadcastManager.registerReceiver(
                updateListReceiver,
                mBroadcastFilter);

        // Load updated activity history
        updateActivityHistory();
    }
    
    /*
     * Unregister the receiver during a pause
     */
    @Override
    protected void onPause() {

        // Stop listening to broadcasts when the Activity isn't visible.
        mBroadcastManager.unregisterReceiver(updateListReceiver);

        super.onPause();
    }    
    
    @Override
    protected void onStop() {
    	achievementsLeaderboardsPulseTimer.cancel();
    	super.onStop();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	if (!gamesInSignInFlow && !gamesExplicitSignOut && wasConnected) {
            // auto sign in
    		gamesInSignInFlow = true;
    		new Thread(new Runnable() {
				@Override
				public void run() {
		            gamesClient.connect();
				}
    		}).start();
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(ActivityUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

        // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            return false;
        }
    }

    public void startUpdates() {

        // Check for Google Play services
        if (!servicesConnected()) {
        	System.err.println("Services not connected yet!");
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mDetectionRequester.requestUpdates();
        System.out.println("updates are requested");
    }

    /**
     * Respond to "Stop" button by canceling updates.
     * @param view The view that triggered this method.
     */
    public void stopUpdates() {

        // Check for Google Play services
        if (!servicesConnected()) {

            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mDetectionRemover.removeUpdates(mDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        mDetectionRequester.getRequestPendingIntent().cancel();
        
        Movement movement = new Movement();
        movement.confidence = 100;
        movement.isStopMarker = true;
        movement.movementDate = Calendar.getInstance();
        movement.movementTypeID = -1;
        MovementHistory.addMovement(movement, this);
        
        isEventRecording = false;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(SHARED_PREFERENCES_RECORD_EVENTS, isEventRecording);
		editor.commit();
    }
    
    /**
     * Broadcast receiver that receives activity update intents
     * It checks to see if the ListView contains items. If it
     * doesn't, it pulls in history.
     * This receiver is local only. It can't read broadcast Intents from other apps.
     */
    BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            /*
             * When an Intent is received from the update listener IntentService, update
             * the displayed log.
             */
        	System.out.println("Got broadcast intent from Receiver");
            updateActivityHistory();
        }
    };
    

    /**
     * Display the activity detection history stored in the
     * log file
     */
    private void updateActivityHistory() {
    	movementHistory.loadHistory(this);
    	if (breakdownChartFragment != null) {
    		breakdownChartFragment.renderMovementHistoryPieChart();
    		breakdownChartFragment.updateEventHistory();
    	}
    }
    
    private void cautiousUpdateActivityHistory() {
    	if (movementHistory.isNeedToReloadHistory()) {
    		movementHistory.loadHistory(this);
    	}
    	if (breakdownChartFragment != null) {
    		breakdownChartFragment.renderMovementHistoryPieChart();
    		breakdownChartFragment.updateEventHistory();
    	}
    }
}
