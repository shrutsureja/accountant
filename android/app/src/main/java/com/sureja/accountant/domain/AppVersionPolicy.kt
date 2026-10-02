package com.sureja.accountant.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionPolicy(
    val minimumSupportedVersionCode: Int,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val message: String = "Contact Shrut for the updated Accountant APK.",
) {
    fun isValid() = minimumSupportedVersionCode > 0 && latestVersionCode >= minimumSupportedVersionCode && latestVersionName.isNotBlank()
}

enum class UpdateRequirement { NONE, OPTIONAL, REQUIRED }
fun updateRequirement(policy: AppVersionPolicy?, installedCode: Int): UpdateRequirement = when {
    policy == null || !policy.isValid() -> UpdateRequirement.NONE
    installedCode < policy.minimumSupportedVersionCode -> UpdateRequirement.REQUIRED
    installedCode < policy.latestVersionCode -> UpdateRequirement.OPTIONAL
    else -> UpdateRequirement.NONE
}
