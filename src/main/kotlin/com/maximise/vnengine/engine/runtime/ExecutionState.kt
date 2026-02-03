package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.VnNode

sealed class ExecutionState {
    data class ShowDialogue(val dialogue: VnNode.Dialogue) : ExecutionState()
    data class ShowChoice(val choiceStatement: VnNode.ChoiceStatement) : ExecutionState()
    data class ShowBackground(val backgroundNode: VnNode.BackgroundStatement) : ExecutionState()
    object Finished : ExecutionState()
}