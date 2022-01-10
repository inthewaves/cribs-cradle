package org.welbodipartnership.cradle5.data.verification

/**
 * Indicates classes will nullable properties that still want to indicate whether
 * certain fields are required or not.
 */
interface HasRequiredFields {
  fun requiredFieldsPresent(): Boolean
}
