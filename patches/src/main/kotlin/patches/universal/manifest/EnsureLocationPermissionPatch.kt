package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val ACCESS_FINE_LOCATION =
    "android.permission.ACCESS_FINE_LOCATION"

private const val ACCESS_COARSE_LOCATION =
    "android.permission.ACCESS_COARSE_LOCATION"

@Suppress("unused")
val ensureLocationPermissionPatch = resourcePatch(
    name = "Ensure Location Permission",
    description = "Adds location permissions to the app manifest.",
    default = false,
) {
    category("Manifest")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var added = false

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
                ?: return@use

            val permissions = setOf(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
            )

            for (permissionName in permissions) {
                var exists = false
                val nodes = manifest.getElementsByTagName("uses-permission")

                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element
                        ?: continue

                    if (
                        element.getAttributeNS(NS_ANDROID, "name") ==
                        permissionName
                    ) {
                        exists = true
                        break
                    }
                }

                if (!exists) {
                    val element = manifest.createElement("uses-permission")

                    element.setAttributeNS(
                        NS_ANDROID,
                        "android:name",
                        permissionName,
                    )

                    // Keep uses-permission before the application element.
                    val application = root.getElementsByTagName("application").item(0)
                    root.insertBefore(element, application)
                    added = true
                }
            }
        }

        if (added) {
            logger.info("Added location permissions")
        } else {
            logger.info(
                "Location permissions already declared. No changes applied."
            )
        }
    }
}
