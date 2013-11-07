package com.droidplanner.drone;

import com.droidplanner.drone.variables.waypoint;
import com.droidplanner.parameters.Parameter;

import java.util.List;

public class DroneInterfaces {
	public interface MapUpdatedListner {
		public void onDroneUpdate();
	}

	public interface MapConfigListener {
		public void onMapTypeChanged();
	}

	public interface DroneTypeListner {
		public void onDroneTypeChanged();
	}

	public interface HudUpdatedListner {
		public void onDroneUpdate();
	}

	public interface ModeChangedListener {
		public void onModeChanged();
	}

	public interface OnParameterManagerListner {
		public void onBeginReceivingParameters();
		public void onParameterReceived(Parameter parameter, int index, int count);
		public void onEndReceivingParameters(List<Parameter> parameter);
        public void onParamterMetaDataChanged();
	}

	public interface OnWaypointManagerListener {
		public void onBeginReceivingWaypoints();
		public void onWaypointReceived(waypoint wp, int index, int count);
		public void onEndReceivingWaypoints(List<waypoint> waypoints);
	}
}