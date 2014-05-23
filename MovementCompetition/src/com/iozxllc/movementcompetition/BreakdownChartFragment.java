package com.iozxllc.movementcompetition;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.google.android.gms.location.DetectedActivity;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.iozxllc.movementcompetition.MovementHistory.Movement;
import com.iozxllc.movementcompetition.MovementHistory.MovementBreakdown;
import com.iozxllc.movementcompetition.MovementHistory.MovementBreakdown.MovementBreakdownItem;


/**
 * A placeholder fragment containing a simple view.
 */
public class BreakdownChartFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_START_DATE = "breakdown_chart_start_date";
    private static final String ARG_END_DATE = "breakdown_chart_end_date";
    
    private PieChart chart;
    
    private MainActivity mainActivity;
    private Calendar startDate = null;
    private Calendar endDate = null;
    private BreakdownChartFragment thisBreakdownChartFragment = this;
    private boolean legendTimesShowing = false;
    private BiMap<Integer, Integer> legendTimeTextViewsAndMovementIDs = HashBiMap.create();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BreakdownChartFragment newInstance(/*BreakdownChartType chartType, */Date startDate, Date endDate) {
    	BreakdownChartFragment fragment = new BreakdownChartFragment();
        Bundle args = new Bundle();
        if (startDate != null) {
        	args.putLong(ARG_START_DATE, startDate.getTime());
        }
        if (endDate != null) {
        	args.putLong(ARG_END_DATE, endDate.getTime());
        }
        fragment.setArguments(args);
        return fragment;
    }

    public BreakdownChartFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View rootView = inflater.inflate(R.layout.fragment_breakdown_chart, container, false);

        legendTimeTextViewsAndMovementIDs.put(R.id.pieChartLegendTextViewTimeStill, DetectedActivity.STILL);
        legendTimeTextViewsAndMovementIDs.put(R.id.pieChartLegendTextViewTimeBiking, DetectedActivity.ON_BICYCLE);
        legendTimeTextViewsAndMovementIDs.put(R.id.pieChartLegendTextViewTimeDriving, DetectedActivity.IN_VEHICLE);
        legendTimeTextViewsAndMovementIDs.put(R.id.pieChartLegendTextViewTimeTilting, DetectedActivity.TILTING);
        legendTimeTextViewsAndMovementIDs.put(R.id.pieChartLegendTextViewTimeWalking, DetectedActivity.ON_FOOT);
        
        Spinner breakdownTitleSpinner = (Spinner) rootView.findViewById(R.id.breakdown_title);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.breakdownTitlesArray, android.R.layout.simple_spinner_item);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		// Apply the adapter to the spinner
		breakdownTitleSpinner.setAdapter(adapter);
		
		breakdownTitleSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
		        mainActivity.onSectionAttached(getResources().getStringArray(R.array.breakdownTitlesArray)[position]);
		        boolean updated = false;
		        if (position == 0) {
		        	updated = true;
					thisBreakdownChartFragment.endDate = null;
					thisBreakdownChartFragment.startDate = null;
		        } else if (position == 1) {
		        	updated = true;
		        	Calendar ce = Calendar.getInstance();
		        	ce.set(ce.get(Calendar.YEAR), ce.get(Calendar.MONTH), ce.get(Calendar.DAY_OF_MONTH), 0, 0);
					thisBreakdownChartFragment.startDate = ce;
		        	Calendar cs = Calendar.getInstance();
		        	cs.set(cs.get(Calendar.YEAR), cs.get(Calendar.MONTH), cs.get(Calendar.DAY_OF_MONTH), 23, 59);
					thisBreakdownChartFragment.endDate = cs;
		        } else if (position == 2) {
		        	updated = true;
		        	Calendar ce = Calendar.getInstance();
		        	ce.set(ce.get(Calendar.YEAR), ce.get(Calendar.MONTH), 0, 0, 0);
					thisBreakdownChartFragment.startDate = ce;
		        	Calendar cs = Calendar.getInstance();
		        	cs.set(cs.get(Calendar.YEAR), cs.get(Calendar.MONTH), cs.get(Calendar.DAY_OF_MONTH), 23, 59);
					thisBreakdownChartFragment.endDate = cs;
		        }
		        if (updated) {
					thisBreakdownChartFragment.renderMovementHistoryPieChart();
					thisBreakdownChartFragment.updateEventHistory();
		        }
		        renderMovementHistoryPieChart();
		        updateEventHistory();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }			
		});

		ImageButton startDateButton = (ImageButton) rootView.findViewById(R.id.startDateButton);
		DateButtonClickListener startListener = new DateButtonClickListener();
		startListener.isStartDate = true;
		startListener.fragment = this;
		startDateButton.setOnClickListener(startListener);

		ImageButton endDateButton = (ImageButton) rootView.findViewById(R.id.endDateButton);
		DateButtonClickListener endListener = new DateButtonClickListener(); 
		endListener.isStartDate = false;
		endListener.fragment = this;
		endDateButton.setOnClickListener(endListener);
		
		RelativeLayout legendLayout = (RelativeLayout) rootView.findViewById(R.id.legendLayout);
		legendLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleLegendTimesView();
			}			
		});
        
        return rootView;
    }
    
    private void toggleLegendTimesView() {
    	if (legendTimesShowing) {
    		legendTimesShowing = false;
    		for (Integer id : legendTimeTextViewsAndMovementIDs.keySet()) {
    			getView().findViewById(id).setVisibility(View.GONE);
    		}
    	} else {
    		legendTimesShowing = true;
    		for (Integer id : legendTimeTextViewsAndMovementIDs.keySet()) {
    			TextView tv = (TextView) getView().findViewById(id);
    			if (!tv.getText().toString().trim().equals("")) {
    				tv.setVisibility(View.VISIBLE);
    			}
    		}
    	}
    }
    
    public static class DateButtonClickListener implements OnClickListener {
    	public boolean isStartDate = true;
    	public BreakdownChartFragment fragment = null;
    	
		@Override
		public void onClick(View v) {
			final DatePicker datePicker = new DatePicker(fragment.getActivity());
			Calendar pretendStartDate = fragment.getFirstDate();

			Calendar pretendEndDate = fragment.getLastDate();

			if (isStartDate) {
				if (fragment.startDate != null) {
					datePicker.updateDate(fragment.startDate.get(Calendar.YEAR), 
							fragment.startDate.get(Calendar.MONTH), 
							fragment.startDate.get(Calendar.DAY_OF_MONTH));
				} else {
					if (pretendStartDate != null) {
						datePicker.updateDate(pretendStartDate.get(Calendar.YEAR), 
								pretendStartDate.get(Calendar.MONTH), 
								pretendStartDate.get(Calendar.DAY_OF_MONTH));
					}
				}
			} else {
				if (fragment.endDate != null) {
					datePicker.updateDate(fragment.endDate.get(Calendar.YEAR), 
							fragment.endDate.get(Calendar.MONTH), 
							fragment.endDate.get(Calendar.DAY_OF_MONTH));
				} else {
					if (pretendEndDate != null) {
						datePicker.updateDate(pretendEndDate.get(Calendar.YEAR), 
								pretendEndDate.get(Calendar.MONTH), 
								pretendEndDate.get(Calendar.DAY_OF_MONTH));
					}
				}
			}
        	AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        	int titleID = isStartDate
        			? R.string.popup_title_set_start_date
        			: R.string.popup_title_set_end_date;
            builder.setView(datePicker)
            	.setTitle(titleID)
            	.setPositiveButton(fragment.getActivity().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (isStartDate) {
							if ((fragment.endDate != null
									&& fragment.endDate.getTimeInMillis() > getDateFromDatePicker(datePicker).getTimeInMillis())
									|| fragment.endDate == null) {
								fragment.startDate = getDateFromDatePicker(datePicker);
							}
						} else {
							if ((fragment.startDate != null
									&& fragment.startDate.getTimeInMillis() < getDateFromDatePicker(datePicker).getTimeInMillis())
									|| fragment.startDate == null) {
								fragment.endDate = getDateFromDatePicker(datePicker);
							}
						}
						fragment.renderMovementHistoryPieChart();
						fragment.updateEventHistory();
						fragment.setCustomDateRange();
					}
				})
            	.setNegativeButton(fragment.getActivity().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) { }
				});
            builder.create();
            builder.show();
		}    	
    }
    
    public Calendar getFirstDate() {
    	if (mainActivity.movementHistory.getLoadedMovementHistory().size() > 0) {
    		return mainActivity.movementHistory.getLoadedMovementHistory().get(0).movementDate;
    	}
    	return null;
    }
    
    public Calendar getLastDate() {
    	if (mainActivity.movementHistory.getLoadedMovementHistory().size() > 0) {
    		return mainActivity.movementHistory.getLoadedMovementHistory()
    				.get(mainActivity.movementHistory.getLoadedMovementHistory().size()-1)
    				.movementDate;
    	}
    	return null;
    }
    
    public static Calendar getDateFromDatePicker(DatePicker datePicker) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year =  datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));*/
        //chartType = (BreakdownChartType) getArguments().getSerializable(ARG_CHART_TYPE);
        long start = getArguments().getLong(ARG_START_DATE);
        if (start != 0L) {
        	Calendar c = Calendar.getInstance();
        	c.setTimeInMillis(start);
        	startDate = c;
        }
        long end = getArguments().getLong(ARG_END_DATE);
        if (end != 0L) {
        	Calendar c = Calendar.getInstance();
        	c.setTimeInMillis(end);
        	endDate = c;
        }
    	mainActivity = (MainActivity) activity;
        mainActivity.onSectionAttached(getResources().getString(R.string.chart_title_lifetime));
    }

    public void renderMovementHistoryPieChart() {
    	if(getView() != null) {
	    	System.out.println("rendering pie chart "+startDate+" "+endDate);
	    	
	    	MovementBreakdown breakdown = mainActivity.movementHistory.getBreakdown(getActivity(), startDate, endDate);
	    	
	    	if (breakdown.breakdownList.size() > 0) {
	    		getView().findViewById(R.id.waitingForEventTextView).setVisibility(View.GONE);
	    	}
	    	
	    	if (getView() != null) {
		        chart = (PieChart) getView().findViewById(R.id.mySimplePieChart);
		        chart.clear();
		
		        if (mainActivity.movementHistory.isNeedToReloadHistory()) {
		        	mainActivity.movementHistory.loadHistory(getActivity());
		        }
		        
		        for (MovementBreakdownItem breakdownItem : breakdown.breakdownList) {
		        	Segment segment = new Segment(MovementHistory.getNameFromType(breakdownItem.movementTypeID, getActivity()), breakdownItem.frequencyProportion);
		
		        	BiMap<Integer, Integer> reverseLegendTimes = legendTimeTextViewsAndMovementIDs.inverse();
		        	int textViewID = reverseLegendTimes.get(breakdownItem.movementTypeID);
		        	String newText = String.format(Locale.US, " (%02d:%02d:%02d)",
		        			TimeUnit.MILLISECONDS.toHours(breakdownItem.millisSpent),
		        			(TimeUnit.MILLISECONDS.toMinutes(breakdownItem.millisSpent) 
		        					- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(breakdownItem.millisSpent))),
		        			(TimeUnit.MILLISECONDS.toSeconds(breakdownItem.millisSpent) 
		        					- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(breakdownItem.millisSpent))));
		        	((TextView) getView().findViewById(textViewID)).setText(newText);
			        //EmbossMaskFilter emf = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 10, 8.2f);
			        
			        SegmentFormatter sf = new SegmentFormatter();
			        sf.configure(getActivity(), breakdownItem.xmlDrawableID);
			        sf.getRadialEdgePaint().setColor(getResources().getColor(breakdownItem.borderColorID));
			        sf.getRadialEdgePaint().setStrokeWidth(getResources().getDimension(R.dimen.pie_chart_border_width));
			        sf.getRadialEdgePaint().setAntiAlias(true);
			        sf.getFillPaint().setAntiAlias(true);
			        sf.getInnerEdgePaint().setAntiAlias(true);
			        sf.getOuterEdgePaint().setAntiAlias(true);
			        //sf.getFillPaint().setMaskFilter(emf);
			        chart.addSeries(segment, sf);
			    }
		
		        chart.getBorderPaint().setColor(Color.TRANSPARENT);
		        chart.getBackgroundPaint().setColor(Color.TRANSPARENT);
		        PieRenderer pieRenderer = chart.getRenderer(PieRenderer.class);
		        if (pieRenderer != null) {
		        	pieRenderer.setDonutSize(0.25f, PieRenderer.DonutMode.PERCENT);
		        }
		        chart.redraw();
	    	}
    	}
    }
    
    public void setCustomDateRange() {
        Spinner breakdownTitleSpinner = (Spinner) getView().findViewById(R.id.breakdown_title);
        breakdownTitleSpinner.setSelection(breakdownTitleSpinner.getCount() - 1);
    }
    
    public void updateEventHistory() {
    	if (getView() != null) {
	    	LinearLayout eventsHolderLayout = (LinearLayout) getView().findViewById(R.id.eventItemsLayout);
	    	
	    	ArrayList<Movement> dupesAllMovementsList = mainActivity.movementHistory.getLoadedMovementHistory();
	    	ArrayList<Movement> allMovementsList = new ArrayList<>();
	    	int lastSeenMovementType = -1;
	    	for (Movement movement : dupesAllMovementsList) {
	    		if (movement.movementTypeID != lastSeenMovementType) {
	    			allMovementsList.add(movement);
	    			lastSeenMovementType = movement.movementTypeID;
	    		}
	    	}
	    	ArrayList<Movement> relavantMovementsList = new ArrayList<>();
	    	if (startDate != null && endDate != null) {
		    	for (Movement movement : allMovementsList) {
		    		if (movement.movementDate.before(endDate) && movement.movementDate.after(startDate)) {
		    			relavantMovementsList.add(movement);
		    		}
		    	}
	    	} else if (startDate == null) {
	    		if (endDate == null) {
		    		relavantMovementsList = allMovementsList;
	    		} else {
	    	    	for (Movement movement : allMovementsList) {
	    	    		if (movement.movementDate.before(endDate)) {
	    	    			relavantMovementsList.add(movement);
	    	    		}
	    	    	}
	    		}
	    	} else if (endDate == null) {
	    		if (startDate == null) {
		    		relavantMovementsList = allMovementsList;
	    		} else {
	    	    	for (Movement movement : allMovementsList) {
	    	    		if (movement.movementDate.after(startDate)) {
	    	    			relavantMovementsList.add(movement);
	    	    		}
	    	    	}
	    		}
	    	}
	    	
	    	if (relavantMovementsList.size() > 0
	    			&& relavantMovementsList.size() > eventsHolderLayout.getChildCount()) {
	    		LayoutInflater inflater = LayoutInflater.from(getActivity());
	    		for (int i = eventsHolderLayout.getChildCount(); i < relavantMovementsList.size(); i++) {
	    			RelativeLayout eventLayout = (RelativeLayout) inflater.inflate(R.layout.event_breakdown_item, null);
	    			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd h:mm:ss.S a", Locale.US);
	    			((TextView) eventLayout.findViewById(R.id.eventDetailsTextView)).setText(sdf.format(relavantMovementsList.get(i).movementDate.getTime()));
	    			((TextView) eventLayout.findViewById(R.id.eventTitleTextView)).setText(
	    					MovementHistory.getNameFromType(relavantMovementsList.get(i).movementTypeID, getActivity())
	    					/*+" ("+relavantMovementsList.get(i).confidence+")"*/);
	    			int imageID = 0;
	    			if (relavantMovementsList.get(i).movementTypeID == DetectedActivity.IN_VEHICLE) {
	    				imageID = R.drawable.ic_driving;
	    			} else if (relavantMovementsList.get(i).movementTypeID == DetectedActivity.ON_BICYCLE) {
	    				imageID = R.drawable.ic_biking;
	    			} else if (relavantMovementsList.get(i).movementTypeID == DetectedActivity.ON_FOOT) {
	    				imageID = R.drawable.ic_walking;
	    			} else if (relavantMovementsList.get(i).movementTypeID == DetectedActivity.STILL
	    					|| relavantMovementsList.get(i).movementTypeID == DetectedActivity.UNKNOWN) {
	    				imageID = R.drawable.ic_still;
	    			} else if (relavantMovementsList.get(i).movementTypeID == DetectedActivity.TILTING) {
	    				imageID = R.drawable.ic_tilting;
	    			} else if (relavantMovementsList.get(i).movementTypeID == -1) {
	    				imageID = R.drawable.ic_launcher;
	    			}
	    			((ImageView) eventLayout.findViewById(R.id.iconImageView)).setImageResource(imageID);
	    			eventsHolderLayout.addView(eventLayout, 0);
	    		}
	    	} else if (relavantMovementsList.size() < eventsHolderLayout.getChildCount()) {
	    		eventsHolderLayout.removeAllViews();
	    		updateEventHistory();
	    	}
    	}
    }
}
