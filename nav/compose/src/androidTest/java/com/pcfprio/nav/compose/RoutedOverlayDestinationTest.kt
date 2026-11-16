package com.pcfprio.nav.compose

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pcfprio.nav.NavOverlayAction
import com.pcfprio.nav.NavOverlayActionId
import com.pcfprio.nav.NavOverlayActionType
import com.pcfprio.nav.NavOverlayRoute
import com.pcfprio.nav.NavOverlayRouteArguments
import com.pcfprio.nav.NavPresentation
import com.pcfprio.nav.parse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutedOverlayDestinationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun generatedSheetRouteRendersReturnsActionAndDismisses() {
        val route =
            NavOverlayRoute.Generated(
                presentation = NavPresentation.SHEET,
                title = "Generated sheet",
                message = "Generated message",
                actions =
                    listOf(
                        NavOverlayAction(
                            id = NavOverlayActionId("confirm"),
                            type = NavOverlayActionType.PRIMARY,
                            title = "Confirm",
                        ),
                    ),
            ).toNavRoute()
        var selectedActionId: NavOverlayActionId? = null
        var capturedArguments: Map<String, String>? = null

        composeRule.setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val currentEntry =
                    navController.currentBackStackEntryAsState().value
                SideEffect {
                    if (
                        currentEntry?.destination?.route?.startsWith("sheet") == true
                    ) {
                        capturedArguments =
                            listOf(
                                NavOverlayRouteArguments.ID,
                                NavOverlayRouteArguments.TITLE,
                                NavOverlayRouteArguments.MESSAGE,
                                NavOverlayRouteArguments.ACTIONS,
                            ).mapNotNull { name ->
                                currentEntry.arguments
                                    ?.getString(name)
                                    ?.let { value -> name to value }
                            }.toMap()
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = "home",
                ) {
                    composable("home") {
                        Button(
                            onClick = {
                                navController.navigate(route.value)
                            },
                        ) {
                            Text("Open sheet")
                        }
                    }
                    routedOverlayDestination(
                        navController = navController,
                        presentation = NavPresentation.SHEET,
                        onGeneratedAction = { _, action ->
                            selectedActionId = action.id
                        },
                    ) { _, _ -> }
                }
            }
        }

        composeRule.onNodeWithText("Open sheet").performClick()
        composeRule.runOnIdle {
            assertEquals(route.parse().queryParameters, capturedArguments)
        }
        composeRule.onNodeWithText("Generated sheet").assertExists()
        composeRule.onNodeWithText("Generated message").assertExists()
        composeRule.onNodeWithText("Confirm").performClick()

        composeRule.runOnIdle {
            assertEquals(NavOverlayActionId("confirm"), selectedActionId)
        }
        composeRule.onNodeWithText("Generated sheet").assertDoesNotExist()
        composeRule.onNodeWithText("Open sheet").assertIsDisplayed()
    }
}
