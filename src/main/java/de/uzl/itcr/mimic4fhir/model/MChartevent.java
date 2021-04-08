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

import java.util.Date;

import ca.uhn.fhir.tinder.model.Slicing;
import org.hl7.fhir.ElementDefinition;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;

/**
 * Represents one row in mimiciii.chartevents
 * @author Stefanie Ververs
 *
 */
/*
* Removed careGiverId field together with getter/setter methods since the corresponding column cgid no longer exists in
* the mimic_icu.chartevents table
 */
public class MChartevent {
	 //Rekord-Datum
	 private Date recordDate;
	 
	 //Type
	 private String measurementType; 
	 
	 //Value + ValueNum
	 private String value;

	//Unit
	 private String unit;
	 
	 private double numValue;
	 
	 private boolean hasNumVal;

	 private Date storeDate;

	 private String patId;

	 private String encId;
	 
	 public boolean hasNumVal() {
		return hasNumVal;
	}

	public void setHasNumVal(boolean hasNumVal) {
		this.hasNumVal = hasNumVal;
	}

	public Date getRecordDate() {
		return recordDate;
	}

	public void setRecordDate(Date recordDate) {
		this.recordDate = recordDate;
	}

	public String getMeasurementType() {
		return measurementType;
	}

	public void setMeasurementType(String measurementType) {
		this.measurementType = measurementType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public double getNumValue() {
		return numValue;
	}

	public void setNumValue(double numValue) {
		this.hasNumVal = true;
		this.numValue = numValue;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isHasNumVal() {
		return hasNumVal;
	}

	public Date getStoreDate() {
		return storeDate;
	}

	public void setStoreDate(Date storeDate) {
		this.storeDate = storeDate;
	}

	public String getPatId() {
		return patId;
	}

	public void setPatId(String patId) {
		this.patId = patId;
	}

	public String getEncId() {
		return encId;
	}

	public void setEncId(String endId) {
		this.encId = endId;
	}

	/**
	 * Create FHIR-"Observation"-resource from this data
	 * @param patId Patient-FHIR-Resource-Id
	 * @param encId Encounter-FHIR-Resource-Id
	 * @return FHIR-Observation
	 */
	public Observation getFhirObservation(String patId, String encId) {
		Observation observation = new Observation();
		
		observation.setStatus(ObservationStatus.FINAL);

		////References the structure definition for the observation ('Vitalstatus') resource of the KDS resources
		observation.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Vitalstatus");

		//A category slice containing the code 'survey' is mandatory
		observation.addCategory().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
				.setCode("survey").setDisplay("Survey");
		
		//Expect all chartevents to be vital signs
		observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category")
				.setCode("vital_signs").setDisplay("Vital Signs");
		
		//Type of Observation
		//D_Items in Mimic doesn't relate the measurement types to any coding system or terminology
		// => Representation as plain text
		/*
		CodeableConcept cc = new CodeableConcept();
		cc.setText(this.getMeasurementType());
		observation.setCode(cc);
		 */

		CodeableConcept cc = new CodeableConcept();
		Coding loinc_coding = new Coding();
		String label = this.getMeasurementType();
		switch(label) {
			case ("Respiratory Rate"):
			case ("Respiratory Rate (Set)"):
			case ("Respiratory Rate (spontaneous)"):
			case ("Respiratory Rate (Total)"):
				loinc_coding.setCode("9279-1").setSystem("http://loinc.org").setDisplay("Respiratory Rate");
				break;
			case ("Heart Rate"):
				loinc_coding.setCode("8867-4").setSystem("http://loinc.org").setDisplay("Heart Rate");
				break;
			case ("PAR-Oxygen saturation"):
				loinc_coding.setCode("2708-6").setSystem("http://loinc.org").setDisplay("Oxygen Saturation");
			case ("Temp ApacheIIValue"):
			case ("LLE Temp"):
			case ("LUE Temp"):
			case ("RLE Temp"):
			case ("RUE Temp"):
				loinc_coding.setCode("8310-5").setSystem("http://loinc.org").setDisplay("Body Temperature");
				break;
			case ("Height"):
			case ("Height (cm)"):
				loinc_coding.setCode("8302-2").setSystem("http://loinc.org").setDisplay("Body Height");
				break;
			case ("Manual Blood Pressure Diastolic Left"):
			case ("Manual Blood Pressure Diastolic Right"):
			case ("Non Invasive Blood Pressure diastolic"):
			case ("Pulmonary Artery Pressure diastolic"):
			case ("ART BP Diastolic"):
			case ("Arterial Blood Pressure diastolic"):
				loinc_coding.setCode("8480-6").setSystem("http://loinc.org").setDisplay("Diastolic Blood Pressure");
				break;
			case ("Manual Blood Pressure Systolic Left"):
			case ("Manual Blood Pressure Systolic Right"):
			case ("Non Invasive Blood Pressure systolic"):
			case ("Pulmonary Artery Pressure systolic"):
			case ("ART BP Systolic"):
			case ("Arterial Blood Pressure systolic"):
				loinc_coding.setCode("8462-4").setSystem("http://loinc.org").setDisplay("Systolic Blood Pressure");
				break;
			default:
				// LOINC code for general vital sign is not contained in the magic LOINC codes table laid out by the
				// FHIR standard specification
				loinc_coding.setCode("75186-7").setSystem("http://loinc.org").setDisplay("Vital Sign");
		}
		cc.addCoding(loinc_coding);
		observation.setCode(cc);
		
		//Pat-Reference
		observation.setSubject(new Reference(patId));
		
		/*Enc-Reference
		* In the R4 specification the context field has been replaced by the partOf field*/
		observation.addPartOf(new Reference(encId));
		
		//Record-Date
		observation.setEffective(new DateTimeType(this.getRecordDate()));
		
		//Performer will be set later
		
		//Actual result
		if(this.hasNumVal()) {
			CodeableConcept value = new CodeableConcept(
					new Coding("https://www.medizininformatik-initiative.de/fhir/core/CodeSystem/Vitalstatus",
							"X", "unbekannt"));
			observation.setValue(value);
		}

		return observation;
	}
}
