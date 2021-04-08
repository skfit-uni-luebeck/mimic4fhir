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

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.tools.ProcedureSNOMEDLookup;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one row in mimiciii.procedures_icd joined with d_icd_procedures
 * @author Stefanie Ververs
 *
 */
/*
 * Renamed field an corresponding methods of icd9_code to icd_code in accordance to the change in the names of the
 * corresponding column of the procedure_icd table
 * Removed shortTitle field and its getter/setter methods due to its corresponding column no longer existing in any table
 */
public class MProcedure {
	private String icdCode;
	private String longTitle;
	private int seqNumber;
	private IcdVersion icdVersion;
	private String patId;
	private String encId;

	public enum IcdVersion{
		ICD9PROC("9"), ICD10PROC("10");

		private final String value;

		IcdVersion(String value){
			this.value = value;
		}

		public String valueOf(){
			return  this.value;
		}
	}

	public String getIcdCode() {
		return icdCode;
	}

	public void setIcdCode(String icdCode) {
		this.icdCode = icdCode;
	}

	public String getLongTitle() {
		return longTitle;
	}

	public void setLongTitle(String longTitle) {
		this.longTitle = longTitle;
	}

	public int getSeqNumber() {
		return seqNumber;
	}

	public void setSeqNumber(int seqNumber) {
		this.seqNumber = seqNumber;
	}

	public IcdVersion getIcdVersion() {
		return icdVersion;
	}

	public void setIcdVersion(IcdVersion icdVersion) {
		this.icdVersion = icdVersion;
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