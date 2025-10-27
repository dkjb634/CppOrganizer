package com.dkjb634.CppOrganizer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import com.intellij.openapi.application.ModalityState

class FileOpenedListener : FileEditorManagerListener {

    companion object {
        private val LOG = Logger.getInstance(FileOpenedListener::class.java)
        private const val CPP_EXTENSION = "cpp"
        private const val H_EXTENSION = "h"
        private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, ApplicationManager.getApplication())
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val extension = file.extension?.lowercase()

        // Only handle .cpp and .h files, ignore everything else
        if (extension != CPP_EXTENSION && extension != H_EXTENSION) {
            return
        }

        LOG.info("FileOpenedListener: File opened: ${file.name}")

        val project = source.project

        // Skip files that are being templated or are empty (new file creation)
        if (isFileBeingTemplated(project, file)) {
            LOG.info("FileOpenedListener: Skipping ${file.name} - appears to be a new file from template. Will organize in 2 second.")

            // Schedule organization in 1 second
            alarm.addRequest({
                LOG.info("FileOpenedListener: Scheduled organization for ${file.name}")
                ApplicationManager.getApplication().invokeLater {
                    val fileEditorManager = FileEditorManager.getInstance(project)
                    processFileOpening(fileEditorManager, file, extension)
                }
            }, 2000, ModalityState.nonModal())

            return
        }

        // This is an existing file - organize it immediately
        LOG.info("FileOpenedListener: Organizing existing file: ${file.name}")
        ApplicationManager.getApplication().invokeLater {
            processFileOpening(source, file, extension)
        }
    }

    /**
     * Checks if the file is being created from a template or is empty.
     */
    private fun isFileBeingTemplated(project: Project, file: VirtualFile): Boolean {
        try {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile != null) {
                val text = psiFile.text

                // Contains template variables
                if (text.contains("\$NAME\$") || text.contains("\$PCH\$")) {
                    return true
                }

                if (text.contains("HEADER")) {
                    return true
                }

                if (text.contains("\$COPYRIGHT_LINE\$") || text.contains("\$BASE_CLASS_INCLUDE_DIRECTIVE\$") || text.contains("\$PUBLIC_HEADER_INCLUDES\$")) {
                    return true
                }

                if (text.isEmpty()) {
                    return true
                }
            }
        } catch (e: Exception) {
            LOG.warn("FileOpenedListener: Error checking file status for ${file.name}", e)
        }
        return false
    }

    private fun processFileOpening(source: FileEditorManager, file: VirtualFile, extension: String) {
        val project = source.project
        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val currentWindow = fileEditorManager.currentWindow
        val allWindows = fileEditorManager.windows

        val caretOffset = captureCaretOffset(source, file)
        val targetWindow = findTargetWindow(allWindows, currentWindow, extension, file)

        if (targetWindow != null && targetWindow != currentWindow) {
            LOG.info("FileOpenedListener: Moving ${file.name} to existing .$extension window")
            currentWindow?.closeFile(file)
            ApplicationManager.getApplication().invokeLater {
                fileEditorManager.openFile(file, targetWindow)
                restoreCaretOffset(source, file, caretOffset)
            }
        } else if (targetWindow == null) {
            LOG.info("FileOpenedListener: Creating new split for .$extension files")
            currentWindow?.closeFile(file)
            createNewSplitAndOpen(project, file, caretOffset, source)
        } else {
            LOG.info("FileOpenedListener: ${file.name} is already in correct window")
        }
    }

    private fun findTargetWindow(
        allWindows: Array<EditorWindow>,
        currentWindow: EditorWindow?,
        extension: String,
        currentFile: VirtualFile
    ): EditorWindow? {
        var bestWindow: EditorWindow? = null
        var maxCount = 0

        // Find window with most files of the same extension
        for (window in allWindows) {
            val count = window.fileList.count {
                it.extension?.lowercase() == extension && it != currentFile
            }
            if (count > maxCount) {
                maxCount = count
                bestWindow = window
            }
        }

        // Found a window with matching files
        if (maxCount > 0) {
            return bestWindow
        }

        // Check if current window is suitable (empty or only has same extension)
        if (currentWindow != null) {
            val otherExtensions = currentWindow.fileList
                .filter { it != currentFile }
                .mapNotNull { it.extension?.lowercase() }
                .distinct()

            if (otherExtensions.isEmpty() || (otherExtensions.size == 1 && otherExtensions[0] == extension)) {
                return currentWindow
            }
        }

        return null
    }

    private fun createNewSplitAndOpen(
        project: Project,
        file: VirtualFile,
        caretOffset: Int?,
        source: FileEditorManager
    ) {
        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val currentWindow = fileEditorManager.currentWindow

        if (currentWindow != null) {
            val newWindow = currentWindow.split(
                orientation = 1,
                forceSplit = true,
                virtualFile = file,
                focusNew = true
            )

            if (newWindow != null && caretOffset != null) {
                ApplicationManager.getApplication().invokeLater {
                    restoreCaretOffset(source, file, caretOffset)
                }
            }
        } else {
            fileEditorManager.openFile(file, true)
        }
    }

    private fun captureCaretOffset(source: FileEditorManager, file: VirtualFile): Int? {
        val selectedEditor = source.selectedEditor
        if (selectedEditor is TextEditor && selectedEditor.file == file) {
            return selectedEditor.editor.caretModel.offset
        }
        return null
    }

    private fun restoreCaretOffset(source: FileEditorManager, file: VirtualFile, caretOffset: Int?) {
        if (caretOffset != null) {
            val editor = source.getSelectedEditor(file)
            if (editor is TextEditor) {
                editor.editor.caretModel.moveToOffset(caretOffset)
            }
        }
    }
}