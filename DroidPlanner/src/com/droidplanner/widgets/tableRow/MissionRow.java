package com.droidplanner.widgets.tableRow;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.droidplanner.drone.variables.waypoint;

import com.droidplanner.R;

public class MissionRow extends ArrayAdapter<waypoint> {

	private Context context;
	private List<waypoint> waypoints;

	private TextView nameView;
	private TextView altitudeView;
	private TextView typeView;
	private TextView delayView;
	private String tmpStr;

	public MissionRow(Context context, int resource, List<waypoint> objects) {
		super(context, resource, objects);
		this.waypoints = objects;
		this.context = context;
	}

	public MissionRow(Context context, int resource) {
		super(context, resource);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createRowViews(parent, position);
	}

	private View createRowViews(ViewGroup root, int position) {
		waypoint waypoint = waypoints.get(position);
		View view = createLayoutFromResource();
		findViewObjects(view);		
		setupViewsText(waypoint);
		return view;
	}

	private View createLayoutFromResource() {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.row_mission_list, null);
		return view;
	}

	private void findViewObjects(View view) {
		nameView = (TextView) view.findViewById(R.id.rowNameView);
		altitudeView = (TextView) view.findViewById(R.id.rowAltitudeView);
		typeView = (TextView) view.findViewById(R.id.rowTypeView);
		delayView = (TextView) view.findViewById(R.id.rowDelayView);
	}

	private void setupViewsText(waypoint waypoint) {
		
		nameView.setText(String.format("%3d", waypoint
				.getNumber()));

		tmpStr = "";
		if (waypoint.getCmd().isNavigation()) {
			tmpStr = String.format(Locale.ENGLISH, "%3.0fm", waypoint.getHeight());
		} 
		else {
			tmpStr = "-";
		}
		altitudeView.setText(tmpStr);
		
		tmpStr="";
		
		tmpStr = waypoint.getCmd().getName()+"\n";

		if(waypoint.getCmd().getName()=="Loiter") {	
			tmpStr += "° turns";
		}
		else if (waypoint.getCmd().getName()=="LoiterN") {
		
			tmpStr += String.format(Locale.ENGLISH, "%3.0f", waypoint.missionItem.param1);
			if(waypoint.missionItem.param1<0)
				tmpStr += "CCW";
			else
				tmpStr += "CW";
		}
		else if (waypoint.getCmd().getName()=="LoiterT") {
			tmpStr += String.format(Locale.ENGLISH, "%3.1fs", waypoint.missionItem.param1);
		}
		else if (waypoint.getCmd().getName()=="Takeoff") {
			tmpStr += String.format(Locale.ENGLISH, "%3.1f¡", waypoint.missionItem.param1);
		}
		
		typeView.setText(tmpStr);

		
		delayView.setText(String.format(Locale.ENGLISH, "%3.1fs", waypoint.missionItem.param2));
	}

}
