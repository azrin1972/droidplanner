package com.droidplanner.drone.variables;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_mission_ack;
import com.MAVLink.Messages.ardupilotmega.msg_mission_count;
import com.MAVLink.Messages.ardupilotmega.msg_mission_current;
import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.ardupilotmega.msg_mission_item_reached;
import com.MAVLink.Messages.ardupilotmega.msg_mission_request;
import com.droidplanner.MAVLink.MavLinkWaypoint;
import com.droidplanner.drone.Drone;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerReadListener;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerVerifyListener;
import com.droidplanner.drone.DroneInterfaces.OnWaypointManagerWriteListener;
import com.droidplanner.drone.DroneVariable;

/**
 * Class to manage the communication of waypoints to the MAV.
 * 
 * Should be initialized with a MAVLink Object, so the manager can send messages
 * via the MAV link. The function processMessage must be called with every new
 * MAV Message.
 * 
 */
public class WaypointMananger extends DroneVariable {

	private OnWaypointManagerReadListener readListener;
	private OnWaypointManagerWriteListener writeListener;
	private OnWaypointManagerVerifyListener verifyListener;

	/**
	 * Try to receive all waypoints from the MAV.
	 * 
	 * If all runs well the callback will return the list of waypoints.
	 */
	public void getWaypoints() {
		state = waypointStates.READ_REQUEST;
		MavLinkWaypoint.requestWaypointsList(myDrone);
	}

	/**
	 * Write a list of waypoints to the MAV.
	 * 
	 * The callback will return the status of this operation
	 * 
	 * @param data
	 *            waypoints to be written
	 */
	public void verifyWaypoints(final List<waypoint> source,
			final List<waypoint> target) {
		final Handler handler = new Handler();
		
		if (source.size() != target.size()) {
			doEndWaypointVerifying(source, target, -1);
		}

		new Thread(new Runnable() {
			public void run() {
				int mismatch = 0;
				doBeginWaypointVerifying();

				for (int i = 0; i < source.size(); i++) {
					final waypoint src, tgt;
					final int idx = i;
					src = source.get(i);
					tgt = target.get(i);

					if (!compareWaypoint(src, tgt)) {
						mismatch++;
						handler.post(new Runnable(){
							public void run(){
								doWaypointVerifyError(src, tgt, idx);								
							}
						});
					}
					handler.post(new Runnable(){
						public void run(){
							doWaypointVerified(src, idx, source.size());
						}
					});
				}
				doEndWaypointVerifying(source, target, mismatch);
			}
		}).start();
	}

	private boolean compareWaypoint(waypoint src, waypoint tgt) {
		return (src.homeType == tgt.homeType
				&& src.missionItem.autocontinue == tgt.missionItem.autocontinue
				&& src.missionItem.command == tgt.missionItem.command
				&& src.missionItem.compid == tgt.missionItem.compid
				&& src.missionItem.msgid == tgt.missionItem.msgid
				&& src.missionItem.sysid == tgt.missionItem.sysid
				&& src.missionItem.param1 == tgt.missionItem.param1
				&& src.missionItem.param2 == tgt.missionItem.param2
				&& src.missionItem.param3 == tgt.missionItem.param3
				&& src.missionItem.param4 == tgt.missionItem.param4
				&& src.missionItem.x == tgt.missionItem.x
				&& src.missionItem.y == tgt.missionItem.y
				&& src.missionItem.z == tgt.missionItem.z
				&& src.missionItem.target_component == tgt.missionItem.target_component && src.missionItem.target_system == tgt.missionItem.target_system);
	}

	public void writeWaypoints(List<waypoint> data) {
		if ((waypoints != null)) {
			waypoints.clear();
			waypoints.addAll(data);
			writeIndex = 0;
			state = waypointStates.WRITTING_WP;
			doBeginWaypointUploading();
			MavLinkWaypoint.sendWaypointCount(myDrone, waypoints.size());
		}
	}

	/**
	 * Sets the current waypoint in the MAV
	 * 
	 * The callback will return the status of this operation
	 */
	public void setCurrentWaypoint(int i) {
		if ((waypoints != null)) {
			MavLinkWaypoint.sendSetCurrentWaypoint(myDrone, (short) i);
		}
	}

	/**
	 * Callback for when a waypoint has been reached
	 * 
	 * @param wpNumber
	 *            number of the completed waypoint
	 */
	public void onWaypointReached(int wpNumber) {
	}

	/**
	 * Callback for a change in the current waypoint the MAV is heading for
	 * 
	 * @param seq
	 *            number of the updated waypoint
	 */
	private void onCurrentWaypointUpdate(short seq) {
	}

	/**
	 * number of waypoints to be received, used when reading waypoints
	 */
	private short waypointCount;
	/**
	 * list of waypoints used when writing or receiving
	 */
	private List<waypoint> waypoints = new ArrayList<waypoint>();
	/**
	 * waypoint witch is currently being written
	 */
	private int writeIndex;

