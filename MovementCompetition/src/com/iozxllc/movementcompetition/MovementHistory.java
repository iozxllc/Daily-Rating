package com.iozxllc.movementcompetition;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.google.android.gms.location.DetectedActivity;
import com.iozxllc.movementcompetition.MovementHistory.MovementBreakdown.MovementBreakdownItem;

public class MovementHistory {
	private static final String MOVEMENT_HISTORY_PATH = "movement-history";
	private ArrayList<Movement> movementHistory = new ArrayList<>();
	private static boolean needToReloadHistory = false;
	private static boolean historyHasEntry = false;
	
	public MovementHistory(Context context) {
		loadHistory(context);
	}
	
	public void loadHistory(Context context) {
		needToReloadHistory = false;
		movementHistory = getSavedHistory(context);
	}
	
	public boolean isNeedToReloadHistory() {
		return needToReloadHistory;
	}
	
	public ArrayList<Movement> getLoadedMovementHistory() {
		return movementHistory;
	}
	
	private static ArrayList<Movement> getSavedHistory(Context context) {
		ArrayList<Movement> staticMovementHistory = new ArrayList<>();
		try {
			FileInputStream fis = context.openFileInput(MOVEMENT_HISTORY_PATH);
			//FileInputStream fis = context.openFileInput(path);
			ObjectInputStream is = new ObjectInputStream(fis);
			staticMovementHistory = (ArrayList<Movement>) is.readObject();
			is.close();
		} catch (Exception e) {
			//System.out.println("Could not load from "+path);
			e.printStackTrace();
			//createProgress();
			//createHistory(context);
			return new ArrayList<>();
		}
		return staticMovementHistory;
	}
	
	/*private void createHistory(Context context) {
		movementHistory = new ArrayList<>();
		saveHistory(context, movementHistory);
	}*/
	
