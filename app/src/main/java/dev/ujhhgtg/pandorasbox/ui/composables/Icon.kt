package dev.ujhhgtg.pandorasbox.ui.composables

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.ujhhgtg.pandorasbox.utils.tooltip

@Composable
fun Icon(
    @DrawableRes painter: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        painter = painterResource(painter),
        contentDescription = contentDescription,
        modifier = modifier.also { if (contentDescription != null) it.tooltip(contentDescription) },
        tint = tint
    )
}

@Composable
fun Icon(
    @DrawableRes painter: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        painter = painterResource(painter),
        contentDescription = stringResource(contentDescription),
        modifier = modifier.also { it.tooltip(contentDescription) },
        tint = tint
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.also { if (contentDescription != null) it.tooltip(contentDescription) },
        tint = tint
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = stringResource(contentDescription),
        modifier = modifier.also { it.tooltip(contentDescription) },
        tint = tint
    )
}