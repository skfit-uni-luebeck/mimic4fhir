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
import de.uzl.itcr.mimic4fhir.model.MPrescription;
import de.uzl.itcr.mimic4fhir.tools.Ingredient;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;
import de.uzl.itcr.mimic4fhir.tools.RxNormConcept;
import de.uzl.itcr.mimic4fhir.tools.RxNormLookup;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrescriptionManager extends ModelManager<Medication, MPrescription>{

    public PrescriptionManager(){
        super();
    }

    @Override
    public Medication createResource(MPrescription mPrescription, Config config){
        Medication m = new Medication();

        m.setId(mPrescription.getMedId());

        switch(config.getSpecification()){
            case KDS:
                //New canonical url from the KDS specification by the 'Medizininformatik Initiative'
                m.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-medikation/StructureDefinition/Medication");

                /*Get ingredients with associated codes and names
                 * They will be used later to fill the Code and Ingredient element*/
                List<Ingredient> ingredients = null;
                if (mPrescription.getNdc() != null && mPrescription.getNdc().compareTo("0") != 0) {
                    //we do have a NDC:
                    ingredients = RemoteInformationLookup.getInstance(config)
                            .rxNormLookup.getIngredientsForNDC(mPrescription.getNdc());
                }

                //Add code element only if ATC codes have been found, since this element isn't mandatory
                CodeableConcept code = new CodeableConcept();
                if (ingredients != null && !ingredients.isEmpty()) {
                    //Add ATC code for every ingredient present in the medication
                    for (Ingredient ingredient : ingredients) {
                        //An ingredient might have multiple ATC codes associated with it
                        for (String atcCode : ingredient.getAtcCodes()) {
                            code.addCoding(new Coding().setSystem("http://www.whocc.no/atc").setCode(atcCode)
                                    .setDisplay(ingredient.getDescription()));
                        }
                    }
                }
                //Only add CodeableConcept if ATC codes are present for a given medication
                if (code.hasCoding()) {
                    m.setCode(code);
                }

                //Add container information (i.e. form of the medication itself; tablets, capsule etc.)
                if (mPrescription.getFormRx() != null) {
                    Coding form = new Coding();
                    boolean matchesAny = true;
                    switch (mPrescription.getFormRx()) {
                        case "AMP":
                            form.setCode("30001000").setDisplay("Ampulle");
                            break;
                        case "BOTTLE":
                            form.setCode("30008000").setDisplay("Flasche");
                            break;
                        case "CART":
                            form.setCode("300014000").setDisplay("Patrone");
                            break;
                        //Does it actually correspond to cream?
                        case "CRE":
                        case "CREA":
                            form.setCode("10502000").setDisplay("Creme");
                            break;
                        case "DROPS":
                            form.setCode("10307000").setDisplay("Tropfen zur Anwendug in der Mundhöhle");
                            break;
                        case "EPI PEN":
                        case "PEN":
                            form.setCode("13114000").setDisplay("Pen");
                            break;
                        case "GEL":
                        case "GELS":
                            form.setCode("1053000").setDisplay("Gel");
                            break;
                        case "GRAN":
                            form.setCode("10204000").setDisplay("Granulat");
                            break;
                        //Missing: GUM, GUMMY
                        //IMP = Implant or Impregnated Material?
                        case "IMP":
                            form.setCode("11301000").setDisplay("Implantat");
                            break;
                        case "INH":
                            form.setCode("30026500").setDisplay("Inhalator");
                            break;
                        case "INJ":
                        case "SYRI":
                            form.setCode("30029000").setDisplay("Injektionsspritze");
                            break;
                        //Missing: INS
                        //Missing: LIQ
                        case "LOZ":
                            form.setCode("10321000").setDisplay("Lutschtablette");
                            break;
                        case "OINT":
                            form.setCode("10504000").setDisplay("Salbe");
                            break;
                        case "OOINT":
                            form.setCode("10214005").setDisplay("Salbe zur Anwendung in der Mundhoehle");
                            break;
                        //PAS = Paste or Pastille?
                        //Missing: PTCH
                        //Missing: PWDR
                        //Missing: SOLN
                        case "PUMP":
                            form.setCode("30035000").setDisplay("Dosierpumpe");
                            break;
                        //Missing: STK (Stick probably)
                        case "SUPP":
                            form.setCode("11013000").setDisplay("Zäpfchen");
                            break;
                        //Missing: SUSP
                        case "SYRP":
                            form.setCode("11017000").setDisplay("Sirup");
                            break;
                        //Does TAB mean Tablet?
                        case "TAB":
                        case "TABLET":
                            form.setCode("10219000").setDisplay("Tablette");
                            break;
                        case "TUBE":
                            form.setCode("30067000").setDisplay("Tube");
                            break;
                        case "VIAL":
                            form.setCode("30069000").setDisplay("Durchstechflasche");
                            break;
                        default:
                            matchesAny = false;
                    }
                    //Since providing a form isn't mandatory it is only added if a matching code was found
                    if (matchesAny) {
                        m.setForm(new CodeableConcept().addCoding(form));
                    }
                }

                //Add all ingredients present in the medication
                if (ingredients != null && !ingredients.isEmpty()) {
                    for (Ingredient ingredient : ingredients) {
                        Medication.MedicationIngredientComponent mIngredient = new Medication.MedicationIngredientComponent();
                        CodeableConcept item = new CodeableConcept();
                        item.setText(ingredient.getDescription());
                        //Add all UNII and SNOMEDCT codes for the ingredient
                        for (String uniiCode : ingredient.getUniiCodes()) {
                            item.addCoding(new Coding().setSystem("http://fdasis.nlm.nih.gov").setCode(uniiCode));
                        }
                        for (String snomedCode : ingredient.getSnomedCodes()) {
                            item.addCoding(new Coding().setSystem("http://snomed.info/sct").setCode(snomedCode));
                        }
                        mIngredient.setItem(item);

                        //Add information on ingredient/substance type
                        Extension wirkstoffTypExtension = new Extension().setUrl("https://simplifier.net/medizininformatikinitiative-modulmedikation/extensionwirkstoffrelation")
                                .setValue(new CodeType().setValue("IN"));
                        mIngredient.addExtension(wirkstoffTypExtension);

                        m.addIngredient(mIngredient);
                    }
                } else {
                    /*Adding reason for ingredients not being present
                     * A reason for this might be that there weren't any RxCUIs associated with the medications NDC or that the
                     * MIMICIV data base didn't contain any NDC for the prescription*/
                    Extension absentReasonExt = new Extension();
                    absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
                    if (mPrescription.getNdc() == null || mPrescription.getNdc().compareTo("0") == 0) {
                        absentReasonExt.setValue(new CodeType().setValue("unknown"));
                    } else {
                        absentReasonExt.setValue(new CodeType().setValue("asked-unknown"));
                    }
                    m.getIngredientFirstRep().addExtension(absentReasonExt);
                }
                break;

            case R4:
                //RxNorm
                List<RxNormConcept> rxNorm = null;
                String existingCode = null;
                RxNormLookup rxLookup = new RxNormLookup();
                if(mPrescription.getNdc() != null && mPrescription.getNdc().compareTo("0") != 0) {
                    //we do have a NDC:
                    existingCode = mPrescription.getNdc();
                    rxNorm = rxLookup.getRxNormForNdc(mPrescription.getNdc());
                }

                if(rxNorm == null && mPrescription.getGsn() != null) {
                    //no result for ndc, but gsn -> try again
                    if(existingCode == null) {
                        existingCode = mPrescription.getGsn();
                    }

                    String[] gsnSingles = mPrescription.getGsn().split(" ");
                    //Multiple GSN-Codes possible - take all..
                    rxNorm = new ArrayList<RxNormConcept>();
                    for(String gsnSingle : gsnSingles)
                    {
                        rxNorm.addAll(rxLookup.getRxNormForGsn(gsnSingle.trim()));
                    }
                }

                CodeableConcept cc = new CodeableConcept();
                if(rxNorm != null && !rxNorm.isEmpty()) {
                    for(RxNormConcept rx : rxNorm) {
                        cc.addCoding().setSystem("http://www.nlm.nih.gov/research/umls/rxnorm").setCode(rx.getCui()).setDisplay(rx.getName());
                    }
                }
                else {
                    if((rxNorm == null || rxNorm.isEmpty()) && mPrescription.getFormularyDrugCd() != null) {
                        //seem to be some mnemonic codes
                        if(existingCode == null) {
                            existingCode = mPrescription.getFormularyDrugCd();
                        }
                        cc.addCoding().setCode(mPrescription.getFormRx()); //maybe add a system
                    }else {
                        if(existingCode == null) {
                            existingCode = mPrescription.getDrug()  + "(Text Only)";
                        }
                        cc.setText(mPrescription.getDrug());
                        cc.addCoding().setCode(existingCode);
                    }
                }
                cc.setText(mPrescription.getDrug());
                m.setCode(cc);

                //ingredient --> prod strength?
                if(mPrescription.getProdStrength() != null) {
                    CodeableConcept ci = new CodeableConcept();
                    ci.setText(mPrescription.getProdStrength());
                    m.addIngredient(new Medication.MedicationIngredientComponent(ci));
                }

                m.setId(IdDt.newRandomUuid());
                break;
        }

        return m;
    }

    public MedicationAdministration createAdministration(MPrescription mPrescription, int seqNum, Config config){
        MedicationAdministration ma = new MedicationAdministration();

        ma.addIdentifier().setSystem("http://www.imi-mimic.de/prescriptions").setValue(mPrescription.getEncId() + "_" + seqNum);

        ma.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-medikation/StructureDefinition/MedicationAdministration");

        ma.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);

        ma.setMedication(new Reference(mPrescription.getMedId()));

        ma.setSubject(new Reference(mPrescription.getPatId()));
        ma.setContext(new Reference(mPrescription.getEncId()));

        MedicationAdministration.MedicationAdministrationDosageComponent mad = new MedicationAdministration.MedicationAdministrationDosageComponent();
        String doseText = "";
        switch (config.getSpecification()){
            case KDS:
                //Set start and end date for effective period while accounting for input errors in the data base
                Date start = mPrescription.getStart(), end = mPrescription.getEnd();
                if (start != null && end != null) {
                    if (end.before(start)) {
                        ma.setEffective(new Period().setEnd(start).setStart(end));
                    } else {
                        ma.setEffective(new Period().setEnd(end).setStart(start));
                    }
                } else {
                    Extension absentReasonExt = new Extension();
                    absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
                    absentReasonExt.setValue(new CodeType().setValue("unknown"));
                    Period absentPeriod = new Period();
                    absentPeriod.addExtension(absentReasonExt);
                    ma.setEffective(absentPeriod);
                }

                if (mPrescription.getRoute() != null) {
                    CodeableConcept route = new CodeableConcept();
                    Coding snomedRoute = new Coding().setSystem("http://snomed.info/sct");
                    Coding edqmRoute = new Coding();
                    switch (mPrescription.getRoute()) {
                        case "IV":
                        case "IV BOLUS":
                        case "IV DRIP":
                        case "IVPCA":
                        case "IVS":
                        case "PB":
                            snomedRoute.setDisplay("Intravenous route").setCode("47625008");
                            edqmRoute.setDisplay("Intravenous use").setCode("20045000");
                            break;
                        case "PO":
                        case "PO/OG":
                        case "ORAL":
                        case "PO OR ENTERAL TUBE":
                            snomedRoute.setDisplay("Oral Route").setCode("26643006");
                            edqmRoute.setDisplay("Oral use").setCode("20053000");
                            break;
                        case "PO/NG":
                        case "NG/OG":
                        case "NG":
                            snomedRoute.setDisplay("Nasogastric route").setCode("127492001");
                            edqmRoute.setDisplay("Nasal use").setCode("20049000");
                            break;
                        case "PR":
                        case "RECTAL":
                            snomedRoute.setDisplay("Per rectum").setCode("37161004");
                            edqmRoute.setDisplay("Rectal use").setCode("20061000");
                            break;
                        case "INTRAPERICARDIAL":
                            snomedRoute.setDisplay("Intrapericardial route").setCode("445771006");
                            edqmRoute.setDisplay("Intrapericardial use").setCode("20037000");
                            break;
                        case "RIGHT EYE":
                        case "LEFT EYE":
                        case "BOTH EYES":
                        case "OS":
                        case "OD":
                        case "OU":
                            snomedRoute.setDisplay("Ophthalmic route").setCode("54485002");
                            edqmRoute.setDisplay("Ocular use").setCode("20051000");
                            break;
                        case "SC":
                        case "SUBCUT":
                            snomedRoute.setDisplay("Subcutaneous route").setCode("34206005");
                            edqmRoute.setDisplay("Subcutaneous use").setCode("20066000");
                            break;
                        case "IH":
                        case "AERO":
                        case "INHALATION":
                        case "NEB":
                            snomedRoute.setDisplay("Respiratory tract route").setCode("447694001");
                            edqmRoute.setDisplay("Inhalation use").setCode("20020000");
                            break;
                        case "ID":
                            snomedRoute.setDisplay("Intradermal use").setCode("372464004");
                            edqmRoute.setDisplay("Intradermal use").setCode("20030000");
                            break;
                        case "LEFT EAR":
                        case "RIGHT EAR":
                        case "BOTH EARS":
                            snomedRoute.setDisplay("Otic route").setCode("10547007");
                            edqmRoute.setDisplay("Auricular use").setCode("20001000");
                            break;
                        case "IC":
                            snomedRoute.setDisplay("Intracardiac use").setCode("372460008");
                            edqmRoute.setDisplay("Intracardiac use").setCode("20026000");
                            break;
                        case "IN":
                        case "NAS":
                        case "NU":
                            snomedRoute.setDisplay("Nasal route").setCode("46713006");
                            edqmRoute.setDisplay("Nasal use").setCode("20049000");
                            break;
                        case "IM":
                            snomedRoute.setDisplay("Intramuscular route").setCode("78421000");
                            edqmRoute.setDisplay("Intramuscular use").setCode("20035000");
                            break;
                        case "BUCCAL":
                        case "BU":
                            snomedRoute.setDisplay("Buccal route").setCode("54471007");
                            edqmRoute.setDisplay("Buccal use").setCode("20002500");
                            break;
                        case "TP": //topic
                            snomedRoute.setDisplay("Topical route").setCode("6064005");
                            edqmRoute.setDisplay("Route of administration not applicable").setCode("20062000");
                            break;
                        case "ED":
                            snomedRoute.setDisplay("Epidural route").setCode("404820008");
                            edqmRoute.setDisplay("Epidural use").setCode("20009000");
                            break;
                        case "TD":
                            snomedRoute.setDisplay("Transdermal route").setCode("45890007");
                            edqmRoute.setDisplay("Transdermal use").setCode("20070000");
                            break;
                        case "IT":
                            snomedRoute.setDisplay("Intrathecal route").setCode("72607000");
                            edqmRoute.setDisplay("Intrathecal use").setCode("20042000");
                            break;
                        case "SL":
                            snomedRoute.setDisplay("Sublingual route").setCode("37839007");
                            edqmRoute.setDisplay("Sublingual use").setCode("20067000");
                            break;
                        case "G TUBE":
                            snomedRoute.setDisplay("Gastrostomy route").setCode("127490009");
                            edqmRoute.setDisplay("Gastric use").setCode("20013500");
                            break;
                        case "VG":
                            snomedRoute.setDisplay("Per vagina").setCode("16857009");
                            edqmRoute.setDisplay("Vaginal use").setCode("20072000");
                            break;
                        case "IP":
                            snomedRoute.setDisplay("Intraperitoneal route").setCode("38239002");
                            edqmRoute.setDisplay("Intraperitoneal use").setCode("20038000");
                            break;
                        case "J TUBE":
                            snomedRoute.setDisplay("Jejunostomy route").setCode("127491008");
                            edqmRoute.setDisplay("Intestinal use").setCode("20021000");
                            break;
                        case "ET":
                            snomedRoute.setDisplay("Intratracheal route").setCode("404818005");
                            edqmRoute.setDisplay("Endotracheopulmonary use").setCode("20008000");
                            break;
                        default:
                            edqmRoute.setDisplay("Route of administration not applicable").setCode("20062000");
                            break;
                    }

                    route.setText(mPrescription.getRoute());
                    mad.setRoute(route);
                }

                String doseUnit = mPrescription.getDoseUnitRx(), doseVal = mPrescription.getDoseValRx();
                if (doseUnit != null && doseVal != null) {
                    SimpleQuantity dose = new SimpleQuantity();
                    String[] conformVals = doseVal.trim().split("\\-");
                    boolean isApplicable = false;
                    double val;
                    try {
                        val = Double.parseDouble(conformVals[conformVals.length - 1]);

                        isApplicable = true;

                        switch (doseUnit) {
                            case "g":
                                dose.setValue(val).setUnit("g").setSystem("http://unitsofmeasure.org").setCode("g");
                                break;
                            case "gm":
                                dose.setValue(val).setUnit("g.m").setSystem("http://unitsofmeasure.org").setCode("g.m");
                                break;
                            case "gtt":
                                dose.setValue(val).setUnit("[drp]").setSystem("http://unitsofmeasure.org").setCode("[drp]");
                                break;
                            case "mcg":
                                dose.setValue(val).setUnit("ug").setSystem("http://unitsofmeasure.org").setCode("ug");
                                break;
                            case "mcg/h":
                            case "mcg/hr":
                                dose.setValue(val).setUnit("ug/h").setSystem("http://unitsofmeasure.org").setCode("ug/h");
                                break;
                            case "mcg/kg":
                                dose.setValue(val).setUnit("ug/kg").setSystem("http://unitsofmeasure.org").setCode("ug/kg");
                                break;
                            case "mcg/kg/hr":
                                dose.setValue(val).setUnit("ug/kg/hr").setSystem("http://unitsofmeasure.org").setCode("ug/kg/hr");
                                break;
                            case "mcg/kg/min":
                                dose.setValue(val).setUnit("ug/kg/min").setSystem("http://unitsofmeasure.org").setCode("ug/kg/min");
                                break;
                            case "mcg/ml":
                            case "mcg/mL":
                                dose.setValue(val).setUnit("ug/mL").setSystem("http://unitsofmeasure.org").setCode("ug/mL");
                                break;
                            case "mEq":
                                dose.setValue(val).setUnit("meq").setSystem("http://unitsofmeasure.org").setCode("meq");
                                break;
                            case "mg":
                                dose.setValue(val).setUnit("mg").setSystem("http://unitsofmeasure.org").setCode("mg");
                                break;
                            case "mg/100 mL":
                                dose.setValue(val).setUnit("mg/(100.mL)").setSystem("http://unitsofmeasure.org").setCode("mg/(100.mL)");
                                break;
                            case "mg/24h":
                                dose.setValue(val).setUnit("mg/(24.h)").setSystem("http://unitsofmeasure.org").setCode("mg/(24.h)");
                                break;
                            case "mg/250 ml":
                            case "mg/250 mL":
                                dose.setValue(val).setUnit("mg/(250.mL)").setSystem("http://unitsofmeasure.org").setCode("mg/(250.mL)");
                                break;
                            case "mg /40 mg":
                            case "mg/40mg":
                                dose.setValue(val).setUnit("mg/(40.mg)").setSystem("http://unitsofmeasure.org").setCode("mg/(40.mg)");
                                break;
                            case "mg/500 ml":
                                dose.setValue(val).setUnit("mg/(500.mL)").setSystem("http://unitsofmeasure.org").setCode("mg/(500.mL)");
                                break;
                            case "mg/50 ml":
                            case "mg/50 mL":
                                dose.setValue(val).setUnit("mg/(50.mL)").setSystem("http://unitsofmeasure.org").setCode("mg/(50.mL)");
                                break;
                            case "mg/day":
                                dose.setValue(val).setUnit("mg/d").setSystem("http://unitsofmeasure.org").setCode("mg/d");
                                break;
                            case "mg/hr":
                                dose.setValue(val).setUnit("mg/h").setSystem("http://unitsofmeasure.org").setCode("mg/h");
                                break;
                            case "mg/kg":
                                dose.setValue(val).setUnit("mg/kg").setSystem("http://unitsofmeasure.org").setCode("mg/kg");
                                break;
                            case "mg/kg/hr":
                                dose.setValue(val).setUnit("mg/kg/h").setSystem("http://unitsofmeasure.org").setCode("mg/kg/h");
                                break;
                            case "mg/m2":
                                dose.setValue(val).setUnit("mg/m2").setSystem("http://unitsofmeasure.org").setCode("mg/m2");
                                break;
                            case "mg/ml":
                            case "mg/mL":
                                dose.setValue(val).setUnit("mg/mL").setSystem("http://unitsofmeasure.org").setCode("mg/mL");
                                break;
                            case "Million Cells":
                            case "million units":
                            case "Million Units":
                                dose.setValue(val).setUnit("10*6").setSystem("http://unitsofmeasure.org").setCode("10*6");
                                break;
                            case "ml":
                            case "mL":
                                dose.setValue(val).setUnit("mL").setSystem("http://unitsofmeasure.org").setCode("mL");
                                break;
                            case "ml/day":
                            case "mL/Day":
                                dose.setValue(val).setUnit("mL/d").setSystem("http://unitsofmeasure.org").setCode("mL/d");
                                break;
                            case "ml/hr":
                            case "mL/hr":
                                dose.setValue(val).setUnit("mL/h").setSystem("http://unitsofmeasure.org").setCode("mL/h");
                                break;
                            case "mL/kg":
                                dose.setValue(val).setUnit("mL/kg").setSystem("http://unitsofmeasure.org").setCode("mL/kg");
                                break;
                            case "mmol":
                                dose.setValue(val).setUnit("mmol").setSystem("http://unitsofmeasure.org").setCode("mmol");
                                break;
                            case "nanogram":
                                dose.setValue(val).setUnit("ng").setSystem("http://unitsofmeasure.org").setCode("ng");
                                break;
                            case "nanograms/kg/minute":
                                dose.setValue(val).setUnit("ng/kg/min").setSystem("http://unitsofmeasure.org").setCode("ng/kg/min");
                                break;
                            case "UNIT/HR":
                                dose.setValue(val).setUnit("/h").setSystem("http://unitsofmeasure.org").setCode("/h");
                                break;
                            case "UNIT/KG":
                            case "Units/kg":
                                dose.setValue(val).setUnit("/kg").setSystem("http://unitsofmeasure.org").setCode("/kg");
                                break;
                            case "Units/Liter":
                                dose.setValue(val).setUnit("/L").setSystem("http://unitsofmeasure.org").setCode("/L");
                                break;
                            default:
                                isApplicable = false;
                                break;
                        }
                    } catch (NumberFormatException exc) {
                        System.out.println("Dose value '" + conformVals[conformVals.length - 1] + "' is not a valid value!");
                    }

                    if (isApplicable) {
                        mad.setDose(dose);
                    }
                }

                //Dosage:
                //Create text if possible:
                if (mPrescription.getDoseValRx() != null && mPrescription.getDoseUnitRx() != null) {
                    String doseValRx = mPrescription.getDoseValRx().replaceAll("\\s", "");
                    String doseUnitRx = mPrescription.getDoseUnitRx().replaceAll("\\s", "");
                    doseText = doseValRx + " " + doseUnitRx;
                }

                if (mPrescription.getFormValDisp() != null && mPrescription.getFormUnitDisp() != null) {
                    String formValDisp = mPrescription.getFormValDisp().replaceAll("\\s", "");
                    String formUnitDisp = mPrescription.getFormUnitDisp().replaceAll("\\s", "");
                    if (doseText.length() > 0) {
                        doseText += " (" + formValDisp + " " + formUnitDisp + ")";
                    } else {
                        doseText = formValDisp + " " + formUnitDisp;
                    }
                }

                if (doseText.length() > 0) {
                    mad.setText(doseText);
                }
                break;

            case R4:
                if(mPrescription.getRoute() != null) {
                    CodeableConcept route = new CodeableConcept();
                    switch(mPrescription.getRoute()) {
                        case "IV":
                        case "IV BOLUS":
                        case "IV DRIP":
                        case "IVPCA":
                        case "IVS":
                        case "PB":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intravenous route").setCode("47625008");
                            break;
                        case "PO":
                        case "PO/OG":
                        case "ORAL":
                        case "PO OR ENTERAL TUBE":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Oral Route").setCode("26643006");
                            break;
                        case "PO/NG":
                        case "NG/OG":
                        case "NG":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Nasogastric route").setCode("127492001");
                            break;
                        case "PR":
                        case "RECTAL":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Per rectum").setCode("37161004");
                            break;
                        case "INTRAPERICARDIAL":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intrapericardial route").setCode("445771006");
                            break;
                        case "RIGHT EYE":
                        case "LEFT EYE":
                        case "BOTH EYES":
                        case "OS":
                        case "OD":
                        case "OU":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Ophthalmic route").setCode("54485002");
                            break;
                        case "SC":
                        case "SUBCUT":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Subcutaneous route").setCode("34206005");
                            break;
                        case "IH":
                        case "AERO":
                        case "INHALATION":
                        case "NEB":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Respiratory tract route").setCode("447694001");
                            break;
                        case "ID":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intradermal use").setCode("372464004");
                            break;
                        case "LEFT EAR":
                        case "RIGHT EAR":
                        case "BOTH EARS":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Otic route").setCode("10547007");
                            break;
                        case "IC":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intracardiac use").setCode("372460008");
                            break;
                        case "IN":
                        case "NAS":
                        case "NU":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Nasal route").setCode("46713006");
                            break;
                        case "IM":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intramuscular route").setCode("78421000");
                            break;
                        case "BUCCAL":
                        case "BU":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Buccal route").setCode("54471007");
                            break;
                        case "TP": //topic
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Topical route").setCode("6064005");
                            break;
                        case "ED":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Epidural route").setCode("404820008");
                            break;
                        case "TD":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Transdermal route	").setCode("45890007");
                            break;
                        case "IT":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intrathecal route").setCode("72607000");
                            break;
                        case "SL":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Sublingual route").setCode("37839007");
                            break;
                        case "G TUBE":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Gastrostomy route").setCode("127490009");
                            break;
                        case "VG":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Per vagina").setCode("16857009");
                            break;
                        case "IP":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intraperitoneal route").setCode("38239002");
                            break;
                        case "J TUBE":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Jejunostomy route").setCode("127491008");
                            break;
                        case "ET":
                            route.addCoding().setSystem("http://snomed.info/sct").setDisplay("Intratracheal route").setCode("404818005");
                            break;
                        default:
                            route.setText(mPrescription.getRoute());
                            break;
                    }
                    mad.setRoute(route);
                }

                //Dosage:
                //Create text if possible:
                if(mPrescription.getDoseValRx() != null && mPrescription.getDoseUnitRx() != null) {
                    doseText = mPrescription.getDoseValRx() + " " + mPrescription.getDoseUnitRx();
                }

                if(mPrescription.getFormValDisp() != null && mPrescription.getFormUnitDisp() != null) {
                    if(doseText.length() > 0) {
                        doseText += " (" + mPrescription.getFormValDisp() + " " + mPrescription.getFormUnitDisp() + ")";
                    }else
                    {
                        doseText = mPrescription.getFormValDisp() + " " + mPrescription.getFormUnitDisp();
                    }
                }

                if(doseText.length() > 0) {
                    mad.setText(doseText);
                }

                //doseVal -> 30, doseUnit -> mg;
                //Amount of medication per dose -> given at one event
                double sqValue = 0.0;
                try {
                    sqValue = Double.parseDouble(mPrescription.getDoseValRx());
                    mad.setDose((SimpleQuantity) new SimpleQuantity().setValue(sqValue).setUnit(mPrescription.getDoseUnitRx()));
                }catch(NumberFormatException nfe) {

                }
                catch(NullPointerException xpe) {

                }
                //rate -> Speed
                ma.setDosage(mad);
                break;
        }

        return ma;
    }

}
