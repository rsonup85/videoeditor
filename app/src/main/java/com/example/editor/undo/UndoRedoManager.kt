package com.example.editor.undo

import com.example.domain.model.Project
import java.util.ArrayDeque

class UndoRedoManager(private val maxHistorySize: Int = 30) {

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun pushState(state: Project) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeLast()
        }
        undoStack.push(state)
        redoStack.clear()
    }

    fun undo(currentState: Project): Project? {
        if (undoStack.isEmpty()) return null
        val previousState = undoStack.pop()
        redoStack.push(currentState)
        return previousState
    }

    fun redo(currentState: Project): Project? {
        if (redoStack.isEmpty()) return null
        val nextState = redoStack.pop()
        undoStack.push(currentState)
        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
