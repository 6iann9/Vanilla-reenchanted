package net.iann.vanillareenchanted.screen;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/research_table.png"
            );

    private static final int ENCHANTMENTS_PER_PAGE = 9;

    private static final int ENCHANT_LIST_X = 34;
    private static final int ENCHANT_LIST_Y = 63;
    private static final int ENCHANT_LINE_HEIGHT = 10;

    private static final int PREV_PAGE_X = 55;
    private static final int NEXT_PAGE_X = PREV_PAGE_X + 35;
    private static final int PAGE_BUTTON_Y = 156;

    private static final int PAGE_TEXT_COLOR = 0xFFAD7757;

    private static final int ARROW_SIZE = 11;

    private static final ResourceLocation ACTIVE_LEFT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/active_left_arrow.png"
            );

    private static final ResourceLocation ACTIVE_RIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/active_right_arrow.png"
            );

    private static final ResourceLocation INACTIVE_LEFT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/inactive_left_arrow.png"
            );

    private static final ResourceLocation INACTIVE_RIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/inactive_right_arrow.png"
            );

    private static final ResourceLocation ARROW_OUTLINE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/arrow_outline.png"
            );

    private int enchantmentPage = 0;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 278;
        this.imageHeight = 278;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderEnchantmentList(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Hide default title and inventory labels.
    }

    private void renderEnchantmentList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int totalPages = getTotalPages(enchantments.size());

        if (this.enchantmentPage >= totalPages) {
            this.enchantmentPage = Math.max(0, totalPages - 1);
        }

        int startIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int startX = this.leftPos + ENCHANT_LIST_X;
        int startY = this.topPos + ENCHANT_LIST_Y;

        for (int i = startIndex; i < endIndex; i++) {
            Holder<Enchantment> enchantmentHolder = enchantments.get(i);

            String name = enchantmentHolder.unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("unknown");

            Component text = Component.literal(formatEnchantName(name))
                    .withStyle(ChatFormatting.DARK_GRAY);

            int lineIndex = i - startIndex;

            guiGraphics.drawString(
                    this.font,
                    text,
                    startX,
                    startY + lineIndex * ENCHANT_LINE_HEIGHT,
                    0x3F2A14,
                    false
            );
        }

        renderPageControls(guiGraphics, totalPages, mouseX, mouseY);
    }

    private String formatEnchantName(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private void renderPageControls(
            GuiGraphics guiGraphics,
            int totalPages,
            int mouseX,
            int mouseY
    ) {
        if (totalPages <= 1) {
            return;
        }

        int leftArrowX = this.leftPos + PREV_PAGE_X;
        int rightArrowX = this.leftPos + NEXT_PAGE_X;
        int arrowY = this.topPos + PAGE_BUTTON_Y;

        boolean canGoPrevious = this.enchantmentPage > 0;
        boolean canGoNext = this.enchantmentPage < totalPages - 1;

        boolean hoveringPrevious = isMouseOver(
                mouseX,
                mouseY,
                leftArrowX,
                arrowY,
                ARROW_SIZE,
                ARROW_SIZE
        );

        boolean hoveringNext = isMouseOver(
                mouseX,
                mouseY,
                rightArrowX,
                arrowY,
                ARROW_SIZE,
                ARROW_SIZE
        );

        ResourceLocation leftTexture = canGoPrevious
                ? ACTIVE_LEFT_ARROW
                : INACTIVE_LEFT_ARROW;

        ResourceLocation rightTexture = canGoNext
                ? ACTIVE_RIGHT_ARROW
                : INACTIVE_RIGHT_ARROW;

        guiGraphics.blit(
                leftTexture,
                leftArrowX,
                arrowY,
                0,
                0,
                ARROW_SIZE,
                ARROW_SIZE,
                ARROW_SIZE,
                ARROW_SIZE
        );

        guiGraphics.blit(
                rightTexture,
                rightArrowX,
                arrowY,
                0,
                0,
                ARROW_SIZE,
                ARROW_SIZE,
                ARROW_SIZE,
                ARROW_SIZE
        );

        if (canGoPrevious && hoveringPrevious) {
            guiGraphics.blit(
                    ARROW_OUTLINE,
                    leftArrowX,
                    arrowY,
                    0,
                    0,
                    ARROW_SIZE,
                    ARROW_SIZE,
                    ARROW_SIZE,
                    ARROW_SIZE
            );
        }

        if (canGoNext && hoveringNext) {
            guiGraphics.blit(
                    ARROW_OUTLINE,
                    rightArrowX,
                    arrowY,
                    0,
                    0,
                    ARROW_SIZE,
                    ARROW_SIZE,
                    ARROW_SIZE,
                    ARROW_SIZE
            );
        }

        Component pageText = Component.literal(
                (this.enchantmentPage + 1) + "/" + totalPages
        );

        int pageTextX = this.leftPos + PREV_PAGE_X + 14;
        int pageTextY = arrowY + 2;

        guiGraphics.drawString(
                this.font,
                pageText,
                pageTextX,
                pageTextY,
                PAGE_TEXT_COLOR,
                false
        );
    }

    private int getTotalPages(int enchantmentCount) {
        if (enchantmentCount == 0) {
            return 1;
        }

        return (int) Math.ceil(enchantmentCount / (double) ENCHANTMENTS_PER_PAGE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePageButtonClick(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePageButtonClick(double mouseX, double mouseY) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();
        int totalPages = getTotalPages(enchantments.size());

        if (totalPages <= 1) {
            return false;
        }

        int prevX = this.leftPos + PREV_PAGE_X;
        int nextX = this.leftPos + NEXT_PAGE_X;
        int y = this.topPos + PAGE_BUTTON_Y;

        if (isMouseOver(mouseX, mouseY, prevX, y, ARROW_SIZE, ARROW_SIZE)) {
            if (this.enchantmentPage > 0) {
                this.enchantmentPage--;
                playButtonClickSound();
            }

            return true;
        }

        if (isMouseOver(mouseX, mouseY, nextX, y, ARROW_SIZE, ARROW_SIZE)) {
            if (this.enchantmentPage < totalPages - 1) {
                this.enchantmentPage++;
                playButtonClickSound();
            }

            return true;
        }

        return false;
    }

    private void playButtonClickSound() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.playSound(
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    1.0F,
                    1.0F
            );
        }
    }

    private boolean isMouseOver(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}