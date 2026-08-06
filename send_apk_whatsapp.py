from __future__ import annotations

import importlib.machinery
import importlib.util
import subprocess
from pathlib import Path
import sys
import time

from pywinauto import Desktop, mouse
import win32clipboard
import win32con
import ctypes
from ctypes import wintypes


PROJECT_ROOT = Path(__file__).resolve().parent
HELPER_PYC = PROJECT_ROOT.parent / "__pycache__" / "send_apk_whatsapp.cpython-312.pyc"


def load_helper():
    if not HELPER_PYC.is_file():
        raise SystemExit(f"Helper bytecode not found: {HELPER_PYC}")

    loader = importlib.machinery.SourcelessFileLoader("send_apk_whatsapp_cached", str(HELPER_PYC))
    spec = importlib.util.spec_from_loader(loader.name, loader)
    if spec is None:
        raise SystemExit("Failed to load helper module")

    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)

    original_find_controls = module.find_controls

    def find_controls(window, control_type=None, exact_text=None, contains_text=None, rect_filter=None, prefer=None):
        return original_find_controls(
            window,
            control_type=control_type,
            exact_text=exact_text,
            contains_text=contains_text,
            rect_filter=rect_filter,
        )

    def find_whatsapp_window():
        wins = []
        for win in Desktop(backend="win32").windows():
            title = (win.window_text() or "").strip()
            if "WhatsApp" not in title:
                continue

            try:
                rect = win.rectangle()
            except Exception:
                continue

            if rect.width() < 200 or rect.height() < 200:
                continue

            wins.append(win)

        if not wins:
            raise RuntimeError("WhatsApp window not found")

        best = max(wins, key=lambda win: win.rectangle().width() * win.rectangle().height())
        return Desktop(backend="uia").window(handle=best.handle)

    def copy_file_to_clipboard(apk_path):
        class DROPFILES(ctypes.Structure):
            _fields_ = [
                ("pFiles", wintypes.DWORD),
                ("pt", wintypes.POINT),
                ("fNC", wintypes.BOOL),
                ("fWide", wintypes.BOOL),
            ]

        dropfiles = DROPFILES()
        dropfiles.pFiles = ctypes.sizeof(DROPFILES)
        dropfiles.pt = wintypes.POINT(0, 0)
        dropfiles.fNC = 0
        dropfiles.fWide = 1

        payload = bytes(dropfiles) + (str(apk_path) + "\0\0").encode("utf-16le")

        win32clipboard.OpenClipboard()
        try:
            win32clipboard.EmptyClipboard()
            win32clipboard.SetClipboardData(win32con.CF_HDROP, payload)
        finally:
            win32clipboard.CloseClipboard()

    def drag_file_to_window(apk_path, window):
        subprocess.Popen(["explorer.exe", str(apk_path.parent)])

        explorer = module.wait_until(
            lambda: next(
                (
                    win
                    for win in Desktop(backend="uia").windows(class_name="CabinetWClass")
                    if win.is_visible()
                    and any((c.window_text() or "").strip() == apk_path.name for c in win.descendants())
                ),
                None,
            ),
            timeout=15,
            label="Explorer window",
        )

        file_item = module.wait_until(
            lambda: next(
                (
                    control
                    for control in explorer.descendants()
                    if (control.window_text() or "").strip() == apk_path.name
                    and getattr(control.element_info, "control_type", None) in {"ListItem", "DataItem", "Text"}
                ),
                None,
            ),
            timeout=10,
            label="APK file item",
        )

        target = module.wait_until(
            lambda: module.find_message_edit(window),
            timeout=10,
            label="WhatsApp message box",
        )

        source_rect = file_item.rectangle()
        target_rect = target.rectangle()
        source = (source_rect.left + source_rect.width() // 2, source_rect.top + source_rect.height() // 2)
        dest = (target_rect.left + target_rect.width() // 2, target_rect.top + target_rect.height() // 2)

        mouse.press(coords=source)
        time.sleep(0.2)
        mouse.move(coords=dest)
        time.sleep(0.2)
        mouse.release(coords=dest)

    def find_send_selected_button(window):
        rect = window.rectangle()
        preferred_filter = lambda r: r.left > rect.left + 250 and r.top > rect.bottom - 500

        for label in (
            "enviar 1 item selecionado",
            "enviar 1 arquivo selecionado",
            "enviar",
            "send",
        ):
            controls = original_find_controls(
                window,
                control_type="Button",
                exact_text=label,
                rect_filter=preferred_filter,
            )
            if controls:
                return module.choose_control(controls, prefer="bottom")

        for label in ("enviar", "send"):
            controls = original_find_controls(
                window,
                control_type="Button",
                contains_text=label,
                rect_filter=preferred_filter,
            )
            if controls:
                return module.choose_control(controls, prefer="bottom")

        raise RuntimeError("Send button not found")

    def attach_and_send_apk(window, apk_path):
        last_error = None

        for strategy in (
            drag_file_to_window,
            copy_file_to_clipboard,
        ):
            try:
                strategy(apk_path, window) if strategy is drag_file_to_window else strategy(apk_path)
                time.sleep(1.0)

                send_button = module.wait_until(
                    lambda: find_send_selected_button(window),
                    timeout=10,
                    label="send selected button",
                )
                module.click_control(send_button)
                return
            except Exception as exc:
                last_error = exc

        raise last_error

    module.find_controls = find_controls
    module.find_whatsapp_window = find_whatsapp_window
    module.find_send_selected_button = find_send_selected_button
    module.attach_and_send_apk = attach_and_send_apk
    module.ROOT = PROJECT_ROOT
    return module


def main() -> int:
    helper = load_helper()
    return helper.main()


if __name__ == "__main__":
    raise SystemExit(main())
