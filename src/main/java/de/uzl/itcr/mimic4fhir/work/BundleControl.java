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
package de.uzl.itcr.mimic4fhir.work;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;

/**
 * Handles bundle operations
 * @author Stefanie Ververs
 *
 */
public class BundleControl {
	private Bundle transactionBundle;
	private int numberOfResources = 0;
	private int internalBundleNumber = 0;
	
	/**
	 * creates a new transaction bundle
	 */
	public BundleControl() {
		//new Bundle
		transactionBundle = new Bundle();
		transactionBundle.setType(BundleType.TRANSACTION);
		internalBundleNumber = 1;
	}	
	
	/**
	 * Number of resources currently present in bundle
	 * @return number of resources
	 */
	public int getNumberOfResources() {
		return numberOfResources;
	}
	
	/**
	 * Internal bundle number (how often bundle "reset"?) 
	 * @return internal bundle number
	 */
	public int getInternalBundleNumber() {
		return internalBundleNumber;
	}
	
	/**
	 * Reset internal bundle number to 1
	 */
	public void resetInternalBundleNumber() {
		internalBundleNumber = 1;
	}

	/**
	 * Reset bundle for "new" bundle with zero resources
	 */
	public void resetBundle() {
		transactionBundle = new Bundle();
		transactionBundle.setType(BundleType.TRANSACTION);
		numberOfResources = 0;
		internalBundleNumber++;
	}
	
	/**
	 * Get the current bundle
	 * @return current bundle
	 */
	public Bundle getTransactionBundle() {
		return transactionBundle;
	}

	/**
	 * Add fhir resource without UUID to current bundle
	 * @param rToAdd fhir-resource to add
	 */
	public void addResourceToBundle(Resource rToAdd)
	{		
		transactionBundle.addEntry()
		   .setResource(rToAdd)
		   .getRequest()
		      .setUrl(rToAdd.fhirType())
		      .setMethod(HTTPVerb.POST);
		
		numberOfResources++;

	}
	
	/**
	 * Add fhir resource with UUID to current bundle
	 * @param rToAdd fhir-resource to add
	 */
	public void addUUIDResourceToBundle(Resource rToAdd){
		transactionBundle.addEntry()
		   .setFullUrl(rToAdd.getId())
		   .setResource(rToAdd)
		   .getRequest()
		      .setUrl(rToAdd.fhirType())
		      .setMethod(HTTPVerb.POST);
		
		numberOfResources++;
		
	}
	
	/**
	 * Conditional Create:
	 * Add fhir resource with UUID to current bundle and set condition (create if none exist)
	 * @param rToAdd fhir-resource to add
	 * @param condition search-condition to match 
	 */
	public void addUUIDResourceWithConditionToBundle(Resource rToAdd, String condition) {
		transactionBundle.addEntry()
		   .setFullUrl(rToAdd.getId())
		   .setResource(rToAdd)
		   .getRequest()
		      .setUrl(rToAdd.fhirType())
		      .setIfNoneExist(condition)
		      .setMethod(HTTPVerb.POST);
		
		numberOfResources++;
	}
}
