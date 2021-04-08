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
import de.uzl.itcr.mimic4fhir.model.MAdmission;
import de.uzl.itcr.mimic4fhir.model.MDiagnose;
import de.uzl.itcr.mimic4fhir.model.MTransfer;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

/*Unfortunately this class can't be a subclass of ModelManager. A redesign is necessary in the future*/
public class AdmissionManager{

    public AdmissionManager(){
        super();
    }

    public Encounter createAdmission(MAdmission mAdmission, StationManager stations, Config config){
        Encounter enc = new Encounter();

        // Id
        enc.addIdentifier().setSystem("http://www.imi-mimic.de/encs").setValue(mAdmission.getAdmissionId())
                .setType(new CodeableConcept().addCoding(
                        new Coding().setCode("VN").setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")));

        // References the structure definition for the encounter ('Fall') resource of
        // the KDS resources
        enc.getMeta().addProfile(
                "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/StructureDefinition/KontaktGesundheitseinrichtung");

        // Patient
        enc.setSubject(new Reference(mAdmission.getPatId()));

        // Period
        enc.setPeriod(new Period().setStart(mAdmission.getAdmissionTime()).setEnd(mAdmission.getDischargeTime()));

        // all admissions are finished
        enc.setStatus(Encounter.EncounterStatus.FINISHED);

        /*
         * //Identifier containing the record type Identifier aufnahmeNummer = new
         * Identifier(); Coding vnType = new Coding();
         * vnType.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203");
         * vnType.setCode("VN"); aufnahmeNummer.getType().addCoding(vnType);
         * enc.addIdentifier(aufnahmeNummer);
         */

        // AdmissionType -> Class
        switch (mAdmission.getAdmissionType()) {
            case "SURGICAL SAME DAY ADMISSION":
                enc.setClass_(new Coding().setCode("operation")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Operation"));
            case "OBSERVATION ADMIT":
                enc.setClass_(new Coding().setCode("ub")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Untersuchung und Behandlung"));
            case "ELECTIVCE":
                enc.setClass_(new Coding().setCode("normalstationaer")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Normalstationaer"));
                break;
            case "URGENT":
            case "AMBULATORY OBSERVATION":
                enc.setClass_(new Coding().setCode("O")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("ambulant"));
                break;
            case "EMERGENCY":
            case "DIRECT EMER.":
            case "EW EMER.":
                enc.setClass_(new Coding().setCode("E")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Notfall"));
                break;
            case "NEWBORN":
                enc.setClass_(new Coding().setCode("B")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Geburtshilfe"));
                break;
            default:
                enc.setClass_(new Coding().setCode("N")
                        .setSystem("https://www.medizininformatik-initiative.de/fhir/core/ValueSet/EncounterClassDE")
                        .setDisplay("Segment nicht anwendbar"));
                break;
        }

        // Add all diagnoses that happened during the admission/stay will be refrenced
        for (MDiagnose diagnosis : mAdmission.getDiagnoses()) {
            Encounter.DiagnosisComponent dComponent = new Encounter.DiagnosisComponent();
            dComponent.setCondition(new Reference(mAdmission.getAdmissionId() + "_" + diagnosis.getSeqNumber()));
            dComponent.setRank(diagnosis.getSeqNumber());
            enc.addDiagnosis(dComponent);
        }

        // Discharge Location
        /*
         * Switched coding system to the "Entlassungsgrund" code system of the KDS
         * specification. Mapping involved some interpretation since a one-to-one match
         * did not exist for every case
         */
        Encounter.EncounterHospitalizationComponent ehc = new Encounter.EncounterHospitalizationComponent();
        CodeableConcept discharge = new CodeableConcept();
        // If there is no data available (dischargeLocation == null), mark the
        // information as unavailable
        if (mAdmission.getDischargeLocation() == null) {
            discharge.addCoding().setSystem(
                    "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                    .setCode("039").setDisplay("Behandlung aus sonstigen Gruenden beendet, keine Angabe");
        } else {
            switch (mAdmission.getDischargeLocation()) {
                case "HOME":
                case "HOME WITH HOME IV PROVIDR":
                case "HOME HEALTH CARE":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("019").setDisplay("Behandlung regulaer beendet, keine Angabe");
                    break;
                case "HOSPICE-MEDICAL FACILITY":
                case "HOSPICE-HOME":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("119").setDisplay("Entlassung in ein Hospiz");
                    break;
                case "REHAB/DISTINCT PART HOSP":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("099").setDisplay("Entlassung in eine Rehabilitationseinrichtung");
                    break;
                case "DISC-TRAN CANCER/CHLDRN H":
                case "OTHER FACILITY":
                case "DISC-TRAN TO FEDERAL HC":
                case "SHORT TERM HOSPITAL":
                case "ICF":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("069").setDisplay("Verlegung in ein anderes Krankenhaus");
                    break;
                case "DISCH-TRAN TO PSYCH HOSP":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("13").setDisplay("externe Verlegung zur psychiatrischen Behandlung");
                    break;
                case "DEAD/EXPIRED":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("079").setDisplay("Tod");
                    break;
                case "LEFT AGAINST MEDICAL ADVI":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("049").setDisplay("Behandlung gegen aerztlichen Rat beendet, keine Angabe");
                    break;
                case "LONG TERM CARE HOSPITAL":
                case "SNF":
                case "SNF-MEDICAID ONLY CERTIF":
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("109").setDisplay("Entlassung in eine Pflegeeinrichtung");
                    break;
                default:
                    discharge.addCoding().setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Entlassungsgrund")
                            .setCode("039").setDisplay("Behandlung aus sonstigen Gruenden beendet, keine Angabe");
                    break;
            }
        }
        ehc.setDischargeDisposition(discharge);

        // Admit Source from Admission location
        /*
         * Switched coding system to the "Aufnahmeanlass" code system of the KDS
         * specification. Mapping involved some interpretation since a one-to-one match
         * did not exist for every case
         */
        CodeableConcept cal = new CodeableConcept();
        boolean informationPresent = true;

        if (mAdmission.getAdmissionLocation() != null) {
            switch (mAdmission.getAdmissionLocation()) {
                case "PHYS REFERRAL/NORMAL DELI":
                case "HMO REFERRAL/SICK":
                    cal.addCoding().setCode("E").setDisplay("Einweisung durch einen Arzt").setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Aufnahmeanlass");
                    break;
                case "TRSF WITHIN THIS FACILITY":
                case "TRANSFER FROM OTHER HEALT":
                    cal.addCoding().setCode("other").setDisplay("Other").setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Aufnahmeanlass");
                    break;
                case "TRANSFER FROM SKILLED NUR":
                    cal.addCoding().setCode("R")
                            .setDisplay("Aufnahme nach vorausgehender Behandlung in einer Rehabilitationseinrichtung")
                            .setSystem(
                                    "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Aufnahmeanlass");
                    break;
                case "** INFO NOT AVAILABLE **":
                    informationPresent = false;
                    break;
                case "CLINIC REFERRAL/PREMATURE":
                case "TRANSFER FROM HOSP/EXTRAM":
                    cal.addCoding().setCode("hosp-trans").setDisplay("Transferred from other hospital").setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Aufnahmeanlass");
                    break;
                case "EMERGENCY ROOM ADMIT":
                    cal.addCoding().setCode("N").setDisplay("Notfall").setSystem(
                            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/CodeSystem/Aufnahmeanlass");
                    break;
            }
        } else {
            informationPresent = false;
        }
        // Only add information about the source of admission if the information is
        // actually present
        if (informationPresent) {
            ehc.setAdmitSource(cal);
        }
        enc.setHospitalization(ehc);

        // Add all locations the patient was at during his stay
        for (MTransfer transfer : mAdmission.getTransfers()) {
            Encounter.EncounterLocationComponent lComponent = new Encounter.EncounterLocationComponent();
            Location location = stations.getLocation(stations.getStation(transfer.getCareUnit()));

            lComponent.setLocation(new Reference(location.getId()));
            lComponent.setStatus(Encounter.EncounterLocationStatus.COMPLETED);
            lComponent.setPhysicalType(location.getPhysicalType());
            lComponent.setPeriod(new Period().setStart(transfer.getIntime()).setEnd(transfer.getOuttime()));

            enc.addLocation(lComponent);
        }

        // Give the encounter a temporary UUID so that other resources in
        // the transaction can refer to it
        enc.setId(IdDt.newRandomUuid());

        return enc;
    }

}
