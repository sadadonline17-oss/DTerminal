package dedeadend.dterminal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
val terminalSuccessTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    lineHeight = 18.sp
)
val terminalErrorTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    color = ErrorTextColor,
    lineHeight = 18.sp
)

val terminalInfoTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    color = InfoTextColor,
    lineHeight = 18.sp
)