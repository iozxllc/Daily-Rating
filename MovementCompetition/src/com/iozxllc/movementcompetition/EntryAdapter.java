package com.iozxllc.movementcompetition;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.iozxllc.movementcompetition.EntryAdapter.NavigationDrawerElement;

public class EntryAdapter extends ArrayAdapter<NavigationDrawerElement> {
	private Context context;
	private ArrayList<NavigationDrawerElement> drawerItems;
	private LayoutInflater vi;
	private MainActivity mainActivity = null;
	private NavigationDrawerFragment navDrawerFrag = null;
	
	public static class NavigationDrawerElement {
		public String viewChart;
		public String showAchievements;
		public String showLeaderboards;
		public String gamesServicesSwitch;
		public String recordEventsSwitch;
		
		public NavigationDrawerElement(String viewChart, String showAchievements, String showLeaderboards, String gamesServicesSwitch, String recordEventsSwitch) {
			this.viewChart = viewChart;
			this.showAchievements = showAchievements;
			this.showLeaderboards = showLeaderboards;
			this.gamesServicesSwitch = gamesServicesSwitch;
			this.recordEventsSwitch = recordEventsSwitch;
		}
	}
	 
	public EntryAdapter(Context context, ArrayList<NavigationDrawerElement> drawerItems, MainActivity mainActivity, NavigationDrawerFragment navDrawerFrag) {
		super(context, 0, drawerItems);
		this.navDrawerFrag = navDrawerFrag;
		this.mainActivity = mainActivity;
		this.context = context;
		this.drawerItems = drawerItems;
		vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	 
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		
        //Toast.makeText(parent.getContext(), "Making Nav Drawer", 2000).show();
		//MainActivity.entryAdapterInitialized = false;
		NavigationDrawerElement drawerItem = drawerItems.get(position);
		if (drawerItem != null) {
			if (drawerItem.viewChart != null) {
				v = vi.inflate(R.layout.list_item_standard_choice, null);
				((TextView) v.findViewById(R.id.itemTextView)).setText(drawerItem.viewChart);
			} else if (drawerItem.showAchievements != null) {
				v = vi.inflate(R.layout.list_item_standard_choice, null);
				((TextView) v.findViewById(R.id.itemTextView)).setText(drawerItem.showAchievements);
				
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mainActivity.tryShowAchievements();
					}					
				});
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
				
			} else if (drawerItem.showLeaderboards != null) {
				v = vi.inflate(R.layout.list_item_standard_choice, null);
				((TextView) v.findViewById(R.id.itemTextView)).setText(drawerItem.showLeaderboards);
				
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mainActivity.tryShowLeaderboards();
					}					
				});
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
				
			} else if (drawerItem.gamesServicesSwitch != null) {
				v = vi.inflate(R.layout.list_item_switch_choice, null);
				Switch theSwitch = ((Switch) v.findViewById(R.id.itemSwitch));
				theSwitch.setText(drawerItem.gamesServicesSwitch);

				theSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							mainActivity.savedWantsToConnectToGooglePlus = true;	
						} else {
							mainActivity.savedWantsToConnectToGooglePlus = false;
						}
						
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainActivity);
						SharedPreferences.Editor editor = sharedPref.edit();
				    	editor.putBoolean(MainActivity.SHARED_PREFERENCES_USE_GOOGLE_PLUS, mainActivity.savedWantsToConnectToGooglePlus);
				    	editor.commit();
				    	
				    	if (isChecked) {
				    		mainActivity.tryConnectToGooglePlus();
				    	} else {
				    		mainActivity.tryDisconnectToGooglePlus();
				    	}
					}
				});
				
				if (mainActivity.savedWantsToConnectToGooglePlus) {
					theSwitch.setChecked(true);
				} else {
					theSwitch.setChecked(false);
				}
				
				v.setOnClickListener(null);
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
			} else if (drawerItem.recordEventsSwitch != null) {
				v = vi.inflate(R.layout.list_item_switch_choice, null);
				Switch theSwitch = ((Switch) v.findViewById(R.id.itemSwitch));
				theSwitch.setText(drawerItem.recordEventsSwitch);
				
				if (mainActivity.isEventRecording) {
					theSwitch.setChecked(true);
				} else {
					theSwitch.setChecked(false);
				}
				
				theSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							mainActivity.isEventRecording = true;	
							mainActivity.startUpdates();						
						} else {
							mainActivity.isEventRecording = false;
							mainActivity.stopUpdates();
						}
						
						SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainActivity);
						SharedPreferences.Editor editor = sharedPref.edit();
				    	editor.putBoolean(MainActivity.SHARED_PREFERENCES_RECORD_EVENTS, mainActivity.isEventRecording);
				    	editor.commit();
					}					
				});
				
				v.setOnClickListener(null);
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
			}
		}
		
		return v;
	}
}