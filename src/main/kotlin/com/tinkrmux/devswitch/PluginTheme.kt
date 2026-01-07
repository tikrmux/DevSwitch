package com.tinkrmux.devswitch

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.intellij.ui.JBColor
import java.awt.Color as JColor

fun JColor.toComposeColor() =
    Color(this.red, this.green, this.blue, this.alpha)

val primaryColor: Color
    get() = JBColor.gray.toComposeColor()

val backgroundColor: Color
    get() = JBColor.background().toComposeColor()

val surfaceColor: Color
    get() = JBColor.foreground().toComposeColor()


private val DarkColorPalette = darkColors(
    primary = primaryColor,
//    primaryVariant = Color(0xFF3700B3),
//    secondary = Color(0xFF03DAC6),
    background = backgroundColor,
//    surface = Color(0xFF1E1E1E),
//    onPrimary = Color.White,
//    onSecondary = Color.Black,
//    onBackground = Color.White,
//    onSurface = Color.White
)

private val LightColorPalette = lightColors(
    primary = primaryColor,
//    primaryVariant = Color(0xFF3700B3),
//    secondary = Color(0xFF03DAC6),
    background = backgroundColor,
//    surface = Color(0xFFFFFFFF),
//    onPrimary = Color.White,
//    onSecondary = Color.Black,
//    onBackground = Color.Black,
//    onSurface = Color.Black
)

const val studioFontSize: Float = 12f

val typography = Typography(
    subtitle1 = TextStyle(fontSize = studioFontSize.sp),
    subtitle2 = TextStyle(fontSize = studioFontSize.sp),
    button = TextStyle(fontSize = studioFontSize.sp),
    caption = TextStyle(fontSize = studioFontSize.sp),
    overline = TextStyle(fontSize = studioFontSize.sp),
    body1 = TextStyle(fontSize = studioFontSize.sp),
    body2 = TextStyle(fontSize = studioFontSize.sp),
    h1 = TextStyle(fontSize = studioFontSize.sp),
    h2 = TextStyle(fontSize = studioFontSize.sp),
    h3 = TextStyle(fontSize = studioFontSize.sp),
    h4 = TextStyle(fontSize = studioFontSize.sp),
    h5 = TextStyle(fontSize = studioFontSize.sp),
    h6 = TextStyle(fontSize = studioFontSize.sp),
)

@Composable
fun QuickSettingsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = Shapes(),
        content = content
    )
}
