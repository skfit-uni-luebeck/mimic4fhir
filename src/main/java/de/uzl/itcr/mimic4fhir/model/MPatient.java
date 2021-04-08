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

import org.hl7.elm.r1.Slice;
import org.hl7.fhir.AddressType;
import org.hl7.fhir.Code;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName.NameUse;

import ca.uhn.fhir.model.primitive.IdDt;

/**
 * FHIR-Patient with data from mimic3, one row in mimiciii.patients
 * 
 * @author Stefanie Ververs
 *
 */
public class MPatient {

	public MPatient() {
		admissions = new ArrayList<MAdmission>();
	}

	private String patientSubjectId;
	private Date birthDate;
	private String gender;
	private Date deathDate;

	private List<MAdmission> admissions;
	private List<MDiagnosticReport> diagnosticReports;


	public List<MAdmission> getAdmissions() {
		return admissions;
	}

	public void addAdmission(MAdmission adm) {
		admissions.add(adm);
	}

	public void setAdmissions(List<MAdmission> admissions) {
		this.admissions = admissions;
	}

	public String getPatientSubjectId() {
		return patientSubjectId;
	}

	public void setPatientSubjectId(String patientSubjectId) {
		this.patientSubjectId = patientSubjectId;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Date getDeathDate() {
		return deathDate;
	}

	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public List<MDiagnosticReport> getDiagnosticReports() {
		return diagnosticReports;
	}

	public void setDiagnosticReports(List<MDiagnosticReport> diagnosticReports) {
		this.diagnosticReports = diagnosticReports;
	}

}
