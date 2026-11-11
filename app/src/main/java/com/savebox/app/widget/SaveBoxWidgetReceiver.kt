package com.savebox.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Android talks to widgets through a "receiver" declared in AndroidManifest.xml.
 * Glance does all the work; we only say which widget this receiver is for.
 */
class SaveBoxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SaveBoxWidget()
}
