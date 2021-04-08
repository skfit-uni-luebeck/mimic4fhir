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
package de.uzl.itcr.mimic4fhir.concur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.uzl.itcr.mimic4fhir.model.MAdmission;
import de.uzl.itcr.mimic4fhir.model.MChartevent;
import de.uzl.itcr.mimic4fhir.model.MDiagnose;
import de.uzl.itcr.mimic4fhir.model.MDiagnosticReport;
import de.uzl.itcr.mimic4fhir.model.MImagingStudy;
import de.uzl.itcr.mimic4fhir.model.MLabevent;
import de.uzl.itcr.mimic4fhir.model.MPatient;
import de.uzl.itcr.mimic4fhir.model.MPrescription;
import de.uzl.itcr.mimic4fhir.model.MProcedure;
import de.uzl.itcr.mimic4fhir.model.MTransfer;
import de.uzl.itcr.mimic4fhir.work.ConnectDB;

public class TransformerHelper {

	public ArrayList<MPatient> getPatients(int numberOfPatients, boolean random) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MPatient> temp = new ArrayList<MPatient>();
		String query = "SELECT * FROM MIMIC_CORE.PATIENTS LIMIT " + numberOfPatients;
		if (random) {
			query = "SELECT * FROM MIMIC_CORE.PATIENTS ORDER BY RANDOM() LIMIT " + numberOfPatients;
		}

		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				MPatient mPat = new MPatient();
				mPat.setPatientSubjectId(rs.getString(1));
				mPat.setGender(rs.getString(2));
				mPat.setDeathDate(rs.getDate(6));
				temp.add(mPat);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public MPatient getPatient(String subjectId) {
		Connection connection = ConnectDB.getConnection();
		String query = "SELECT * FROM MIMIC_CORE.PATIENTS WHERE SUBJECT_ID = " + subjectId;
		PreparedStatement statement;
		MPatient mPat = new MPatient();
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				mPat.setPatientSubjectId(rs.getString(1));
				mPat.setGender(rs.getString(2));
				mPat.setDeathDate(rs.getDate(6));
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mPat;
	}