	private static void saveHistory(Context context, ArrayList<Movement> staticMovementHistory) {
		try {
			FileOutputStream fos = context.openFileOutput(MOVEMENT_HISTORY_PATH, Context.MODE_PRIVATE);
			//FileOutputStream fos = new FileOutputStream(path);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(staticMovementHistory);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized void addMovement(Movement movement, Context context) {
		System.err.println("Adding Movement: "+MovementHistory.getNameFromType(movement.movementTypeID, context));
		ArrayList<Movement> history = getSavedHistory(context);
		history.add(movement);
		saveHistory(context, history);
		needToReloadHistory = true;
		historyHasEntry = true;
	}
	
	/*public MovementBreakdown getTodaysBreakdown() {
		//TODO: this
		for (Movement movement : movementHistory) {
			
		}
		return null;
	}*/

	public MovementBreakdown getBreakdown(Context context, Calendar startDate, Calendar endDate) {
		loadHistory(context);
		MovementBreakdown breakdown = new MovementBreakdown();
		breakdown.startDate = Calendar.getInstance();// new Date();
		breakdown.endDate = Calendar.getInstance();// new Date();
		ArrayList<Movement> countableMovementsList = new ArrayList<>();
		breakdown.countableMovementsList = countableMovementsList;
		Map<Integer, Long> movementLengthMap = new HashMap<>();
		long totalTime = 0;
		for (int i = 0; i < movementHistory.size(); i++) {
			Movement movement = movementHistory.get(i);
			boolean countable = false;
			if (!movement.isStopMarker) {
				if (startDate == null) {
					if (endDate == null) {
						countable = true;
					} else {
						countable = movement.movementDate.before(endDate);
					}
				}
				if (endDate == null) {
					if (startDate == null) {
						countable = true;
					} else {
						countable = movement.movementDate.after(startDate);
					}
				}
				if (startDate != null && endDate != null) {
					countable = movement.movementDate.before(endDate) && movement.movementDate.after(startDate);
				}
			}
			/*if (startDate == null) {
				if (endDate == null) {
					countable = true;
				} else {
					countable = movement.movementDate.before(endDate);
				}
			} else if (endDate == null) {
				if (startDate == null) {
					countable = true;
				} else {
					countable = movement.movementDate.after(startDate);
				}
			} else {
				countable = movement.movementDate.before(endDate) && movement.movementDate.after(startDate);
			}*/
			if (countable) {
				countableMovementsList.add(movement);
				
				if (movement.movementDate.before(breakdown.startDate)) {
					breakdown.startDate = movement.movementDate;
				}
				if (movement.movementDate.after(breakdown.endDate)) {
					breakdown.endDate = movement.movementDate;
				}
				Long frequency = movementLengthMap.get(movement.movementTypeID);
				long timeTaken = 0;
				if (movementHistory.size()-1 > i) {
					timeTaken = movementHistory.get(i + 1).movementDate.getTimeInMillis() - movement.movementDate.getTimeInMillis();
				} else {
					timeTaken = Calendar.getInstance().getTimeInMillis() - movement.movementDate.getTimeInMillis();
				}
				if (frequency == null) {
					movementLengthMap.put(movement.movementTypeID, timeTaken);
				} else {
					movementLengthMap.put(movement.movementTypeID, movementLengthMap.get(movement.movementTypeID) + timeTaken);
				}
				totalTime += timeTaken;
			}
		}
		for (Integer detectedActivityTypeID : movementLengthMap.keySet()) {
			MovementBreakdownItem item = new MovementBreakdownItem();
			item.frequencyProportion = (double) movementLengthMap.get(detectedActivityTypeID) / (double) totalTime;
			item.millisSpent = movementLengthMap.get(detectedActivityTypeID);
			item.movementTypeID = detectedActivityTypeID;
			item.xmlDrawableID = getXMLIDFromDetectedActivityType(detectedActivityTypeID);
			item.borderColorID = getBorderColorIDFromDetectedActivityType(detectedActivityTypeID);
			breakdown.breakdownList.add(item);
		}

		return breakdown;
	}
	
	private int getXMLIDFromDetectedActivityType(int detectedActivityID) {
        switch(detectedActivityID) {
            case DetectedActivity.IN_VEHICLE:
                return R.xml.pie_segment_formatter_driving;
            case DetectedActivity.ON_BICYCLE:
                return R.xml.pie_segment_formatter_biking;
            case DetectedActivity.ON_FOOT:
                return R.xml.pie_segment_formatter_walking_running;
            case DetectedActivity.STILL:
                return R.xml.pie_segment_formatter_still;
            case DetectedActivity.UNKNOWN:
                return R.xml.pie_segment_formatter_still;
            case DetectedActivity.TILTING:
                return R.xml.pie_segment_formatter_tilting;
        }
        return 0;
	}
	
	private int getBorderColorIDFromDetectedActivityType(int detectedActivityID) {
        switch(detectedActivityID) {
            case DetectedActivity.IN_VEHICLE:
                return R.color.pie_chart_color_border_driving;
            case DetectedActivity.ON_BICYCLE:
                return R.color.pie_chart_color_border_biking;
            case DetectedActivity.ON_FOOT:
                return R.color.pie_chart_color_border_walking;
            case DetectedActivity.STILL:
                return R.color.pie_chart_color_border_still;
            case DetectedActivity.UNKNOWN:
                return R.color.pie_chart_color_border_still;
            case DetectedActivity.TILTING:
                return R.color.pie_chart_color_border_tilting;
        }
        return 0;
	}
	
	public static class Movement implements Serializable {
		private static final long serialVersionUID = 3644970252993680130L;
		public Calendar movementDate;
		public int movementTypeID;
		public int confidence;
		public boolean isStopMarker = false;
	}
	
	public static class MovementBreakdown {
		public Calendar startDate;
		public Calendar endDate;
		public ArrayList<MovementBreakdownItem> breakdownList = new ArrayList<>();
		public ArrayList<Movement> countableMovementsList = null;
		
		public static class MovementBreakdownItem {
			public int movementTypeID;
			public double frequencyProportion;
			public int xmlDrawableID;
			public int borderColorID;
			public long millisSpent;
		}
	}
	
	public static boolean historyHasEntry(Context context) {
		if (historyHasEntry) {
			return true;
		} else {
			ArrayList<Movement> history = getSavedHistory(context);
			if (history.size() > 0) {
				historyHasEntry = true;
				return true;
			} else {
				return false;
			}
		}
	}
    
    public static String getNameFromType(int activityType, Context context) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return context.getResources().getString(R.string.driving);
            case DetectedActivity.ON_BICYCLE:
                return context.getResources().getString(R.string.biking);
            case DetectedActivity.ON_FOOT:
                return context.getResources().getString(R.string.walking_running);
            case DetectedActivity.STILL:
                return context.getResources().getString(R.string.still);
            case DetectedActivity.UNKNOWN:
                return context.getResources().getString(R.string.unknown_movement);
            case DetectedActivity.TILTING:
                return context.getResources().getString(R.string.tilting);
            case -1:
                return context.getResources().getString(R.string.recording_stopped);
        }
        return context.getResources().getString(R.string.unknown_movement);
    }
}