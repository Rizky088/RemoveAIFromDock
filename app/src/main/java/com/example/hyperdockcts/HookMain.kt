package com.example.hyperdockcts

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.miui.securitycenter") return

        try {
            val sidebarColumn = XposedHelpers.findClassIfExists(
                "com.miui.gamebooster.windowmanager.newbox.p0",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(
                sidebarColumn,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val root = param.thisObject as? View ?: return
                        root.post {
                            XposedBridge.log("HyperDockCTS: --- p0 attached ---")
                            scanAndUnhide(root, 0)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private fun scanAndUnhide(view: View, depth: Int) {
        if (depth > 6) return
        val idName = try {
            if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else "no-id"
        } catch (_: Throwable) {
            view.id.toString()
        }

        if (idName != "no-id") {
            XposedBridge.log("HyperDockCTS: ${"  ".repeat(depth)}${view.javaClass.simpleName} id=$idName vis=${view.visibility}")
        }

        // Automatically unhide Circle to Search if it's present but hidden
        val lower = idName.lowercase()
        if ((lower.contains("cts") || lower.contains("circle") || lower.contains("search")) && view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
            XposedBridge.log("HyperDockCTS: Forced VISIBLE on $idName")
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanAndUnhide(view.getChildAt(i), depth + 1)
            }
        }
    }
}
