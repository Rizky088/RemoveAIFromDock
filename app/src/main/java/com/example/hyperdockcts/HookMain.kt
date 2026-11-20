package com.example.hyperdockcts

import android.content.ContentResolver
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "com.miui.home" -> hookHideDockAi(lpparam)
            "com.miui.securitycenter" -> hookSidebarCtsProbe(lpparam)
        }
    }

    private fun hookHideDockAi(lpparam: XC_LoadPackage.LoadPackageParam) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args.size >= 2 && param.args[1] == "system_ai_assistant") {
                    param.result = "none"
                }
            }
        }
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure::class.java,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                hook
            )
            XposedHelpers.findAndHookMethod(
                Settings.System::class.java,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                hook
            )
            XposedBridge.log("HyperDockCTS: Dock AI hook active in com.miui.home")
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private fun hookSidebarCtsProbe(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val turboLayout = XposedHelpers.findClassIfExists(
                "com.miui.gamebooster.windowmanager.newbox.TurboLayout",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(
                turboLayout,
                "getTargetBox",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val root = param.result as? View ?: return
                        XposedBridge.log("HyperDockCTS: getTargetBox -> ${root.javaClass.name}")
                        dumpViews(root, 0)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    private fun dumpViews(view: View, depth: Int) {
        if (depth > 4) return
        val idName = try {
            if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else "no-id"
        } catch (_: Throwable) {
            view.id.toString()
        }
        if (idName != "no-id" || view.visibility != View.VISIBLE) {
            XposedBridge.log("HyperDockCTS: ${"  ".repeat(depth)}${view.javaClass.simpleName} id=$idName vis=${view.visibility}")
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                dumpViews(view.getChildAt(i), depth + 1)
            }
        }
    }
}
