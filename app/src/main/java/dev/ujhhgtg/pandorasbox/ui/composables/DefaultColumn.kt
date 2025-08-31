package dev.ujhhgtg.pandorasbox.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultColumn(scrollBehavior: TopAppBarScrollBehavior?, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .apply {
                if (scrollBehavior != null) {
                    nestedScroll(scrollBehavior.nestedScrollConnection)
                    verticalScroll(rememberScrollState())
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}
