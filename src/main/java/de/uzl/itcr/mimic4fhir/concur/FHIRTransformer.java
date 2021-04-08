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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.model.*;
import de.uzl.itcr.mimic4fhir.model.manager.*;
import de.uzl.itcr.mimic4fhir.queue.Sender;
import de.uzl.itcr.mimic4fhir.tools.FHIRInstanceValidator;
import de.uzl.itcr.mimic4fhir.work.BundleControl;
import de.uzl.itcr.mimic4fhir.work.Config;
import de.uzl.itcr.mimic4fhir.work.FHIRComm;

public class FHIRTransformer {

	private FHIRComm fhir;
	private static FHIRInstanceValidator instanceValidator = FHIRInstanceValidator.getInstance();

	private Sender sendr = new Sender();
	private BundleControl bundleC = new BundleControl();

	private Organization hospital;
	private HashMap<Integer, MCaregiver> caregivers;
	private HashMap<String, String> locationsInBundle;
	private HashMap<String, String> caregiversInBundle;
	private HashMap<String, String> medicationInBundle;

	private boolean validateResources = false;

	private Config config;

	public FHIRTransformer(FHIRComm fhir, Config config, boolean validateResources) {
		this.fhir = fhir;
		this.config = config;
		this.validateResources = validateResources;
	}

	public void processPatient(MPatient mimicPat, int numPat, StationManager stations) {
		// Create Managers
		PatientManager paManager = new PatientManager();
		DiagnoseManager dManager = new DiagnoseManager();
		CharteventManager chManager = new CharteventManager();
		LabeventManager laManager = new LabeventManager();
		ProcedureManager proManager = new ProcedureManager();
		PrescriptionManager preManager = new PrescriptionManager();
		AdmissionManager adManager = new AdmissionManager();
		ImagingManager imManager = new ImagingManager();

		// Fill FHIR-Structure
		Patient fhirPat = paManager.createResource(mimicPat, this.config);
		locationsInBundle = new HashMap<String, String>();
		caregiversInBundle = new HashMap<String, String>();
		medicationInBundle = new HashMap<String, String>();
		hospital = createTopHospital();
		String patNumber;
		int admissionIndex = 0;

		// Add DiagnosticReports for each Patient if available

		if (config.useCXR()) {
			if (mimicPat.getDiagnosticReports().size() > 0) {
				for (MDiagnosticReport mDiagnosticReport : mimicPat.getDiagnosticReports()) {
					bundleC.addResourceToBundle(
							imManager.createDiagnosticReport(mDiagnosticReport, this.config));
				}
				JsonObject message = Json.createObjectBuilder()
						.add("number", numPat + "_" + bundleC.getInternalBundleNumber())
						.add("bundle", fhir.getBundleAsString(bundleC.getTransactionBundle())).build();
				sendr.send(message.toString());
				bundleC.resetBundle();
			}
		}

		// All admissions of one patient
		for (MAdmission admission : mimicPat.getAdmissions()) {

			// First: Load/create fhir resources
			Encounter enc = adManager.createAdmission(admission, stations, this.config);

			// create Conditions per Admission
			List<Condition> conditions = new ArrayList<>();
			for (MDiagnose mDiagnose : admission.getDiagnoses()) {
				conditions.add(dManager.createResource(mDiagnose, this.config));
			}

			// create Procedures per Admission
			List<Procedure> procedures = new ArrayList<>();
			for (MProcedure mProcedure : admission.getProcedures()) {
				procedures.add(proManager.createResource(mProcedure, this.config));
			}

			// create List Of Medication & MedicationAdministrations
			List<Medication> medications = new ArrayList<>();
			List<MedicationAdministration> prescriptions = new ArrayList<>();
			int count = 0;
			for (MPrescription mPrescription : admission.getPrescriptions()) {
				medications.add(preManager.createResource(mPrescription, this.config));
				prescriptions
						.add(preManager.createAdministration(mPrescription, count++, this.config));
			}

			// create Observations per Admission
			List<Observation> obs = new ArrayList<>();
			for (MChartevent mChartevent : admission.getEvents()) {
				obs.add(chManager.createResource(mChartevent, this.config));
			}

			// create Observation from Labevents
			List<Observation> obsLab = new ArrayList<>();
			for (MLabevent mLabevent : admission.getLabEvents()) {
				obsLab.add(laManager.createResource(mLabevent, this.config));
			}

			// No note events in mimiciv currently
			// create Observation from Noteevents
			/*
			 * List<Observation> obsNotes =
			 * admission.createFhirNoteObservationsFromMimic(fhirPat.getId(), enc.getId());
			 */

			// create bundle without observations and medication:
			createBasicBundle(fhirPat, admission, enc, conditions, procedures, stations);

			// Medication only in first bundle of admission
			// Prescriptions
			for (Medication med : medications) {
				String identifier = med.getCode().getCodingFirstRep().getCode();
				if (!medicationInBundle.containsKey(identifier)) {
					bundleC.addUUIDResourceWithConditionToBundle(med,
							"code=" + med.getCode().getCodingFirstRep().getCode());
					medicationInBundle.put(identifier, med.getId());

					if (validateResources) {
						instanceValidator.validateAndPrint(med);
					}
				}
			}

			// ..and MedicationAdministrations (with correct Medication as Reference)
			for (MedicationAdministration madm : prescriptions) {
				String identifier = medications.get(prescriptions.indexOf(madm)).getCode().getCodingFirstRep()
						.getCode();
				String medId = medicationInBundle.get(identifier);
				madm.setMedication(new Reference(medId));
				bundleC.addUUIDResourceToBundle(madm);
				if (validateResources) {
					instanceValidator.validateAndPrint(madm);
				}
			}

			// Identification
			admissionIndex++;
			patNumber = numPat + "_" + admissionIndex;

			// add observations to bundle
			for (Observation o : obs) {
				// check if bundle is full
				checkBundleLimit(patNumber, fhirPat, admission, enc, conditions, procedures, stations);

				// Order important - these reference pat & encounter
				bundleC.addResourceToBundle(o);
				if (validateResources) {
					instanceValidator.validateAndPrint(o);
				}
			}

			for (Observation o : obsLab) {
				// check if bundle is full
				checkBundleLimit(patNumber, fhirPat, admission, enc, conditions, procedures, stations);

				bundleC.addResourceToBundle(o);

				if (validateResources) {
					instanceValidator.validateAndPrint(o);
				}
			}

			// Push bundle to queue
			JsonObject message = Json.createObjectBuilder()
					.add("number", patNumber + "_" + bundleC.getInternalBundleNumber())
					.add("bundle", fhir.getBundleAsString(bundleC.getTransactionBundle())).build();

			sendr.send(message.toString());

			// reset bundle and memory lists
			bundleC.resetBundle();
			resetMemoryLists();
		}
		bundleC.resetInternalBundleNumber();
	}

