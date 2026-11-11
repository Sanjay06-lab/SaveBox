package com.savebox.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.savebox.app.SaveBoxApplication
import com.savebox.app.navigation.DeepLinks
import com.savebox.app.ui.theme.DarkColors
import com.savebox.app.ui.theme.LightColors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The SaveBox home-screen widget, built with Jetpack Glance
 * (Compose-like code that Android turns into a widget).
 *
 * Shows: total saved, one goal's name, its progress bar and percentage, and what's left.
 * Tapping it opens that goal in the app.
 *
 * The widget is refreshed by AppDataObserver whenever savings change.
 */
class SaveBoxWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as SaveBoxApplication).container

        // Live data: goals + settings, turned into ready-to-show text.
        val dataFlow = combine(
            container.savingsRepository.observeGoals(),
            container.userPreferencesRepository.userPreferences,
        ) { goals, preferences ->
            WidgetDataBuilder.build(goals, preferences.widgetGoalId, preferences.currency)
        }
        // Load the first value BEFORE drawing, so the widget never shows an empty state.
        val initialData = dataFlow.first()

        provideContent {
            // Glance keeps this widget "session" alive for a while. Collecting the flow here
            // means every change is drawn immediately, not just the data from when it started.
            val data by dataFlow.collectAsState(initial = initialData)

            // Same colours as the app, light and dark.
            GlanceTheme(colors = ColorProviders(light = LightColors, dark = DarkColors)) {
                WidgetContent(context = context, data = data)
            }
        }
    }

    companion object {
        /** Redraw every SaveBox widget on the home screen (does nothing if there are none). */
        suspend fun refreshAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            if (manager.getGlanceIds(SaveBoxWidget::class.java).isNotEmpty()) {
                SaveBoxWidget().updateAll(context)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, data: WidgetData) {
    val colors = GlanceTheme.colors
    val goal = data.goal

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.surface)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(DeepLinks.openApp(context, goal?.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "SaveBox · Total saved",
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Text(
            text = data.totalSavedText,
            style = TextStyle(color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold),
        )

        Spacer(GlanceModifier.height(10.dp))

        if (goal == null) {
            Text(
                text = "Tap to create your first goal",
                style = TextStyle(color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.title,
                    maxLines = 1,
                    style = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = "${goal.percent}%",
                    style = TextStyle(color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = goal.progress,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                color = colors.primary,
                backgroundColor = colors.primaryContainer,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = if (goal.isCompleted) "Goal reached 🎉" else "${goal.remainingText} to go",
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }
}
