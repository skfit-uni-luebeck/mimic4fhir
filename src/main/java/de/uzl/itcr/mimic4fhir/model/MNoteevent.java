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

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;

import de.uzl.itcr.mimic4fhir.tools.Helper;

/**
 * Represents one row in mimiciii.noteevents
 * @author Stefanie Ververs
 *
 */
public class MNoteevent {
	private Date chartdate;
	private String category;
	private String description;
	private int caregiverId;
	private String text;
	private boolean hasError;
	
	public boolean getHasError() {
		return hasError;
	}
	public void setHasError(boolean hasError) {
		this.hasError = hasError;
	}
	public Date getChartdate() {
		return chartdate;
	}
	public void setChartdate(Date chartdate) {
		this.chartdate = chartdate;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getCaregiverId() {
		return caregiverId;
	}
	public void setCaregiverId(int caregiverId) {
		this.caregiverId = caregiverId;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = Helper.CleanInvalidXmlChars(text, " ");
	}
	
	/**
	 * Create FHIR-"Observation" resources for this data
	 * @param patId Patient-FHIR-Resource-Id
	 * @param encId Encounter-FHIR-Resource-Id
	 * @return FHIR-Observation
	 */
	public Observation getFhirObservation(String patId, String encId) {
		Observation observation = new Observation();
		
		if(this.getHasError()) {
			observation.setStatus(ObservationStatus.ENTEREDINERROR);
		}
		else{
			observation.setStatus(ObservationStatus.FINAL);
		}
		
		//Not sure if observation is correct for (all) notes
		
		//"Type" of Observation
		CodeableConcept cc = new CodeableConcept();
		//Representation as plain text 
		cc.setText(this.getDescription());
		observation.setCode(cc);
		
		//Category is only possible for some:
		switch(this.getCategory())
		{
			case "Echo":
			//imaging
				observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category").setCode("imaging").setDisplay("Imaging");		
				break;
			case "ECG":
			case "Respiratory":
				observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category").setCode("procedure").setDisplay("Procedure");		
				break;
			case "Social Work":
				observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category").setCode("social-history").setDisplay("Social History");		
				break;
			case "Pharmacy":
			case "Rehab Services":
				observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category").setCode("therapy").setDisplay("Therapy");		
				break;
		}
				
		/*Indication for Noteevent -> No Value, but comment
		* In the R4 specification the functionality of the comment field has been replaced by the note field*/
		observation.addNote(new Annotation().setText(this.getText()));
					
		//Pat-Reference
		observation.setSubject(new Reference(patId));
		
		/*Enc-Reference
		* In the R4 specification the functionality of the context field has been replaced by the partOf field*/
		observation.addPartOf(new Reference(encId));
		
		//Record-Date
		observation.setEffective(new DateTimeType(this.getChartdate()));
		return observation;
	}
}
