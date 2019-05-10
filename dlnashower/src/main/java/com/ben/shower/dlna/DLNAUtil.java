package com.ben.shower.dlna;

final class DLNAUtil {
	private static final String URN = "urn:";
	private static final String DEFAULT_NAMESPACE = "schemas-upnp-org";

	public static final String TYPE_MediaRenderer = "MediaRenderer";
	public static final String TYPE_MediaServer = "MediaServer";

	public static final String SERVICENAME_AVTransport = "AVTransport";
	public static final String SERVICENAME_RenderingControl = "RenderingControl";
	public static final String SERVICENAME_ContentDirectory = "ContentDirectory";

	/** actions **/
	/** renderer actions **/
	public static final String ACTION_GetMediaInfo = "GetMediaInfo";
	public static final String ACTION_GetTransportInfo = "GetTransportInfo";
	public static final String ACTION_GetPositionInfo = "GetPositionInfo";
	public static final String ACTION_SetAVTransportURI = "SetAVTransportURI";
	public static final String ACTION_Stop = "Stop";
	public static final String ACTION_Play = "Play";
	public static final String ACTION_Pause = "Pause";
	public static final String ACTION_Seek = "Seek";
	public static final String ACTION_GetVolume = "GetVolume";
	public static final String ACTION_SetVolume = "SetVolume";

	/** server actions **/
	public static final String ACTION_GetSysUpdateID = "GetSystemUpdateID";
	public static final String ACTION_Browse = "Browse";
	public static final String ACTION_Search = "Search";
	/** actions end **/

	public static final String VALUE_CHANNEL_MASTER = "Master";
	public static final String VALUE_REL_TIME = "REL_TIME";

	public static final String IN_INSTANCEID = "InstanceID";
	public static final String IN_TRANSPORT_URI = "CurrentURI";
	public static final String IN_TRANSPORT_METADATA = "CurrentURIMetaData";
	public static final String IN_PLAY_SPEED = "Speed";
	public static final String IN_SEEK_UNIT = "Unit";
	public static final String IN_SEEK_TARGET = "Target";
	public static final String IN_VOLUME_CHANNEL = "Channel";
	public static final String IN_VOLUME_VALUE = "DesiredVolume";

	public static final String IN_CONTAINERID = "ContainerID";
	public static final String IN_SEARCHCRITERIA = "SearchCriteria";
	public static final String IN_OBJECTID = "ObjectID";
	public static final String IN_BROWSEFLAG = "BrowseFlag";
	public static final String IN_FILTER = "Filter";
	public static final String IN_STARTINGINDEX = "StartingIndex";
	public static final String IN_REQUESTEDCOUNT = "RequestedCount";
	public static final String IN_SORTCRITERIA = "SortCriteria";

	public static final String OUT_STATE = "CurrentTransportState";
	public static final String OUT_CURRENT_URI = "CurrentURI";
	public static final String OUT_CURRENT_METADATA = "CurrentURIMetaData";
	public static final String OUT_MEDIA_DURATION = "MediaDuration";
	public static final String OUT_TRACK_DURATION = "TrackDuration";
	public static final String OUT_TRACK_POSITION = "RelTime";
	public static final String OUT_VOLUME = "CurrentVolume";

	public static final String OUT_RESULT = "Result";
	public static final String OUT_NUMBERRETURNED = "NumberReturned";
	public static final String OUT_TOTALMATCHES = "TotalMatches";
	public static final String OUT_UPDATEID = "UpdateID";

	public static final String OUT_ID = "Id";

	public static final String ATTR_VALUE = "val";

	public static final String VAR_INSTANCEID = "InstanceID";
	public static final String VAR_STATE = "TransportState";
	public static final String VAR_TRANSPORT_URI = "AVTransportURI";
	public static final String VAR_CURRENT_URI = "CurrentTrackURI";
	public static final String VAR_PLAY_SPEED = "TransportPlaySpeed";
	public static final String VAR_TRACK_DURATION = "CurrentTrackDuration";
	public static final String VAR_TRACK_POSITION = "RelativeTimePosition";
	public static final String VAR_VOLUME = "Volume";

