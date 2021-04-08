/*******************************************************************************
 * Copyright (C) 2021 S. Ververs, P. Behrend, J. Wiedekopf, H.Ulrich - University of Lübeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.uzl.itcr.mimic4fhir.concur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import de.uzl.itcr.mimic4fhir.model.MPatient;
import de.uzl.itcr.mimic4fhir.model.manager.StationManager;
import de.uzl.itcr.mimic4fhir.work.Config;
import de.uzl.itcr.mimic4fhir.work.ConnectDB;
import de.uzl.itcr.mimic4fhir.work.FHIRComm;

public class ConversionThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ConversionThread.class);

	// TransformerHelper helper
	ConnectDB connectDB;
	FHIRComm fhir;
	private MPatient mPat;
	private String patientId;
	private int mPatNumber;
	private StationManager stations;
	private boolean validate;
	private Config config;

	/*
	 * public ConversionThread(FHIRComm fhir, MPatient mPat, int mPatNumber,
	 * MStations stations, boolean validate) { this.fhir = fhir; this.mPat = mPat;
	 * this.mPatNumber = mPatNumber; this.stations = stations; this.validate =
	 * validate; }
	 */

	public ConversionThread(FHIRComm fhirComm, String patientID, int mPatNumber, StationManager stations, Config config,
			boolean validateResources, ConnectDB dbAccess) {
		this.fhir = fhirComm;
		this.patientId = patientID;
		this.mPatNumber = mPatNumber;
		this.stations = stations;
		this.validate = validateResources;
		this.connectDB = dbAccess;
		this.config = config;
	}

	public void test() {
		loadData();
		FHIRTransformer transformer = new FHIRTransformer(fhir, config, validate);
		transformer.processPatient(mPat, mPatNumber, stations);
	}

	@Override
	public void run() {
		StopWatch watch = new StopWatch();
		watch.start();
		logger.info("[{}] - Pat. {} - Query", this.mPatNumber, this.patientId);
		loadData();
		logger.info("[{}] - Pat. {} - Convert", this.mPatNumber, this.patientId);
		FHIRTransformer transformer = new FHIRTransformer(fhir, config, validate);
		transformer.processPatient(mPat, mPatNumber, stations);
		watch.stop();
		logger.info("[{}] - Pat. {} - Done in {} ms", this.mPatNumber, this.patientId, watch.getTotalTimeMillis());
		TimeMeasurements.getInstance().addTiming(this.mPatNumber, this.patientId, this.mPat.getAdmissions().size(),
				watch.getTotalTimeMillis());
	}

	private void loadData() {
		this.mPat = connectDB.getPatientBySubjectIdSynchronized(this.patientId);
	}
}
