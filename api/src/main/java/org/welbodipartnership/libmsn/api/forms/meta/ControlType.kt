package org.welbodipartnership.libmsn.api.forms.meta

enum class ControlType {
  Null,
  /** A link to another endpoint/URL */
  Link,
  /** Text data (read only) */
  Text,
  /**
   * A single value from list of possible values (e.g. dropdown, radio buttons, mutually exclusive
   * checkbox lists)
   */
  SingleValueFromList,
  /** A single value from a dynamic list */
  SingleValueFromDynamicList,
  /** one or more values from a list of possible values (e.g. checkbox list) */
  MultiValueFromList,
  /** A number, text, date, or bool for data entry (e.g. textbox, checkbox) */
  DataInput,
  FileUpload
}