	private void checkBundleLimit(String numPat, Patient fhirPat, MAdmission admission, Encounter enc,
			List<Condition> conditions, List<Procedure> procedures, StationManager stations) {

		// if bundle exceeds 15000 resources -> start new bundle
		if (bundleC.getNumberOfResources() > 15000) {
			// Push bundle to queue
			JsonObject message = Json.createObjectBuilder()
					.add("number", numPat + "_" + bundleC.getInternalBundleNumber())
					.add("bundle", fhir.getBundleAsString(bundleC.getTransactionBundle())).build();

			sendr.send(message.toString());

			// reset bundle and memory lists
			bundleC.resetBundle();
			resetMemoryLists();
			// reload basic bundle stuff
			createBasicBundle(fhirPat, admission, enc, conditions, procedures, stations);
		}
	}

	private void createBasicBundle(Patient fhirPat, MAdmission admission, Encounter enc, List<Condition> conditions,
			List<Procedure> procedures, StationManager stations) {

		// Pat to bundle
		bundleC.addUUIDResourceWithConditionToBundle(fhirPat, "identifier="
				+ fhirPat.getIdentifierFirstRep().getSystem() + "|" + fhirPat.getIdentifierFirstRep().getValue());

		// Top of all: Hospital
		bundleC.addUUIDResourceWithConditionToBundle(hospital, "identifier="
				+ hospital.getIdentifierFirstRep().getSystem() + "|" + hospital.getIdentifierFirstRep().getValue());

		enc.getDiagnosis().clear(); // clear all procedures & diagnoses

		if (validateResources) {
			instanceValidator.validateAndPrint(fhirPat);
			instanceValidator.validateAndPrint(hospital);
		}

		// Diagnoses
		for (Condition c : conditions) {
			int rank = admission.getDiagnoses().get(conditions.indexOf(c)).getSeqNumber();

			// set Condition in enc.diagnosis
			enc.addDiagnosis().setCondition(new Reference(c.getId())).setRank(rank);

			// add Condition to bundle
			bundleC.addUUIDResourceWithConditionToBundle(c,
					"identifier=" + c.getIdentifierFirstRep().getSystem() + "|" + c.getIdentifierFirstRep().getValue());

			if (validateResources) {
				instanceValidator.validateAndPrint(c);
			}
		}

		// Procedures
		for (Procedure p : procedures) {
			int rank = admission.getProcedures().get(procedures.indexOf(p)).getSeqNumber();

			// set Procedure in enc.diagnosis
			enc.addDiagnosis().setCondition(new Reference(p.getId())).setRank(rank);

			// add Procedure to bundle
			bundleC.addUUIDResourceWithConditionToBundle(p,
					"identifier=" + p.getIdentifierFirstRep().getSystem() + "|" + p.getIdentifierFirstRep().getValue());

			if (validateResources) {
				instanceValidator.validateAndPrint(p);
			}
		}

		// create transfer chain

		enc.getLocation().clear(); // clear all locations -> to be newly added

		for (MTransfer t : admission.getTransfers()) {
			Location locWard = stations.getLocation(stations.getStation(t.getCareUnit()));

			// Create transfer encounter
			Encounter tEnc = new Encounter();
			tEnc.setId(IdDt.newRandomUuid());
			tEnc.setStatus(Encounter.EncounterStatus.FINISHED);
			tEnc.setClass_(new Coding().setSystem(
					"https://www.medizininformatik-initiative.de/fhir/core/CodeSystem/EncounterClassAdditionsDE")
					.setCode("_ActEncounterCode").setDisplay("ActEncounterCode"));
			tEnc.setSubject(enc.getSubject());
			tEnc.setPeriod(new Period().setStart(t.getIntime()).setEnd(t.getOuttime()));
			tEnc.addLocation().setLocation(new Reference(locWard.getId()))
					.setStatus(Encounter.EncounterLocationStatus.COMPLETED).setPhysicalType(locWard.getPhysicalType())
					.setPeriod(new Period().setStart(t.getIntime()).setEnd(t.getOuttime()));
			tEnc.setServiceProvider(
					new Reference(stations.getOrganization(stations.getStation(t.getCareUnit())).getId()));
			tEnc.setPartOf(new Reference(enc.getId()));

			String wIdentifier = locWard.getIdentifierFirstRep().getValue();
			String eIdentifier = tEnc.getIdentifierFirstRep().getValue();
			if (!locationsInBundle.containsKey(wIdentifier)) {
				// add to memory list:
				locationsInBundle.put(wIdentifier, locWard.getId());

				bundleC.addUUIDResourceWithConditionToBundle(locWard,
						"identifier=" + locWard.getIdentifierFirstRep().getSystem() + "|" + wIdentifier);
				bundleC.addUUIDResourceWithConditionToBundle(tEnc,
						"identifier=" + tEnc.getIdentifierFirstRep().getSystem() + "|" + eIdentifier);
			}

			if (validateResources) {
				instanceValidator.validateAndPrint(locWard);
				instanceValidator.validateAndPrint(tEnc);
			}
		}

		// add Encounter to bundle
		bundleC.addUUIDResourceWithConditionToBundle(enc,
				"identifier=" + enc.getIdentifierFirstRep().getSystem() + "|" + enc.getIdentifierFirstRep().getValue());

		if (validateResources) {
			instanceValidator.validateAndPrint(enc);
		}
	}

