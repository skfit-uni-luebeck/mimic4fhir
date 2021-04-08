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

import org.hl7.fhir.r4.model.*;

import de.uzl.itcr.mimic4fhir.model.MChartevent;
import de.uzl.itcr.mimic4fhir.work.Config;

public class CharteventManager extends ModelManager<Observation, MChartevent>{

    public CharteventManager(){
        super();
    }

    @Override
    public Observation createResource(MChartevent mChartevent, Config config){
        Observation observation = new Observation();

        observation.setStatus(Observation.ObservationStatus.FINAL);

        CodeableConcept cc = new CodeableConcept();
        switch(config.getSpecification()){
            case KDS:
                //References the structure definition for the observation ('Vitalstatus') resource of the KDS resources
                observation.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Vitalstatus");

                //A category slice containing the code 'survey' is mandatory
                observation.addCategory().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                        .setCode("survey").setDisplay("Survey");

                Coding loinc_coding = new Coding();
                String label = mChartevent.getMeasurementType();
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

                //Expect all chartevents to be vital signs
                observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category")
                        .setCode("vital_signs").setDisplay("Vital Signs");

                //Actual result
                if(mChartevent.hasNumVal()) {
                    CodeableConcept value = new CodeableConcept(
                            new Coding("https://www.medizininformatik-initiative.de/fhir/core/CodeSystem/Vitalstatus",
                                    "X", "unbekannt"));
                    observation.setValue(value);
                }
                break;
            case R4:
                //Type of Observation
                //D_Items in Mimic doesn't relate the measurement types to any coding system or terminology
                // => Representation as plain text
                cc.setText(mChartevent.getMeasurementType());
                observation.setCode(cc);

                //Actual result
                if(mChartevent.hasNumVal()) {
                    Quantity value = new Quantity();
                    value.setValue(mChartevent.getNumValue());
                    value.setUnit(mChartevent.getUnit());

                    observation.setValue(value);
                }
                else
                {
                    observation.setValue(new StringType(mChartevent.getValue()));
                    //no units in data
                }
                break;
        }

        //Pat-Reference
        observation.setSubject(new Reference(mChartevent.getPatId()));

        /*Enc-Reference
         * In the R4 specification the context field has been replaced by the partOf field*/
        observation.addPartOf(new Reference(mChartevent.getEncId()));

        //Record-Date
        observation.setEffective(new DateTimeType(mChartevent.getRecordDate()));

        //Performer will be set later

        return observation;
    }

}