	enum waypointStates {
		IDLE, READ_REQUEST, READING_WP, WRITTING_WP, WAITING_WRITE_ACK
	}

	waypointStates state = waypointStates.IDLE;

	public WaypointMananger(Drone drone) {
		super(drone);
	}

	/**
	 * Try to process a Mavlink message if it is a mission related message
	 * 
	 * @param msg
	 *            Mavlink message to process
	 * @return Returns true if the message has been processed
	 */
	public boolean processMessage(MAVLinkMessage msg) {
		switch (state) {
		default:
		case IDLE:
			break;
		case READ_REQUEST:
			if (msg.msgid == msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT) {
				waypointCount = ((msg_mission_count) msg).count;
				doBeginWaypointReceiving();
				waypoints.clear();
				MavLinkWaypoint.requestWayPoint(myDrone, waypoints.size());
				state = waypointStates.READING_WP;
				return true;
			}
			break;
		case READING_WP:
			if (msg.msgid == msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM) {
				processReceivedWaypoint((msg_mission_item) msg);
				doWaypointReceived(waypoints.get(waypoints.size() - 1),
						waypoints.size(), waypointCount);

				if (waypoints.size() < waypointCount) {
					MavLinkWaypoint.requestWayPoint(myDrone, waypoints.size());
				} else {
					state = waypointStates.IDLE;
					MavLinkWaypoint.sendAck(myDrone);
					doEndWaypointReceiving(waypoints);
					myDrone.mission.onWaypointsReceived(waypoints);
				}
				return true;
			}
			break;
		case WRITTING_WP:
			if (msg.msgid == msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST) {
				MavLinkWaypoint.sendWaypoint(myDrone, writeIndex,
						waypoints.get(writeIndex));
				writeIndex++;
				doWaypointUploaded(waypoints.get(writeIndex - 1), writeIndex,
						waypoints.size());
				if (writeIndex >= waypoints.size()) {
					state = waypointStates.WAITING_WRITE_ACK;
				}
				return true;
			}
			break;
		case WAITING_WRITE_ACK:
			if (msg.msgid == msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK) {
				myDrone.mission.onWriteWaypoints((msg_mission_ack) msg);
				state = waypointStates.IDLE;
				doEndWaypointUploading(waypoints);
				return true;
			}
			break;
		}

		if (msg.msgid == msg_mission_item_reached.MAVLINK_MSG_ID_MISSION_ITEM_REACHED) {
			onWaypointReached(((msg_mission_item_reached) msg).seq);
			return true;
		}
		if (msg.msgid == msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT) {
			onCurrentWaypointUpdate(((msg_mission_current) msg).seq);
			return true;
		}
		return false;
	}

	public void setOnWaypointManagerReadListener(
			OnWaypointManagerReadListener mListener) {
		this.readListener = mListener;
	}

	public void setOnWaypointManagerWriteListener(
			OnWaypointManagerWriteListener mListener) {
		this.writeListener = mListener;
	}

	public void setOnWaypointManagerVerifyListener(
			OnWaypointManagerVerifyListener mListener) {
		this.verifyListener = mListener;
	}

	private void doEndWaypointReceiving(List<waypoint> waypoints) {
		if (readListener != null) {
			readListener.onEndReceivingWaypoints(waypoints);
		}
	}

	private void doBeginWaypointReceiving() {
		if (readListener != null) {
			readListener.onBeginReceivingWaypoints();
		}
	}

	private void doWaypointReceived(waypoint wp, int index, int count) {
		if (readListener != null) {
			readListener.onWaypointReceived(wp, index, count);
		}
	}

	private void doEndWaypointUploading(List<waypoint> waypoints) {
		if (writeListener != null) {
			writeListener.onEndUploadingWaypoints(waypoints);
		}
	}

	private void doBeginWaypointUploading() {
		if (writeListener != null) {
			writeListener.onBeginUploadingWaypoints();
		}
	}

	private void doWaypointUploaded(waypoint wp, int index, int count) {
		if (writeListener != null) {
			writeListener.onWaypointUploaded(wp, index, count);
		}
	}

	private void doEndWaypointVerifying(List<waypoint> source,
			List<waypoint> target, int mismatch) {
		if (verifyListener != null) {
			verifyListener.onEndVerifyingWaypoints(source, target, mismatch);
		}
	}

	private void doBeginWaypointVerifying() {
		if (verifyListener != null) {
			verifyListener.onBeginVerifyingWaypoints();
		}
	}

	private void doWaypointVerified(waypoint wp, int index, int count) {
		if (verifyListener != null) {
			verifyListener.onWaypointVerified(wp, index, count);
		}
	}

	private void doWaypointVerifyError(waypoint src, waypoint tgt, int index) {
		if (verifyListener != null) {
			verifyListener.onVerifyError(src, tgt, index);
		}
	}

	private void processReceivedWaypoint(msg_mission_item msg) {
		waypoints.add(new waypoint(msg));
	}
}
