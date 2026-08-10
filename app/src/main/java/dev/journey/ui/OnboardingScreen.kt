package dev.journey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.domain.Journey

/**
 * Everything the app needs before it can start: a height, and permission to read steps.
 *
 * Height is asked once and used for nothing else. Health Connect writes steps and no distance at
 * all (ADR-0007), so stride is the only route to a distance figure.
 */
@Composable
fun OnboardingScreen(
    journey: Journey,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onBegin: (heightCm: Int) -> Unit,
) {
    var height by remember { mutableStateOf("") }
    val cm = height.toIntOrNull()
    val heightOk = cm != null && cm in 100..250

    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 64.dp)
        ) {
            Text(journey.name, color = Behind, fontSize = 32.sp, fontWeight = FontWeight.Medium)
            Text(journey.subtitle, color = Ahead, fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))
            Text(
                "Every kilometre you walk moves you along this route. Progress starts at zero " +
                    "today — nothing you have already walked counts, because Health Connect only " +
                    "begins recording once you allow it to.",
                color = Body,
                fontSize = 16.sp,
                lineHeight = 26.sp,
            )

            Spacer(Modifier.height(40.dp))
            Text("Your height", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Used to estimate your stride, which is how steps become distance. It is the only " +
                    "thing about you the app stores.",
                color = Ahead,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = height,
                onValueChange = { height = it.filter(Char::isDigit).take(3) },
                label = { Text("cm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(32.dp))
            if (!permissionsGranted) {
                Text(
                    "Allow step access",
                    color = Here,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestPermissions)
                        .padding(vertical = 14.dp),
                )
                Text(
                    "Background access is included so the app can tell you when you arrive " +
                        "somewhere without you having to check.",
                    color = Ahead,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            } else {
                Text(
                    if (heightOk) "Begin" else "Enter your height to begin",
                    color = if (heightOk) Here else Ahead,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (heightOk) Modifier.clickable { onBegin(cm!!) } else Modifier)
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}
