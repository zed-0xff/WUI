package me.zed_0xff.WUI;

public class RadioButton extends ToggleBase {
    private final RadioGroup group;

    public RadioButton(RadioGroup group, int x, int y, int w, int h, String text) {
        super(group.getWindow(), x, y, w, h, text);
        this.group = group;
        group.add(this);
    }

    @Override protected String styleName() { return "radiobutton"; }

    @Override
    protected void onClick() {
        if (!checked) group.select(this);
    }
}
