package me.retrodaredevil.solarthing.couchdb;

import me.retrodaredevil.couchdbjava.request.ViewQuery;
import me.retrodaredevil.couchdbjava.request.ViewQueryParams;
import me.retrodaredevil.solarthing.annotations.UtilityClass;

@UtilityClass
public class SolarThingCouchDb {
	private SolarThingCouchDb() { throw new UnsupportedOperationException(); }

	public static ViewQuery createMillisView(ViewQueryParams params) {
		return new ViewQuery("packets", "millis", params);
	}
}
