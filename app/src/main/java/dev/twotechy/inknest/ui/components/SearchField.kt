package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SearchField(
    initialQuery: String,
    placeholder: String,
    focus: FocusRequester,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
) {
    // Editing/selection must update synchronously; derived search results may arrive later.
    // Closing search disposes this field; a new session starts from the ViewModel's saved query.
    var editing by remember {
        mutableStateOf(TextFieldValue(initialQuery, TextRange(initialQuery.length)))
    }
    val colors = MaterialTheme.colorScheme
    val border = InkBorder.default
    Row(
        Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.minHeight).drawBehind {
            drawLine(
                colors.outlineVariant,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                strokeWidth = border.toPx(),
            )
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.padding(end = InkSpacing.sm).size(InkHeaderTokens.iconSize),
        )
        BasicTextField(
            value = editing,
            onValueChange = {
                val textChanged = editing.text != it.text
                editing = it
                if (textChanged) onQuery(it.text)
            },
            modifier = Modifier.weight(
                1f,
            ).heightIn(min = InkHeaderTokens.touchTarget).focusRequester(focus),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.onSurface),
            singleLine = true,
            decorationBox = { field ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (editing.text.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    field()
                }
            },
        )
        HeaderIconButton(R.drawable.ic_close, stringResource(R.string.close_search), onClose)
    }
}
