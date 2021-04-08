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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.util.LinkedList;

public class TimeMeasurements {

    private static TimeMeasurements instance;

    private final LinkedList<String> timingTexts;

    int rowId = 0;

    private TimeMeasurements() {
        this.timingTexts = new LinkedList<>();
    }

    synchronized void addTiming(int jobNumber, String patientSubjectId, int numberAdmissions, long timeMillis) {
        this.timingTexts.add(String.format("%d,%d,%s,%d,%d", ++rowId, jobNumber + 1, patientSubjectId, numberAdmissions, timeMillis));
    }

    public void writeToFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("thread-timings.csv"));
            writer.write("row_id,job_id,subject_id,number_admissions,time_milliseconds");
            writer.newLine();
            for (String timingText : timingTexts) {
                writer.write(timingText);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Wrote timings to thread-timings.csv");
    }

    public static TimeMeasurements getInstance() {
        if (instance == null) {
            instance = new TimeMeasurements();
        }
        return instance;
    }

}
