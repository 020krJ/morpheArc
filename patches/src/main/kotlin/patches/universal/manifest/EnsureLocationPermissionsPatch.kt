package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
private const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"

/**
 * Adds location permission declarations so applications can request
 * access to the device's location on Android.
 */
@Suppress("unused")
val ensureLocationPermissionsPatch = resourcePatch(
    name = "Ensure Location Permissions",
    description = "Adds location permissions to the Android manifest.",
    default = false,
) {
    category("Manifest")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val requiredPermissions = setOf(
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
        )
        var added = 0

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use
            val declaredPermissions = mutableSetOf<String>()

            for (tagName in listOf("uses-permission", "uses-permission-sdk-23")) {
                val permissions = manifest.getElementsByTagName(tagName)
                for (i in 0 until permissions.length) {
                    val permission = permissions.item(i) as? Element ?: continue
                    val name = permission.getAttributeNS(ANDROID_NAMESPACE, "name")
                    if (name.isNotEmpty()) declaredPermissions += name
                }
            }

            for (permissionName in requiredPermissions) {
                if (permissionName in declaredPermissions) continue

                val permission = manifest.createElement("uses-permission")
                permission.setAttributeNS(
                    ANDROID_NAMESPACE,
                    "android:name",
                    permissionName,
                )

                val application = root.getElementsByTagName("application").item(0)
                if (application != null) {
                    root.insertBefore(permission, application)
                } else {
                    root.appendChild(permission)
                }

                added++
                logger.info("Added missing $permissionName permission")
            }
        }

        if (added > 0) {
            logger.info("Ensured $added location permission declaration(s)")
        } else {
            logger.info("Location permissions already declared. No changes applied.")
        }
    }
}
