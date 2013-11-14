package com.droidplanner.activitys;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.droidplanner.DroidPlannerApp.OnWaypointUpdateListner;
import com.droidplanner.R;
import com.droidplanner.activitys.helpers.SuperActivity;
import com.droidplanner.dialogs.AltitudeDialog.OnAltitudeChangedListner;
import com.droidplanner.dialogs.openfile.OpenFileDialog;
import com.droidplanner.dialogs.openfile.OpenMissionDialog;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerReadListener;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerVerifyListener;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerWriteListener;
import com.droidplanner.drone.variables.waypoint;
import com.droidplanner.file.IO.MissionReader;
import com.droidplanner.file.IO.MissionWriter;
import com.droidplanner.fragments.MissionFragment;
import com.droidplanner.fragments.PlanningMapFragment;
import com.droidplanner.fragments.helpers.GestureMapFragment;
import com.droidplanner.fragments.helpers.GestureMapFragment.OnPathFinishedListner;
import com.droidplanner.fragments.helpers.MapProjection;
import com.droidplanner.fragments.helpers.OnMapInteractionListener;
import com.droidplanner.fragments.survey.SurveyFragment;
import com.droidplanner.fragments.survey.SurveyFragment.OnNewGridListner;
import com.droidplanner.helpers.geoTools.PolylineTools;
import com.droidplanner.helpers.units.Length;
import com.droidplanner.polygon.Polygon;
import com.droidplanner.polygon.PolygonPoint;
import com.droidplanner.survey.grid.Grid;
import com.google.android.gms.maps.model.LatLng;

