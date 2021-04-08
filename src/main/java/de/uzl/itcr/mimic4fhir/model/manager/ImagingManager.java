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
package de.uzl.itcr.mimic4fhir.model.manager;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.model.MDiagnosticReport;
import de.uzl.itcr.mimic4fhir.model.MImagingStudy;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ImagingManager extends ModelManager<ImagingStudy, MImagingStudy> {

	public ImagingManager() {
		super();
	}

	@Override
	public ImagingStudy createResource(MImagingStudy mImStudy, Config config) {
		if (mImStudy.getStudy() != null) {
			return mImStudy.getStudy();
		} else {
			ImagingStudy iStudy = new ImagingStudy();

			// Add ID
			iStudy.setId(mImStudy.getStudyId());

			// Set status; unknown, registered or available?
			iStudy.setStatus(ImagingStudy.ImagingStudyStatus.UNKNOWN);

			// Add patient reference
			iStudy.setSubject(new Reference(mImStudy.getSubjectId()));

			// TODO: Add encounter reference

			// Add endpoint reference
			iStudy.addEndpoint(new Reference(this.createEndpoint(mImStudy).getId()));

			// Add series
			iStudy.addSeries(new ImagingStudy.ImagingStudySeriesComponent().setUid(mImStudy.getDicomId()));

			return iStudy;
		}
	}

	public Endpoint createEndpoint(MImagingStudy mImStudy) {
		if (mImStudy.getEndpoint() != null) {
			return mImStudy.getEndpoint();
		} else {
			Endpoint ePoint = new Endpoint();

			// Set endpoint ID
			ePoint.setId(IdDt.newRandomUuid());

			// Set endpoint status
			ePoint.setStatus(Endpoint.EndpointStatus.ACTIVE);

			// Set connection type
			ePoint.setConnectionType(new Coding().setSystem("http://hl7.org/fhir/ValueSet/endpoint-connection-type")
					.setCode(
							"https://www.hl7.org/fhir/codesystem-endpoint-connection-type.html#endpoint-connection-type-dicom-wado-uri")
					.setDisplay("DICOM WADO-URI"));

			// Add payload type
			ePoint.addPayloadType(
					new CodeableConcept(new Coding().setSystem("http://ihe-d.de/CodeSystems/IHEXDStypeCode")
							.setCode("BILD").setDisplay("Ergebnisse bildgebender Diagnostik")));

			// Add address (in this case it will be the file path)
			ePoint.setAddress(mImStudy.getPath());

			mImStudy.setEndpoint(ePoint);

			return ePoint;
		}
	}

	public DiagnosticReport createDiagnosticReport(MDiagnosticReport mDiaReport, Config config) {
		if (mDiaReport.getDiagnosticReport() != null) {
			return mDiaReport.getDiagnosticReport();
		} else {
			DiagnosticReport dReport = new DiagnosticReport();

			// Add ID
			dReport.setId(IdDt.newRandomUuid());

			// Add subject ID
			dReport.setSubject(new Reference(mDiaReport.getSubjectId()));

			// Add status; It can considered 'final' for all reports
			dReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

			// Add imaging studies
			List<Reference> references = new ArrayList<>();
			for (MImagingStudy study : mDiaReport.getStudies()) {
				references.add(new Reference(this.createResource(study, config).getId()));
			}
			dReport.setImagingStudy(references);

			// Add a conclusion to the report
			dReport.setConclusion(mDiaReport.getReport());

			mDiaReport.setDiagnosticReport(dReport);

			return dReport;
		}
	}

}
