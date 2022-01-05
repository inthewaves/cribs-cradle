package org.welbodipartnership.api.forms

/**
 * Represents operation ID to use in a POST request in order to "Save" a form on the database.
 */
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class PostOperationId(val id: Int)
