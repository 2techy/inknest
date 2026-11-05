package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius

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
    OutlinedTextField(
        value = editing,
        onValueChange = {
            val textChanged = editing.text != it.text
            editing = it
            if (textChanged) onQuery(it.text)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.minHeight)
            .border(
                InkBorder.default,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(InkRadius.medium),
            ).focusRequester(focus),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(InkRadius.medium),
        trailingIcon = {
            HeaderIconButton(R.drawable.ic_close, stringResource(R.string.close_search), onClose)
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}
