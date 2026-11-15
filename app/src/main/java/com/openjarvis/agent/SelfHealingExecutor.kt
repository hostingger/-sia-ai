package com.openjarvis.agent

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.delay
import org.json.JSONObject

class SelfHealingExecutor(private val context: Context) {

    private val screenReader = ScreenReader(JarvisAccessibilityService.instance)
    private val graphifyRepo = GraphifyRepository(context)
    private val llm = UniversalAdapter(context)

    private val maxAttempts = 3
    private val baseDelayMs = 1000L

    suspend fun executeWithHealing(
        action: Action,
        context: ExecutionContext,
        attempt: Int = 1
    ): ActionResult {
        when (val result = tryExecuteAction(action, context)) {
            is ActionResult.Success -> return result
            is ActionResult.Failed -> if (attempt >= maxAttempts) return result
        }

        if (attempt >= maxAttempts) {
            return ActionResult.Failed("Could not complete after $maxAttempts attempts")
        }

        val currentScreen = screenReader.extractAllText()
        val healingPrompt = buildHealingPrompt(action, context, currentScreen, attempt)
        val alternativeResponse = try {
            llm.complete(healingPrompt.first, healingPrompt.second).getOrNull()
        } catch (_: Exception) {
            null
        }

        delay(baseDelayMs * attempt)

        val alternativeAction = alternativeResponse?.let(::parseActionFromLLM)
        return executeWithHealing(alternativeAction ?: action, context, attempt + 1)
    }

    private fun tryExecuteAction(action: Action, context: ExecutionContext): ActionResult {
        return try {
            val service = JarvisAccessibilityService.instance
                ?: return ActionResult.Failed("Accessibility service is not enabled")

            when (action.action) {
                Action.TAP -> {
                    val tapped = service.tapByText(action.text.orEmpty()) == true
                    if (tapped) ActionResult.Success("Tapped ${action.text}")
                    else ActionResult.Failed("Tap failed: ${action.text}")
                }
                Action.TYPE -> {
                    val typed = service.typeText(action.value.orEmpty()) == true
                    if (typed) ActionResult.Success("Typed text")
                    else ActionResult.Failed("Text entry failed")
                }
                Action.OPEN_APP -> {
                    val packageName = action.packageName
                        ?: return ActionResult.Failed("Missing app package name")
                    service.openAppByPackage(packageName)
                    ActionResult.Success("Opened $packageName")
                }
                Action.PRESS_BACK -> {
                    service.pressBack()
                    ActionResult.Success("Pressed back")
                }
                else -> ActionResult.Failed("Unsupported healing action: ${action.action}")
            }
        } catch (e: Exception) {
            ActionResult.Failed(e.message ?: "Unknown error")
        }
    }

    private fun buildHealingPrompt(
        action: Action,
        context: ExecutionContext,
        currentScreen: String,
        attempt: Int
    ): Pair<String, String> {
        val system = """
Action failed: ${action.label ?: action.action}
Expected to see: ${context.expectedState ?: "task completion"}
Current screen shows: ${currentScreen.take(500)}
Attempt: $attempt/$maxAttempts

Suggest one alternative Action JSON with an action and optional text, value, or package fields.
        """.trimIndent()

        return system to "The action failed. Current screen: \"${currentScreen.take(500)}\". Suggest one alternative action."
    }

    private fun parseActionFromLLM(llmResponse: String): Action? {
        return try {
            val clean = llmResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(clean)
            Action(
                action = json.getString("action"),
                text = json.optString("text").takeIf { it.isNotBlank() },
                value = json.optString("value").takeIf { it.isNotBlank() },
                packageName = json.optString("package").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }
    }

    data class ExecutionContext(
        val expectedState: String?,
        val phaseId: String,
        val previousPhaseOutcome: String?,
        val screenBefore: String
    )

    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Failed(val reason: String) : ActionResult()
    }
}
