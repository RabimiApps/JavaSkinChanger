import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawableHelper;

public class SkinChangeScreen extends Screen {

    private int leftX = 10;
    private int leftY = 40;
    private int buttonWidth = 150;
    private int buttonHeight = 20;
    private int spacing = 5;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
    }

    @Override
    protected void init() {
        addDrawableChild(new SimpleButton(leftX, leftY, buttonWidth, buttonHeight, Text.of("モデル変更"), button -> {
            // モデル変更処理
        }));

        addDrawableChild(new SimpleButton(leftX, leftY + buttonHeight + spacing, buttonWidth, buttonHeight, Text.of("スキン変更"), button -> {
            // スキン変更処理
        }));

        addDrawableChild(new SimpleButton(leftX, leftY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, Text.of("リロード"), button -> {
            // リロード処理
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景透明75%
        DrawableHelper.fill(context.getMatrices(), 0, 0, width, height, 0xBF000000);

        // 左上タイトル
        context.drawTextWithShadow(textRenderer, "JavaSkinChanger", 5, 5, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    /** サブクラス化でButtonWidget protectedコンストラクタ回避 */
    private static class SimpleButton extends ButtonWidget {
        public SimpleButton(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        }
    }
}