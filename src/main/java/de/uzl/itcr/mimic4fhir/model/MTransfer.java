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

/**
 * Represents one row in mimiciii.transfers
 * @author Stefanie Ververs
 *
 */
public class MTransfer {
	private String eventType;
	private String careUnit;
	private Date intime;
	private Date outtime;
	private String transferId;

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getCareUnit() {
		return careUnit;
	}

	public void setCareUnit(String careUnit) {
		this.careUnit = careUnit;
	}

	public Date getIntime() {
		return intime;
	}

	public void setIntime(Date intime) {
		this.intime = intime;
	}

	public Date getOuttime() {
		return outtime;
	}

	public void setOuttime(Date outtime) {
		this.outtime = outtime;
	}

	public String getTransferId() {
		return transferId;
	}

	public void setTransferId(String transferId) {
		this.transferId = transferId;
	}
}
