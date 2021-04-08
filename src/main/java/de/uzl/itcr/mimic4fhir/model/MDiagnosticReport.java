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
package de.uzl.itcr.mimic4fhir.model;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;

public class MDiagnosticReport {

    private String subjectId;
    private String studyId;
    private String path;
    private String report;
    private List<MImagingStudy> studies;

    private DiagnosticReport diagnosticReport;

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getStudyId() {
        return studyId;
    }

    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public List<MImagingStudy> getStudies() {
        return studies;
    }

    public void setStudies(List<MImagingStudy> studies) {
        this.studies = studies;
    }

    public DiagnosticReport getDiagnosticReport() {
        return diagnosticReport;
    }

    public void setDiagnosticReport(DiagnosticReport diagnosticReport) {
        this.diagnosticReport = diagnosticReport;
    }

}
