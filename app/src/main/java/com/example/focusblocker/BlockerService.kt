package com.example.focusblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == this.packageName || packageName.contains("systemui")) return

            val prefs = getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
            val isBlockerActive = prefs.getBoolean("is_active", false)
            if (!isBlockerActive) return

            val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

            if (blockedApps.contains(packageName)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {}
}
