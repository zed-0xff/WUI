package me.zed_0xff.XUI;

import java.util.ArrayList;
import java.util.List;

public final class RadioGroup {
    private final Window window;
    final List<RadioButton> buttons = new ArrayList<>();

    RadioGroup(Window window) { this.window = window; }

    Window getWindow() { return window; }

    void add(RadioButton rb) { buttons.add(rb); }

    /** Create a {@link RadioButton} in this group (auto-registered). */
    RadioButton button(int x, int y, int w, int h, String text) {
        return new RadioButton(this, x, y, w, h, text);
    }

    void select(RadioButton chosen) {
        buttons.forEach(r -> r.checked = (r == chosen));
    }

    RadioButton getChecked() {
        return buttons.stream().filter(r -> r.checked).findFirst().orElse(null);
    }
}
