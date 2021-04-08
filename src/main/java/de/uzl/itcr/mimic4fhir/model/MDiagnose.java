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

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.tools.ICD9MapperLookup;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;
import de.uzl.itcr.mimic4fhir.tools.StringManipulator;

import java.util.Date;

/**
 * Represents one diagnose in diagnoses_icd joined with d_icd_diagnoses
 * @author Stefanie Ververs
 *
 */
/*
* Renamed field an corresponding methods of icd9_code to icd_code in accordance to the change in the names of the
* corresponding column of the diagnosis_icd table
* Removed shortTitle field and its getter/setter methods due to its corresponding column no longer existing in any table
 */
public class MDiagnose {
	private String icdCode;
	private String icdVersion;
	private String longTitle;
	private int seqNumber;
	private String patId;
	private String encId;

	public int getSeqNumber() {
		return seqNumber;
	}
	public void setSeqNumber(int seqNumber) {
		this.seqNumber = seqNumber;
	}
	public String getIcdCode() {
		return icdCode;
	}
	public void setIcdCode(String icdCode) {
		this.icdCode = icdCode;
	}
	public String getIcdVersion() { return this.icdVersion; }
	public void setIcdVersion(String icdVersion) {this.icdVersion = icdVersion;}
	public String getLongTitle() {
		return longTitle;
	}
	public void setLongTitle(String longTitle) {
		this.longTitle = longTitle;
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