	public static final String VAR_SYS_UPDATEID = "SystemUpdateID";

	public static String fullServiceType(String type, int version) {
		return URN + DEFAULT_NAMESPACE + ":service:" + type + ':' + version;
	}

	public static String fullServiceType(String type) {
		return fullServiceType(type, 1);
	}

	public static String fullDeviceType(String type, int version) {
		return URN + DEFAULT_NAMESPACE + ":device:" + type + ':' + version;
	}

	public static String fullDeviceType(String type) {
		return fullDeviceType(type, 1);
	}

	public static <T> T buildGetMediaInfoParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId) {
		return setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
	}

	public static <T> T buildTransportInfoParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId) {
		return setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
	}

	public static <T> T buildGetPositionInfoParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId) {
		return setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
	}

	public static <T> T buildSetAVTransportURIParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId,
			String uri, String metaData) {
		target = setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
		target = setter.putKeyValue(target, IN_TRANSPORT_URI, uri);
		target = setter.putKeyValue(target, IN_TRANSPORT_METADATA, metaData);
		return target;
	}

	public static <T> T buildStopParams(IKeyValueSet<T, String, String> setter,
			T target, int InstanceId) {
		return setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
	}

	public static <T> T buildPlayParams(IKeyValueSet<T, String, String> setter,
			T target, int InstanceId, int speed) {
		target = setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
		target = setter.putKeyValue(target, IN_PLAY_SPEED,
				String.valueOf(speed));
		return target;
	}

	public static <T> T buildPauseParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId) {
		return setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
	}

	public static <T> T buildSeekParams(IKeyValueSet<T, String, String> setter,
			T target, int InstanceId, String unit, String relTime) {
		target = setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
		target = setter.putKeyValue(target, IN_SEEK_UNIT, unit);
		target = setter.putKeyValue(target, IN_SEEK_TARGET, relTime);
		return target;
	}

	public static <T> T buildGetVolumeParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId,
			String Channel) {
		target = setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
		target = setter.putKeyValue(target, IN_VOLUME_CHANNEL, Channel);
		return target;
	}

	public static <T> T buildSetVolumeParams(
			IKeyValueSet<T, String, String> setter, T target, int InstanceId,
			String Channel, int volume) {
		target = setter.putKeyValue(target, IN_INSTANCEID,
				String.valueOf(InstanceId));
		target = setter.putKeyValue(target, IN_VOLUME_CHANNEL, Channel);
		target = setter.putKeyValue(target, IN_VOLUME_VALUE,
				String.valueOf(volume));
		return target;
	}

	public static <T> T buildBrowseParams(
			IKeyValueSet<T, String, String> setter, T target, String objectID,
			String browseFlag, String filter, int start, int count,
			String sortCriteria) {
		target = setter.putKeyValue(target, IN_OBJECTID, objectID);
		target = setter.putKeyValue(target, IN_BROWSEFLAG, browseFlag);
		target = setter.putKeyValue(target, IN_FILTER, filter);
		target = setter.putKeyValue(target, IN_STARTINGINDEX,
				String.valueOf(start));
		target = setter.putKeyValue(target, IN_REQUESTEDCOUNT,
				String.valueOf(count));
		target = setter.putKeyValue(target, IN_SORTCRITERIA, sortCriteria);
		return target;
	}

	public static <T> T buildSearchParams(
			IKeyValueSet<T, String, String> setter, T target,
			String containerID, String searchCriteria, String filter,
			int start, int count, String sortCriteria) {
		target = setter.putKeyValue(target, IN_CONTAINERID, containerID);
		target = setter.putKeyValue(target, IN_SEARCHCRITERIA, searchCriteria);
		target = setter.putKeyValue(target, IN_FILTER, filter);
		target = setter.putKeyValue(target, IN_STARTINGINDEX,
				String.valueOf(start));
		target = setter.putKeyValue(target, IN_REQUESTEDCOUNT,
				String.valueOf(count));
		target = setter.putKeyValue(target, IN_SORTCRITERIA, sortCriteria);
		return target;
	}

	public static int parseInt(Object value, int def) {
		try {
			return Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
