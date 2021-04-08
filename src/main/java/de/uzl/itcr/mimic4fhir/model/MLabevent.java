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

import org.hl7.fhir.Code;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;

/**
 * Represents one row in mimiciii.labevents
 * 
 * @author Stefanie Ververs
 *
 */
public class MLabevent {

	// Labevent entry ID
	private int labeventId;

	// Rekord-Datum
	private Date acquisitionDate;

	// Type
	private String measurementType;

	// Value + ValueNum
	private String value;

	private double numValue;

	private boolean hasNumVal;

	private boolean abnormal;

	private String fluid;

	private String loinc;

	private String comments;

	private String patId;

	private String encId;

	public int getLabeventId() {
		return labeventId;
	}

	public void setLabeventId(int labeventId) {
		this.labeventId = labeventId;
	}

	public String getFluid() {
		return fluid;
	}

	public void setFluid(String fluid) {
		this.fluid = fluid;
	}

	public String getLoinc() {
		return loinc;
	}

	public void setLoinc(String loinc) {
		this.loinc = loinc;
	}

	public boolean isAbnormal() {
		return abnormal;
	}

	public void setAbnormal(boolean abnormal) {
		this.abnormal = abnormal;
	}

	public boolean hasNumVal() {
		return hasNumVal;
	}

	public void setHasNumVal(boolean hasNumVal) {
		this.hasNumVal = hasNumVal;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	// Unit
	private String unit;

	public Date getAcquisitionDate() {
		return acquisitionDate;
	}

	public void setAcquisitionDate(Date recordDate) {
		this.acquisitionDate = recordDate;
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

	public String getPatId() {
		return patId;
	}

	public void setPatId(String patId) {
		this.patId = patId;
	}

	public String getEncId() {
		return encId;
	}

	public void setEncId(String encId) {
		this.encId = encId;
	}

}
