package patches.universal.permissions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val CHROME_CLIENT = "LD6/n0;"
private const val BROWSER_ACTIVITY = "Lcompany/thebrowser/arc/activity/BrowserActivity;"
private const val EXTENSION = "Lapp/template/extension/extension/ArcGeolocationPrompt;"
private const val GEO_CALLBACK = "Landroid/webkit/GeolocationPermissions\$Callback;"
private const val STRING = "Ljava/lang/String;"
private const val ACTIVITY = "Landroid/app/Activity;"
private const val STRING_ARRAY = "[Ljava/lang/String;"
private const val INT_ARRAY = "[I"

@Suppress("unused")
val arcGeolocationPromptPatch = bytecodePatch(
    name = "Arc Search Website Location Prompt",
    description = "Adds a website location prompt and connects it to Android runtime location permission.",
    default = false,
) {
    extendWith("extensions/extension.mpe")

    execute {
        val chrome = mutableClassDefBy(classDefBy(CHROME_CLIENT))
        check(chrome.methods.none {
            it.name == "onGeolocationPermissionsShowPrompt" &&
                it.parameterTypes.map { type -> type.toString() } == listOf(STRING, GEO_CALLBACK)
        }) {
            "D6.n0 already defines onGeolocationPermissionsShowPrompt; duplicate method avoided."
        }

        chrome.methods.add(
            ImmutableMethod(
                CHROME_CLIENT,
                "onGeolocationPermissionsShowPrompt",
                listOf(
                    ImmutableMethodParameter(STRING, null, "origin"),
                    ImmutableMethodParameter(GEO_CALLBACK, null, "callback"),
                ),
                "V",
                AccessFlags.PUBLIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                    invoke-static { p1, p2 }, $EXTENSION->showPrompt(Ljava/lang/String;$GEO_CALLBACK)V
                    return-void
                    """,
                )
            },
        )

        val activity = mutableClassDefBy(classDefBy(BROWSER_ACTIVITY))
        val onResume = activity.methods.firstOrNull {
            it.name == "onResume" && it.parameterTypes.isEmpty() && it.returnType == "V"
        }
        if (onResume != null) {
            onResume.addInstructions(0, "invoke-static { p0 }, $EXTENSION->setActivity($ACTIVITY)V")
        } else {
            val superClass = classDefBy(BROWSER_ACTIVITY).superclass
                ?: error("BrowserActivity superclass not found.")
            activity.methods.add(
                ImmutableMethod(
                    BROWSER_ACTIVITY,
                    "onResume",
                    emptyList(),
                    "V",
                    AccessFlags.PROTECTED.value,
                    null,
                    null,
                    MutableMethodImplementation(1),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                        invoke-super { p0 }, $superClass->onResume()V
                        invoke-static { p0 }, $EXTENSION->setActivity($ACTIVITY)V
                        return-void
                        """,
                    )
                },
            )
        }

        val permissionResult = activity.methods.firstOrNull {
            it.name == "onRequestPermissionsResult" &&
                it.parameterTypes.map { type -> type.toString() } == listOf("I", STRING_ARRAY, INT_ARRAY) &&
                it.returnType == "V"
        }
        if (permissionResult != null) {
            permissionResult.addInstructions(
                0,
                "invoke-static { p1, p2, p3 }, $EXTENSION->onRequestPermissionsResult(I$STRING_ARRAY$INT_ARRAY)V",
            )
        } else {
            val superClass = classDefBy(BROWSER_ACTIVITY).superclass
                ?: error("BrowserActivity superclass not found.")
            activity.methods.add(
                ImmutableMethod(
                    BROWSER_ACTIVITY,
                    "onRequestPermissionsResult",
                    listOf(
                        ImmutableMethodParameter("I", null, "requestCode"),
                        ImmutableMethodParameter(STRING_ARRAY, null, "permissions"),
                        ImmutableMethodParameter(INT_ARRAY, null, "grantResults"),
                    ),
                    "V",
                    AccessFlags.PROTECTED.value,
                    null,
                    null,
                    MutableMethodImplementation(4),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                        invoke-super { p0, p1, p2, p3 }, $superClass->onRequestPermissionsResult(I$STRING_ARRAY$INT_ARRAY)V
                        invoke-static { p1, p2, p3 }, $EXTENSION->onRequestPermissionsResult(I$STRING_ARRAY$INT_ARRAY)V
                        return-void
                        """,
                    )
                },
            )
        }
    }
}
