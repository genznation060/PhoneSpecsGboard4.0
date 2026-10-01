package com.example.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.SpecBoardApplication
import com.example.ui.theme.SpecBoardTheme

class SpecBoardImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@SpecBoardImeService))

            val decorView = this@SpecBoardImeService.window?.window?.decorView
            if (decorView != null) {
                decorView.setViewTreeLifecycleOwner(this@SpecBoardImeService)
                decorView.setViewTreeViewModelStoreOwner(this@SpecBoardImeService)
                decorView.setViewTreeSavedStateRegistryOwner(this@SpecBoardImeService)
            }

            setViewTreeLifecycleOwner(this@SpecBoardImeService)
            setViewTreeViewModelStoreOwner(this@SpecBoardImeService)
            setViewTreeSavedStateRegistryOwner(this@SpecBoardImeService)

            setContent {
                SpecBoardTheme(darkTheme = true) {
                    ImeKeyboardScreen(
                        repository = (application as SpecBoardApplication).repository,
                        onInsertText = { text -> insertText(text) },
                        onBackspace = { handleBackspace() },
                        onEnter = { handleEnter() },
                        onSpace = { insertText(" ") },
                        onSwitchIme = { switchIme() },
                        onHideKeyboard = { requestHideSelf(0) },
                        modifier = Modifier
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    private fun insertText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun switchIme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val switched = switchToPreviousInputMethod()
            if (!switched) {
                showPicker()
            }
        } else {
            showPicker()
        }
    }

    private fun showPicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }
}
