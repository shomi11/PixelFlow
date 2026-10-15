package com.vpmapps.pixelflow.ui.screens

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vpmapps.pixelflow.ui.components.RoutineEditor
import com.vpmapps.pixelflow.ui.viewmodel.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    routineId: Long? = null,
    viewModel: RoutineViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val routines by viewModel.routines.collectAsState()
    val existingRoutine = remember(routineId, routines) {
        if (routineId != null && routineId > 0) {
            routines.find { it.id == routineId }
        } else null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (existingRoutine != null) "Edit Routine" else "New Routine",
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        RoutineEditor(
            initialRoutine = existingRoutine,
            onSave = { routine ->
                viewModel.saveRoutine(routine) {
                    onNavigateBack()
                }
            },
            onCancel = onNavigateBack,
            // Keep the pinned Save bar above the keyboard while typing
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        )
    }
}
