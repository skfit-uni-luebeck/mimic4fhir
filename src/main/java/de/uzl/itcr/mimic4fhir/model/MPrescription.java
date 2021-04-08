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

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Medication.MedicationIngredientComponent;

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.tools.Ingredient;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;
import de.uzl.itcr.mimic4fhir.tools.RxNormConcept;
import de.uzl.itcr.mimic4fhir.tools.RxNormLookup;

import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationDosageComponent;
import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationStatus;

/**
 * Represents one row in mimiciii.prescriptions
 *
 * @author Stefanie Ververs
 */
public class MPrescription {

    private final IdDt medId;

    private Date start;
    private Date end;
    private String drugtype;
    private String drug;
    private String drugNamePoe;
    private String drugNameGeneric;
    private String formularyDrugCd;
    //Generic Sequence Number
    private String gsn;
    //National Drug Code
    private String ndc;
    private String prodStrength;
    private String formRx;
    private String doseValRx;
    private String doseUnitRx;
    private String formValDisp;
    private String formUnitDisp;

    private String route;
    private String patId;
    private String encId;

    public MPrescription() {
        //Add new and final uuid, which will be useful for referencing the Medication in the MedicationAdministration
        this.medId = IdDt.newRandomUuid();
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getDrugtype() {
        return drugtype;
    }

    public void setDrugtype(String drugtype) {
        this.drugtype = drugtype;
    }

    public String getDrug() {
        return drug;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public String getDrugNamePoe() {
        return drugNamePoe;
    }

    public void setDrugNamePoe(String drugNamePoe) {
        this.drugNamePoe = drugNamePoe;
    }

    public String getDrugNameGeneric() {
        return drugNameGeneric;
    }

    public void setDrugNameGeneric(String drugNameGeneric) {
        this.drugNameGeneric = drugNameGeneric;
    }

    public String getFormularyDrugCd() {
        return formularyDrugCd;
    }

    public void setFormularyDrugCd(String formularyDrugCd) {
        this.formularyDrugCd = formularyDrugCd;
    }

    public String getGsn() {
        return gsn;
    }

    public void setGsn(String gsn) {
        this.gsn = gsn;
    }

    public String getNdc() {
        return ndc;
    }

    public void setNdc(String ndc) {
        this.ndc = ndc;
    }

    public String getProdStrength() {
        return prodStrength;
    }

    public void setProdStrength(String prodStrength) {
        this.prodStrength = prodStrength;
    }

    public String getFormRx() {
        return formRx;
    }

    public void setFormRx(String formRx) {
        this.formRx = formRx;
    }

    public String getDoseValRx() {
        return doseValRx;
    }

    public void setDoseValRx(String doseValRx) {
        this.doseValRx = doseValRx;
    }

    public String getDoseUnitRx() {
        return doseUnitRx;
    }

    public void setDoseUnitRx(String doseUnitRx) {
        this.doseUnitRx = doseUnitRx;
    }

    public String getFormValDisp() {
        return formValDisp;
    }

    public void setFormValDisp(String formValDisp) {
        this.formValDisp = formValDisp;
    }

    public String getFormUnitDisp() {
        return formUnitDisp;
    }

    public void setFormUnitDisp(String formUnitDisp) {
        this.formUnitDisp = formUnitDisp;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public IdDt getMedId() {
        return medId;
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