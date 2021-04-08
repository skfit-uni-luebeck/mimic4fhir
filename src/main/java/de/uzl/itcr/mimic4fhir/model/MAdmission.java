/***********************************************************************
Copyright 2018 Stefanie Ververs, University of Lübeck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
/***********************************************************************/
package de.uzl.itcr.mimic4fhir.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.model.manager.StationManager;

/**
 * Represents one row (and references) in mimiciii.admissions
 * 
 * @author Stefanie Ververs
 *
 */
/*
 * Removed field
 */
public class MAdmission {

	public MAdmission() {
		diagnoses = new ArrayList<MDiagnose>();
		procedures = new ArrayList<MProcedure>();
		events = new ArrayList<MChartevent>();
		labevents = new ArrayList<MLabevent>();
		noteevents = new ArrayList<MNoteevent>();
		prescriptions = new ArrayList<MPrescription>();
		transfers = new ArrayList<MTransfer>();
	}

	private String admissionId;
	private String maritalStatus;
	private String language;
	// attribute religion no longer a column in the core.admissions table
	// private String religion;

	private Date admissionTime;
	private Date dischargeTime;
	private String admissionType;
	private String dischargeLocation;
	private String admissionLocation;
	private String patId;

	private List<MChartevent> events;
	private List<MLabevent> labevents;
	private List<MNoteevent> noteevents;
	private List<MDiagnose> diagnoses;
	private List<MProcedure> procedures;
	private List<MPrescription> prescriptions;
	private List<MTransfer> transfers;

	public List<MLabevent> getLabevents() {
		return labevents;
	}

	public void setLabevents(List<MLabevent> labevents) {
		this.labevents = labevents;
	}

	public void setEvents(List<MChartevent> events) {
		this.events = events;
	}

	public void setNoteevents(List<MNoteevent> noteevents) {
		this.noteevents = noteevents;
	}

	public void setDiagnoses(List<MDiagnose> diagnoses) {
		this.diagnoses = diagnoses;
	}

	public void setProcedures(List<MProcedure> procedures) {
		this.procedures = procedures;
	}

	public void setPrescriptions(List<MPrescription> prescriptions) {
		this.prescriptions = prescriptions;
	}

	public void setTransfers(List<MTransfer> transfers) {
		this.transfers = transfers;
	}

	public List<MTransfer> getTransfers() {
		return transfers;
	}

	public void addTransfer(MTransfer proc) {
		transfers.add(proc);
	}

	public List<MPrescription> getPrescriptions() {
		return prescriptions;
	}

	public void addPrescription(MPrescription proc) {
		prescriptions.add(proc);
	}

	public List<MProcedure> getProcedures() {
		return procedures;
	}

	public void addProcedure(MProcedure proc) {
		procedures.add(proc);
	}

	public List<MDiagnose> getDiagnoses() {
		return diagnoses;
	}

	public void addDiagnose(MDiagnose diag) {
		diagnoses.add(diag);
	}

	public List<MChartevent> getEvents() {
		return events;
	}

	public void addEvent(MChartevent event) {
		events.add(event);
	}

	public List<MLabevent> getLabEvents() {
		return labevents;
	}

	public void addLabEvent(MLabevent event) {
		labevents.add(event);
	}

	public List<MNoteevent> getNoteevents() {
		return noteevents;
	}

	public void addNoteEvent(MNoteevent event) {
		noteevents.add(event);
	}

	public String getAdmissionId() {
		return admissionId;
	}

	public void setAdmissionId(String admissionId) {
		this.admissionId = admissionId;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getPatId() {
		return patId;
	}

	public void setPatId(String patId) {
		this.patId = patId;
	}

	/*
	 * Since there is no more religion column in core.admissions these two methods
	 * no longer have a use
	 *
	 * public String getReligion() { return religion; } public void
	 * setReligion(String religion) { this.religion = religion; }
	 */

	public String getAdmissionLocation() {
		return admissionLocation;
	}

	public void setAdmissionLocation(String admissionLocation) {
		this.admissionLocation = admissionLocation;
	}

	public String getDischargeLocation() {
		return dischargeLocation;
	}

	public void setDischargeLocation(String dischargeLocation) {
		this.dischargeLocation = dischargeLocation;
	}

	public String getAdmissionType() {
		return admissionType;
	}

	public void setAdmissionType(String admissionType) {
		this.admissionType = admissionType;
	}

	public Date getAdmissionTime() {
		return admissionTime;
	}

	public void setAdmissionTime(Date admissionTime) {
		this.admissionTime = admissionTime;
	}

	public Date getDischargeTime() {
		return dischargeTime;
	}

	public void setDischargeTime(Date dischargeTime) {
		this.dischargeTime = dischargeTime;
	}

}
