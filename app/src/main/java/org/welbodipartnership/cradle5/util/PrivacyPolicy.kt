package org.welbodipartnership.cradle5.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.welbodipartnership.cradle5.BuildConfig

fun Context.launchPrivacyPolicyWebIntent() {
  startActivity(
    Intent(Intent.ACTION_VIEW)
      .setData(Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
  )
}