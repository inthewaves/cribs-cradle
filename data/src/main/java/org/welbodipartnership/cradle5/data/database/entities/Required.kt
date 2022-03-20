package org.welbodipartnership.cradle5.data.database.entities

/**
 * Indicates that a field is required even if it is nullable. Nullable is meant to accomodate for
 * draft state, so any properties marked as required is null iff draft is true.
 */
@MustBeDocumented
annotation class Required()
