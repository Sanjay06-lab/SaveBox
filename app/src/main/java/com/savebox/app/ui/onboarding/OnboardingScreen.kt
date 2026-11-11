package com.savebox.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.savebox.app.R
import com.savebox.app.ui.components.EmojiBadge
import com.savebox.app.ui.theme.SaveBoxTheme
import com.savebox.app.ui.theme.spacing

/**
 * First-launch introduction. Shown once; the choice is saved in DataStore.
 *
 * @param onGetStarted user tapped "Get started" (we then open "Create goal").
 * @param onSkip user tapped "Skip" (we go to the dashboard).
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MaterialTheme.spacing.screenPadding),
    ) {
        // Skip button, top right.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }

        // Scrollable middle part, so small phones and large font sizes still fit.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(MaterialTheme.spacing.large))
            SaveBoxLogo()
            Spacer(Modifier.height(MaterialTheme.spacing.large))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = "Set a goal, record every rupee you put aside, and watch your progress grow.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )

            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))

            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                FeatureRow("🎯", "Set your goals", "A phone, a trip, an emergency fund - any target you like.")
                FeatureRow("🧱", "Fill your boxes", "Tick ₹100, ₹200 or ₹500 boxes, or add any amount.")
                FeatureRow("📈", "Watch it grow", "See exactly how much is saved and how much is left.")
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))

            // Honest disclaimer: SaveBox is a tracker, not a bank.
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.widthIn(max = 420.dp),
            ) {
                Text(
                    text = "🔒 SaveBox only records the amounts you enter. It never holds, moves or checks real money. " +
                        "No account, no internet - your data stays on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                )
            }
            Spacer(Modifier.height(MaterialTheme.spacing.large))
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.medium)
                .heightIn(min = 56.dp),
        ) {
            Text("Get started", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** The SaveBox logo: the launcher icon drawing inside a green circle. */
@Composable
private fun SaveBoxLogo() {
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "SaveBox logo",
            modifier = Modifier.size(140.dp),
        )
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmojiBadge(emoji = emoji)
        Spacer(Modifier.width(MaterialTheme.spacing.medium))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    SaveBoxTheme {
        OnboardingScreen(onGetStarted = {}, onSkip = {})
    }
}
