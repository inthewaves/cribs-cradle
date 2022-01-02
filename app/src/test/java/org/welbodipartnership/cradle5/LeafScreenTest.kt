package org.welbodipartnership.cradle5

import org.junit.Test

internal class LeafScreenTest {
  @Test
  fun testMe() {
    assert(LeafScreen.PatientEdit.doesRouteMatch("patients/edit/50"))
    assert(LeafScreen.PatientEdit.doesRouteMatch("patients/patients/edit/50"))
    assert(!LeafScreen.PatientEdit.doesRouteMatch("patients/new/50"))
    assert(!LeafScreen.PatientCreate.doesRouteMatch("patients/new/50"))
    assert(LeafScreen.Patients.doesRouteMatch("patients"))
    assert(!LeafScreen.Patients.doesRouteMatch("patients/new/50"))
    assert(!LeafScreen.PatientDetails.doesRouteMatch("patients/new/50"))
  }
}
