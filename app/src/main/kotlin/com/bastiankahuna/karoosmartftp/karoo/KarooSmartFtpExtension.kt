package com.bastiankahuna.karoosmartftp.karoo

import com.bastiankahuna.karoosmartftp.SettingsStore
import io.hammerhead.karooext.extension.KarooExtension

class KarooSmartFtpExtension : KarooExtension(EXTENSION_ID, EXTENSION_VERSION) {
    override val types = listOf(SmartFtpDataType(EXTENSION_ID))

    override fun onBonusAction(actionId: String) {
        val store = SettingsStore(this)
        when (actionId) {
            ACTION_RESET -> store.resetWorkout()
            ACTION_NEXT -> store.skipRequestedSegment()
        }
    }

    companion object {
        const val EXTENSION_ID = "smart_ftp"
        const val EXTENSION_VERSION = "1.1.4"
        const val FIELD_TYPE_ID = "smart_ftp_field"
        const val ACTION_RESET = "reset_workout"
        const val ACTION_NEXT = "next_segment"
    }
}
