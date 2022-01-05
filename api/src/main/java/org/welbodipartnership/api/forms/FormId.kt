package org.welbodipartnership.api.forms

/**
 * Represents the ID of a form on CTF.
 */
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class FormId(val id: Int)
