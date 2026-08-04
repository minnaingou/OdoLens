package com.mndublo.odolens.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collect

/**
 * Tracks whether a scroll-dependent floating action button should be visible:
 * hidden while scrolling down, shown again while scrolling up (10px hysteresis).
 * Shared by the Dashboard and Parking screens.
 */
@Composable
fun rememberFabVisibility(listState: LazyListState): State<Boolean> {
    val isFabVisible = remember { mutableStateOf(true) }
    var previousIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset + 10)) {
                    isFabVisible.value = false
                } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset - 10)) {
                    isFabVisible.value = true
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    return isFabVisible
}