	private String processCaregiver(int caregiverId) {
		MCaregiver cgHere = caregivers.get(caregiverId);
		// Create FHIR-Resources for Practitioner und -Role
		Practitioner pFhir = cgHere.getFhirRepresentation();
		String identifier = pFhir.getIdentifierFirstRep().getValue();
		String id;
		if (!caregiversInBundle.containsKey(identifier)) {
			// add to memory list
			caregiversInBundle.put(identifier, pFhir.getId());
			id = pFhir.getId();
			bundleC.addUUIDResourceWithConditionToBundle(pFhir,
					"identifier=" + pFhir.getIdentifierFirstRep().getSystem() + "|" + identifier);
			if (validateResources) {
				instanceValidator.validateAndPrint(pFhir);
			}

			PractitionerRole roleFhir = cgHere.getFhirRepresentationRole();
			if (roleFhir != null) {
				roleFhir.setPractitioner(new Reference(pFhir.getId()));
				roleFhir.setOrganization(new Reference(hospital.getId()));
				bundleC.addUUIDResourceWithConditionToBundle(roleFhir,
						"identifier=" + roleFhir.getIdentifierFirstRep().getSystem() + "|"
								+ roleFhir.getIdentifierFirstRep().getValue());

				if (validateResources) {
					instanceValidator.validateAndPrint(roleFhir);
				}
			}
		} else {
			id = caregiversInBundle.get(identifier);
		}
		return id;
	}

	private Organization createTopHospital() {
		// Create a "dummy" Organization that is "top player" of PractitionerRoles and
		// Locations
		Organization hospital = new Organization();
		hospital.addIdentifier().setSystem("http://www.imi-mimic.de").setValue("hospital");
		hospital.addType().addCoding().setCode("prov").setSystem("http://hl7.org/fhir/organization-type")
				.setDisplay("Healthcare Provider");
		hospital.setName("IMI-Mimic Hospital");
		hospital.setId(IdDt.newRandomUuid());
		return hospital;
	}

	private void resetMemoryLists() {
		caregiversInBundle.clear();
		locationsInBundle.clear();
		medicationInBundle.clear();
	}
}