public class PlanningActivity extends SuperActivity implements
		OnMapInteractionListener, OnWaypointUpdateListner,
		OnAltitudeChangedListner, OnPathFinishedListner, OnNewGridListner,
		OnWaypointManagerVerifyListener, OnWaypointManagerWriteListener,
		OnWaypointManagerReadListener, OnDismissListener {

	public Polygon polygon;
	private PlanningMapFragment planningMapFragment;
	private MissionFragment missionFragment;
	private GestureMapFragment gestureMapFragment;
	private TextView lengthView;
	private SurveyFragment surveyFragment;
	private ProgressDialog pd;
	private List<waypoint> tempWps;

	@Override
	public int getNavigationItem() {
		return 0;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_planning);

		planningMapFragment = ((PlanningMapFragment) getFragmentManager()
				.findFragmentById(R.id.planningMapFragment));
		gestureMapFragment = ((GestureMapFragment) getFragmentManager()
				.findFragmentById(R.id.gestureMapFragment));
		missionFragment = (MissionFragment) getFragmentManager()
				.findFragmentById(R.id.missionFragment);
		surveyFragment = (SurveyFragment) getFragmentManager()
				.findFragmentById(R.id.surveyFragment);

		lengthView = (TextView) findViewById(R.id.textViewTotalLength);

		polygon = new Polygon();

		gestureMapFragment.setOnPathFinishedListner(this);
		missionFragment.setMission(drone.mission);
		planningMapFragment.setMission(drone.mission);
		surveyFragment.setSurveyData(polygon, drone.mission.getDefaultAlt());
		surveyFragment.setOnSurveyListner(this);

		drone.mission.missionListner = this;

		checkIntent();

		update();
	}

	private void checkIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		if (Intent.ACTION_VIEW.equals(action) && type != null) {
			Toast.makeText(this, intent.getData().getPath(), Toast.LENGTH_LONG)
					.show();
			openMission(intent.getData().getPath());
			update();
			zoom();
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_planning, menu);
		getMenuInflater().inflate(R.menu.menu_map_type, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_zoom:
			zoom();
			return true;
		case R.id.menu_save_file:
			menuSaveFile();
			return true;
		case R.id.menu_open_file:
			openMissionFile();
			return true;
		case R.id.menu_send_to_apm:
			sendMissionToAPM();
			return true;
		case R.id.menu_clear_wp:
			clearWaypointsAndUpdate();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	private void sendMissionToAPM() {
		drone.waypointMananger.setOnWaypointManagerReadListener(this);
		drone.waypointMananger.setOnWaypointManagerWriteListener(this);
		drone.waypointMananger.setOnWaypointManagerVerifyListener(this);
		drone.mission.sendMissionToAPM();
	}

	private void zoom() {
		planningMapFragment.zoomToExtents(drone.mission
				.getAllVisibleCoordinates());
	}

	private void processReceivedPoints(List<LatLng> points) {
		switch (surveyFragment.getPathToDraw()) {
		case MISSION:
			drone.mission.addWaypointsWithDefaultAltitude(points);
			break;
		case POLYGON:
			polygon.addPoints(points);
			surveyFragment.generateGrid();
			break;
		default:
			break;
		}
		update();
	}

	private void clearWaypointsAndUpdate() {
		drone.mission.clearWaypoints();
		update();
	}

	private void update() {
		planningMapFragment.update(polygon);
		missionFragment.update();
		updateDistanceView();
	}

	private void updateDistanceView() {
		Length length = PolylineTools.getPolylineLength(drone.mission
				.getPathPoints());
		lengthView.setText(getString(R.string.length) + ": " + length);
	}

	@Override
	public void onMapClick(LatLng point) {
		Toast.makeText(this, "Draw your path", Toast.LENGTH_SHORT).show();
		gestureMapFragment.enableGestureDetection();
	}

	@Override
	public void onAddPoint(LatLng point) {
		List<LatLng> points = new ArrayList<LatLng>();
		points.add(point);
		processReceivedPoints(points);
	}

	@Override
	public void onAltitudeChanged(double newAltitude) {
		super.onAltitudeChanged(newAltitude);
		update();
	}

	@Override
	public void onMoveHome(LatLng coord) {
		drone.mission.setHome(coord);
		update();
	}

	@Override
	public void onMoveWaypoint(waypoint source, LatLng latLng) {
		source.setCoord(latLng);
		update();
	}

	@Override
	public void onMovingWaypoint(waypoint source, LatLng latLng) {
		updateDistanceView();
		missionFragment.update();
	}

	@Override
	public void onMovePolygonPoint(PolygonPoint source, LatLng newCoord) {
		source.coord = newCoord;
		update();
	}

	@Override
	public void onWaypointsUpdate() {
		update();
		zoom();
	}

	@Override
	public void onPathFinished(List<Point> path) {
		List<LatLng> points = MapProjection.projectPathIntoMap(path,
				planningMapFragment.mMap);
		processReceivedPoints(points);
	}

	private void openMission(String path) {
		MissionReader missionReader = new MissionReader();
		if (missionReader.openMission(path)) {
			drone.mission.setHome(missionReader.getHome());
			drone.mission.setWaypoints(missionReader.getWaypoints());
		}

	}

	private boolean writeMission() {
		MissionWriter missionWriter = new MissionWriter(
				drone.mission.getHome(), drone.mission.getWaypoints());
		return missionWriter.saveWaypoints();
	}

	private void openMissionFile() {
		OpenFileDialog missionDialog = new OpenMissionDialog(drone) {
			@Override
			public void waypointFileLoaded(MissionReader reader) {
				drone.mission.setHome(reader.getHome());
				drone.mission.setWaypoints(reader.getWaypoints());
				zoom();
				update();
			}
		};
		missionDialog.openDialog(this);
	}

	private void menuSaveFile() {
		if (writeMission()) {
			Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT)
					.show();
		} else {
			Toast.makeText(this, R.string.error_when_saving, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onNewGrid(Grid grid) {
		drone.mission.setWaypoints(grid.getWaypoints());
		planningMapFragment.cameraOverlays.removeAll();
		if (surveyFragment.isFootPrintOverlayEnabled()) {
			planningMapFragment.cameraOverlays.addOverlays(
					grid.getCameraLocations(), surveyFragment.getSurveyData());
		}
		update();
	}

	@Override
	public void onClearPolygon() {
		polygon.clearPolygon();
		update();
	}

	private ProgressDialog getProgressDialog(boolean reCreate) {
		if (pd != null && reCreate) {
			if (pd.isShowing())
				pd.dismiss();
			else
				pd = null;

		} else if (pd != null) {
			return pd;
		}

		pd = new ProgressDialog(this);
		pd.setIcon(R.drawable.ic_launcher);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setIndeterminate(true);
		pd.setCancelable(false);
		pd.setCanceledOnTouchOutside(true);
		pd.setOnDismissListener(this);

		return pd;
	}

	@Override
	public void onBeginUploadingWaypoints() {
		tempWps = new ArrayList<waypoint>();

		pd = getProgressDialog(true);
		pd.setTitle(R.string.mission_uploading);
		pd.show();
	}

	@Override
	public void onWaypointUploaded(waypoint wp, int index, int count) {
		tempWps.add(wp);
		if (pd != null) {
			pd.setTitle(R.string.mission_uploading);
			if (pd.isIndeterminate()) {
				pd.setIndeterminate(false);
				pd.setMax(count);
			}
			pd.setProgress(index);
		}
	}

	@Override
	public void onEndUploadingWaypoints(List<waypoint> waypoints) {
		drone.waypointMananger.getWaypoints();
	}

	@Override
	public void onBeginReceivingWaypoints() {
		pd = getProgressDialog(false);
		pd.setTitle(R.string.mission_verifying_1);
		if (!pd.isShowing())
			pd.show();
	}

	@Override
	public void onWaypointReceived(waypoint wp, int index, int count) {
		if (pd != null) {
			String title = getString(R.string.mission_verifying_1);
			pd.setTitle(title + " 1/2");
			if (pd.isIndeterminate()) {
				pd.setIndeterminate(false);
				pd.setMax(count);
			}
			pd.setProgress(index+1);
		}
	}

	@Override
	public void onEndReceivingWaypoints(final List<waypoint> waypoints) {
		Log.d("TAG", "start verify - " + String.valueOf(waypoints.size())
				+ " / " + String.valueOf(tempWps.size()));
		drone.waypointMananger.verifyWaypoints(waypoints, tempWps);
	}

	@Override
	public void onBeginVerifyingWaypoints() {

		pd = getProgressDialog(false);
		pd.setTitle(R.string.mission_verifying_2);
		if (!pd.isShowing())
			pd.show();
	}

	@Override
	public void onWaypointVerified(waypoint wp, final int index, int count) {
		Log.d("TAG",
				"Verifying " + String.valueOf(index) + "/"
						+ String.valueOf(count));
		if (pd != null) {
			String title = getString(R.string.mission_verifying_2);
			pd.setTitle(title + " 2/2");
			if (pd.isIndeterminate()) {
				pd.setIndeterminate(false);
				pd.setMax(count);
			}

			pd.setProgress(index);
		}
	}

	@Override
	public void onEndVerifyingWaypoints(List<waypoint> source,
			List<waypoint> target, int mismatch) {
		// dismiss progress dialog
		if (pd != null) {
			pd.dismiss();
		}

		String msg;
		if (mismatch < 0)
			msg = "Verification Error!!, size mismatch";
		else if (mismatch > 0)
			msg = String.valueOf(mismatch) + " data mismatch found";
		else
			msg = "Verification completed";

		Toast toast = Toast.makeText(drone.context, msg, Toast.LENGTH_SHORT);
		TextView v = (TextView) toast.getView().findViewById(
				android.R.id.message);
		v.setTextColor(mismatch != 0 ? Color.RED : Color.GREEN);
		drone.tts.speak(msg);

		tempWps.clear();
		tempWps = null;
		Log.d("TAG", "End");

	}

	@Override
	public void onVerifyError(waypoint src, waypoint tgt, int index) {
		Toast.makeText(drone.context, "Waypoints received from Drone",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDismiss(DialogInterface arg0) {
		if (pd != null) {
			pd = null;
		}
	}

}