	public ArrayList<MAdmission> getAdmission(MPatient mPat) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MAdmission> temp = new ArrayList<MAdmission>();
		String query = "SELECT * FROM MIMIC_CORE.ADMISSIONS WHERE SUBJECT_ID = " + mPat.getPatientSubjectId();
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				MAdmission mAdm = new MAdmission();
				mAdm.setAdmissionId(rs.getString(2));
				mAdm.setAdmissionTime(rs.getDate(3));
				mAdm.setDischargeTime(rs.getDate(4));
				mAdm.setAdmissionType(rs.getString(6));
				mAdm.setDischargeLocation(rs.getString(8));
				mAdm.setMaritalStatus(rs.getString(11));
				mAdm.setLanguage(rs.getString(10));
				mAdm.setAdmissionLocation(rs.getString(7));
				temp.add(mAdm);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MDiagnosticReport> getDiagnosticReports(MPatient mPat) {
		Connection connection = ConnectDB.getConnection();
		String query = "SELECT * FROM CXR.RECORDS WHERE SUBJECT_ID = " + mPat.getPatientSubjectId();
		PreparedStatement statement;
		ArrayList<MDiagnosticReport> temp = new ArrayList<>();
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				MDiagnosticReport dReport = new MDiagnosticReport();
				dReport.setSubjectId(rs.getString(1));
				dReport.setStudyId(rs.getString(2));
				dReport.setPath(rs.getString(3));
				dReport.setReport(rs.getString(4));
				dReport.setStudies(getImagingStudies(dReport, mPat));
				temp.add(dReport);
			}
			connection.close();
		} catch (SQLException exc) {
			exc.printStackTrace();
		}
		return temp;
	}

	private ArrayList<MImagingStudy> getImagingStudies(MDiagnosticReport diagnosticReport, MPatient mPat) {
		Connection connection = ConnectDB.getConnection();
		String query = "SELECT * FROM CXR.STUDIES WHERE SUBJECT_ID = " + mPat.getPatientSubjectId() + " AND STUDY_ID = "
				+ diagnosticReport.getStudyId();
		PreparedStatement statement;
		ArrayList<MImagingStudy> temp = new ArrayList<>();
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				MImagingStudy iStudy = new MImagingStudy();
				iStudy.setSubjectId(rs.getString(1));
				iStudy.setSubjectId(rs.getString(2));
				iStudy.setDicomId(rs.getString(3));
				iStudy.setPath(rs.getString(4));
				temp.add(iStudy);
			}
			connection.close();
		} catch (SQLException exc) {
			exc.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MDiagnose> getDiagnoses(MPatient mPat, MAdmission adm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MDiagnose> temp = new ArrayList<MDiagnose>();
		String query = "SELECT *" + "	FROM mimic_hosp.diagnoses_icd d"
				+ "   INNER JOIN mimic_hosp.d_icd_diagnoses i ON d.icd_code = i.icd_code" + "   WHERE d.subject_id = "
				+ mPat.getPatientSubjectId() + " AND d.hadm_id = " + adm.getAdmissionId()
				+ "   ORDER BY d.seq_num DESC";
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				MDiagnose mDiag = new MDiagnose();
				mDiag.setIcdCode(rs.getString(4));
				mDiag.setLongTitle(rs.getString(7));
				mDiag.setSeqNumber(rs.getInt(3));
				mDiag.setIcdVersion(rs.getString(5));
				temp.add(mDiag);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MProcedure> getProcedures(MPatient mPat, MAdmission adm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MProcedure> temp = new ArrayList<MProcedure>();
		String query = "SELECT *" + "	FROM mimic_hosp.procedures_icd p"
				+ "   INNER JOIN mimic_hosp.d_icd_procedures i ON p.icd_code = i.icd_code" + "   WHERE p.subject_id = "
				+ mPat.getPatientSubjectId() + "AND p.hadm_id = " + adm.getAdmissionId() + "   ORDER BY p.seq_num DESC";
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				MProcedure mProc = new MProcedure();
				mProc.setIcdCode(rs.getString(5));
				mProc.setLongTitle(rs.getString(9));
				mProc.setSeqNumber(rs.getInt(3));
				switch (rs.getString(6)) {
				case "9":
					mProc.setIcdVersion(MProcedure.IcdVersion.ICD9PROC);
					break;
				case "10":
					mProc.setIcdVersion(MProcedure.IcdVersion.ICD10PROC);
					break;
				}
				temp.add(mProc);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MChartevent> getChartEvents(MAdmission mAdm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MChartevent> temp = new ArrayList<MChartevent>();

		String fasterQuery = "SELECT C1.SUBJECT_ID, C1.HADM_ID, C1.CHARTTIME, C1.VALUE, C1.VALUENUM, C1.VALUEUOM, D.LABEL "
				+ "FROM (SELECT C.SUBJECT_ID, C.HADM_ID, C.CHARTTIME, C.VALUE, C.VALUENUM, C.VALUEUOM, C.ITEMID "
				+ "FROM MIMIC_ICU.CHARTEVENTS C) AS C1 "
				+ "INNER JOIN MIMIC_ICU.D_ITEMS D ON C1.ITEMID = D.ITEMID WHERE C1.HADM_ID=" + mAdm.getAdmissionId();

		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(fasterQuery);
			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				if (rs.getObject(5) != null) {
					MChartevent event = new MChartevent();
					event.setRecordDate(rs.getDate(3));
					event.setMeasurementType(rs.getString(7));
					event.setValue(rs.getString(4));
					if (rs.getObject(5) != null) {
						event.setNumValue(rs.getDouble(5));
					}
					if (rs.getObject(6) != null) {
						event.setUnit(rs.getString(6));
					}
					temp.add(event);
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MLabevent> getLabEvents(MPatient mPat, MAdmission mAdm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MLabevent> temp = new ArrayList<MLabevent>();
		String fasterQuery = "SELECT L1.SUBJECT_ID, L1.HADM_ID, L1.CHARTTIME, L1.VALUE, L1.VALUENUM, L1.VALUEUOM, L1.FLAG, D.LABEL, D.FLUID, D.LOINC_CODE, L1.LABEVENT_ID, L1.COMMENTS "
				+ "FROM (SELECT L.SUBJECT_ID, L.HADM_ID, L.CHARTTIME, L.VALUE, L.VALUENUM, L.VALUEUOM, L.FLAG, L.ITEMID, L.LABEVENT_ID, L.COMMENTS "
				+ "FROM MIMIC_HOSP.LABEVENTS L) AS L1 " + "INNER JOIN MIMIC_HOSP.D_LABITEMS D ON L1.ITEMID = D.ITEMID "
				+ "WHERE L1.SUBJECT_ID = " + mPat.getPatientSubjectId() + " AND L1.HADM_ID= " + mAdm.getAdmissionId();
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(fasterQuery);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				if (rs.getObject(4) != null) {
					MLabevent event = new MLabevent();
					event.setAcquisitionDate(rs.getDate(3));
					event.setMeasurementType(rs.getString(8));
					event.setFluid(rs.getString(9));
					if (rs.getObject(10) != null) {
						event.setLoinc(rs.getString(10));
					}
					event.setValue(rs.getString(4));
					if (rs.getObject(5) != null) {
						event.setNumValue(rs.getDouble(5));
					}
					if (rs.getObject(6) != null) {
						event.setUnit(rs.getString(6));
					}
					if (rs.getObject(7) != null && rs.getString(7) == "abnormal") {
						event.setAbnormal(true);
					}
					event.setLabeventId(rs.getInt(11));
					event.setComments(rs.getString(12));
					temp.add(event);
				}
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MPrescription> getPrescriptions(MPatient mPat, MAdmission mAdm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MPrescription> temp = new ArrayList<>();
		String query = "SELECT * " + "FROM MIMIC_HOSP.PRESCRIPTIONS " + "WHERE SUBJECT_ID = "
				+ mPat.getPatientSubjectId() + " AND HADM_ID= " + mAdm.getAdmissionId();
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				MPrescription pres = new MPrescription();
				pres.setStart(rs.getDate(4));
				pres.setEnd(rs.getDate(5));
				pres.setDrugtype(rs.getString(6));
				pres.setDrug(rs.getString(7));
				pres.setGsn(rs.getString(8));
				pres.setNdc(rs.getString(9));
				pres.setProdStrength(rs.getString(10));
				pres.setDoseValRx(rs.getString(12));
				pres.setDoseUnitRx(rs.getString(13));
				pres.setFormValDisp(rs.getString(14));
				pres.setFormUnitDisp(rs.getString(15));
				pres.setRoute(rs.getString(17));
				temp.add(pres);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}

	public ArrayList<MTransfer> getTransfers(MPatient mPat, MAdmission mAdm) {
		Connection connection = ConnectDB.getConnection();
		ArrayList<MTransfer> temp = new ArrayList<MTransfer>();
		String query = "SELECT * " + "FROM MIMIC_CORE.TRANSFERS " + "WHERE SUBJECT_ID = " + mPat.getPatientSubjectId()
				+ " AND HADM_ID= " + mAdm.getAdmissionId();
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			int index = 0;
			while (rs.next()) {
				index++;
				MTransfer t = new MTransfer();
				t.setTransferId(rs.getString(3) + "-" + index);
				t.setEventType(rs.getString(4));
				t.setCareUnit(rs.getString(5));
				t.setIntime(rs.getDate(6));
				t.setOuttime(rs.getDate(7));
				temp.add(t);
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}
}
