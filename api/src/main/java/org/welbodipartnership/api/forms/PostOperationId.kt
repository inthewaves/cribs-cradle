package org.welbodipartnership.api.forms

/**
 * Represents operation ID to use in a POST request in order to "Save" a form on the database.
 * While we could read the Meta fields and dynamically load it from there, this seems better for now
 */
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class PostOperationId(val id: Int)
