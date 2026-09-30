package io.thernal.mediakit.media.impl.presentation

import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Presents a picker from the top-most view controller, one at a time: UIKit refuses to present from a
 * controller that is already presenting, so a second request waits for the first to be on screen.
 */
internal object PickerPresenter {
    private class Request(
        val controller: UIViewController,
        val onUnavailable: () -> Unit,
    )

    private val queue = ArrayDeque<Request>()
    private var presenting = false

    fun present(
        controller: UIViewController,
        onUnavailable: () -> Unit,
    ) {
        dispatch_async(dispatch_get_main_queue()) {
            queue.addLast(Request(controller = controller, onUnavailable = onUnavailable))
            presentNext()
        }
    }

    private fun presentNext() {
        if (presenting) {
            return
        }
        val request = queue.removeFirstOrNull() ?: return
        val presenter = topMostViewController()
        if (presenter == null) {
            request.onUnavailable()
            presentNext()
            return
        }
        presenting = true
        presenter.presentViewController(request.controller, animated = true) {
            presenting = false
            presentNext()
        }
    }

    private fun topMostViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .let { windows -> windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull() }
        return window?.rootViewController?.topMost()
    }

    private fun UIViewController.topMost(): UIViewController {
        presentedViewController?.let { return it.topMost() }
        return when (this) {
            is UINavigationController -> visibleViewController?.topMost() ?: this
            is UITabBarController -> selectedViewController?.topMost() ?: this
            else -> this
        }
    }
}
