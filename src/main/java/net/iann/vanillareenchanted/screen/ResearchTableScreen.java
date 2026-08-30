package net.iann.vanillareenchanted.screen;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.cost.EnchantmentCostCalculator;
import net.iann.vanillareenchanted.cost.EnvironmentCostModifier;
import net.iann.vanillareenchanted.enchantment.DuplicateBookDiscountHelper;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.iann.vanillareenchanted.network.EnchantItemPayload;
import net.iann.vanillareenchanted.network.ResearchEnchantmentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final int GUI_WIDTH = 278;
    private static final int GUI_HEIGHT = 278;

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/research_table.png"
            );

    private static final int ENCHANTMENTS_PER_PAGE = 6;

    private static final int ENCHANT_ROW_X = 33;
    private static final int ENCHANT_ROW_Y = 60;

    private static final int ENCHANT_ROW_WIDTH = 97;
    private static final int ENCHANT_ROW_HEIGHT = 13;
    private static final int ENCHANT_ROW_SPACING = 15;

    private static final int ENCHANT_LEVEL_TEXT_X = 4;
    private static final int ENCHANT_NAME_TEXT_X = 15;
    private static final int ENCHANT_TEXT_Y = 3;
    private static final int ENCHANT_NAME_RIGHT_PADDING = 2;

    private static final int ENCHANT_NAME_MAX_WIDTH =
            ENCHANT_ROW_WIDTH
                    - ENCHANT_NAME_TEXT_X
                    - ENCHANT_NAME_RIGHT_PADDING;

    private static final int TEXT_COLOR = 0xFFCCA886;

    private static final ResourceLocation ENCHANT_ROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/enchantment_list_text_box.png"
            );

    private static final ResourceLocation ENCHANT_ROW_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/enchantment_list_text_box_outline.png"
            );

    private static final int SELECT_ARROW_WIDTH = 4;
    private static final int SELECT_ARROW_HEIGHT = 8;

    private static final int SELECT_ARROW_X_OFFSET = -5;
    private static final int SELECT_ARROW_Y_OFFSET = 3;

    private static final ResourceLocation SELECT_ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/select_arrow.png"
            );

    private static final int PAGE_PREVIOUS_X = 58;
    private static final int PAGE_NEXT_X = PAGE_PREVIOUS_X + 33;
    private static final int PAGE_Y = 156;

    private static final int PAGE_ARROW_SIZE = 11;

    private static final int PAGE_TEXT_X_OFFSET = 13;
    private static final int PAGE_TEXT_Y_OFFSET = 2;

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

    private static final ResourceLocation PAGE_ARROW_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/arrow_outline.png"
            );

    private static final int DETAILS_X = 146;
    private static final int DETAILS_Y = 40;

    private static final int DETAILS_LINE_HEIGHT = 12;

    private static final int KNOWLEDGE_SECTION_Y = DETAILS_Y + 18;
    private static final int ITEM_SECTION_Y = DETAILS_Y + 82;

    private static final int TEXT_PROGRESS_BAR_WIDTH = 10;

    private static final int RESEARCH_BUTTON_X = DETAILS_X;
    private static final int RESEARCH_BUTTON_Y =
            KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT * 3;

    private static final int RESEARCH_BUTTON_WIDTH = 70;
    private static final int RESEARCH_BUTTON_HEIGHT = 10;

    private static final int ENCHANT_BUTTON_X = DETAILS_X;
    private static final int ENCHANT_BUTTON_Y =
            ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 3;

    private static final int ENCHANT_BUTTON_WIDTH = 70;
    private static final int ENCHANT_BUTTON_HEIGHT = 10;

    private static final int LAPIS_SLOT_X = 170;
    private static final int LAPIS_SLOT_Y = 19;

    private static final int DUPLICATE_BOOK_SLOT_X = 210;
    private static final int DUPLICATE_BOOK_SLOT_Y = 19;

    private static final int LEFT_ARMOR_X = 37;
    private static final int RIGHT_ARMOR_X = 225;
    private static final int ARMOR_TOP_Y = 196;
    private static final int ARMOR_BOTTOM_Y = 232;

    private static final ResourceLocation LAPIS_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/lapis_hint.png"
            );

    private static final ResourceLocation BOOK_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/book_hint.png"
            );

    private static final ResourceLocation HELMET_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/helmet_hint.png"
            );

    private static final ResourceLocation CHESTPLATE_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/chestplate_hint.png"
            );

    private static final ResourceLocation LEGGINGS_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/leggings_hint.png"
            );

    private static final ResourceLocation BOOTS_HINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/icons/boots_hint.png"
            );

    private int enchantmentPage = 0;

    private ResourceLocation selectedEnchantmentId = null;

    public ResearchTableScreen(
            ResearchTableMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.blit(
                BACKGROUND_TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                GUI_WIDTH,
                GUI_HEIGHT,
                GUI_WIDTH,
                GUI_HEIGHT
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderSlotHintTextures(guiGraphics);

        renderEnchantmentList(guiGraphics, mouseX, mouseY);
        renderSelectedEnchantmentDetails(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
    }

    private void renderSlotHintTextures(GuiGraphics guiGraphics) {
        renderSlotHintTexture(
                guiGraphics,
                this.menu.getLapisItem(),
                LAPIS_HINT_TEXTURE,
                LAPIS_SLOT_X,
                LAPIS_SLOT_Y
        );

        renderSlotHintTexture(
                guiGraphics,
                this.menu.getDuplicateBookItem(),
                BOOK_HINT_TEXTURE,
                DUPLICATE_BOOK_SLOT_X,
                DUPLICATE_BOOK_SLOT_Y
        );

        renderSlotHintTexture(
                guiGraphics,
                this.menu.getHelmetItem(),
                HELMET_HINT_TEXTURE,
                LEFT_ARMOR_X,
                ARMOR_TOP_Y
        );

        renderSlotHintTexture(
                guiGraphics,
                this.menu.getChestplateItem(),
                CHESTPLATE_HINT_TEXTURE,
                LEFT_ARMOR_X,
                ARMOR_BOTTOM_Y
        );

        renderSlotHintTexture(
                guiGraphics,
                this.menu.getLeggingsItem(),
                LEGGINGS_HINT_TEXTURE,
                RIGHT_ARMOR_X,
                ARMOR_TOP_Y
        );

        renderSlotHintTexture(
                guiGraphics,
                this.menu.getBootsItem(),
                BOOTS_HINT_TEXTURE,
                RIGHT_ARMOR_X,
                ARMOR_BOTTOM_Y
        );
    }

    private void renderSlotHintTexture(
            GuiGraphics guiGraphics,
            ItemStack slotStack,
            ResourceLocation hintTexture,
            int x,
            int y
    ) {
        if (!slotStack.isEmpty()) {
            return;
        }

        guiGraphics.blit(
                hintTexture,
                this.leftPos + x,
                this.topPos + y,
                0,
                0,
                16,
                16,
                16,
                16
        );
    }

    private void renderEnchantmentList(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int totalPages = getTotalPages(enchantments.size());
        clampCurrentPage(totalPages);

        int firstIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int lastIndex = Math.min(firstIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int rowX = this.leftPos + ENCHANT_ROW_X;

        for (int enchantmentIndex = firstIndex; enchantmentIndex < lastIndex; enchantmentIndex++) {
            Holder<Enchantment> enchantmentHolder = enchantments.get(enchantmentIndex);

            ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

            if (enchantmentId == null) {
                continue;
            }

            int rowIndexOnPage = enchantmentIndex - firstIndex;
            int rowY = this.topPos
                    + ENCHANT_ROW_Y
                    + rowIndexOnPage * ENCHANT_ROW_SPACING;

            boolean isHovered = isMouseOver(
                    mouseX,
                    mouseY,
                    rowX,
                    rowY,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            );

            boolean isSelected = enchantmentId.equals(this.selectedEnchantmentId);

            renderEnchantmentRowBackground(
                    guiGraphics,
                    rowX,
                    rowY,
                    isHovered,
                    isSelected
            );

            int displayedLevel = getDisplayedEnchantmentLevel(enchantmentHolder);

            renderEnchantmentRowText(
                    guiGraphics,
                    rowX,
                    rowY,
                    enchantmentHolder,
                    displayedLevel
            );
        }

        renderPageControls(guiGraphics, totalPages, mouseX, mouseY);
    }

    private void renderEnchantmentRowBackground(
            GuiGraphics guiGraphics,
            int rowX,
            int rowY,
            boolean isHovered,
            boolean isSelected
    ) {
        guiGraphics.blit(
                ENCHANT_ROW_TEXTURE,
                rowX,
                rowY,
                0,
                0,
                ENCHANT_ROW_WIDTH,
                ENCHANT_ROW_HEIGHT,
                ENCHANT_ROW_WIDTH,
                ENCHANT_ROW_HEIGHT
        );

        if (isHovered) {
            guiGraphics.blit(
                    ENCHANT_ROW_HOVER_TEXTURE,
                    rowX,
                    rowY,
                    0,
                    0,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            );
        }

        if (isSelected) {
            guiGraphics.blit(
                    SELECT_ARROW_TEXTURE,
                    rowX + SELECT_ARROW_X_OFFSET,
                    rowY + SELECT_ARROW_Y_OFFSET,
                    0,
                    0,
                    SELECT_ARROW_WIDTH,
                    SELECT_ARROW_HEIGHT,
                    SELECT_ARROW_WIDTH,
                    SELECT_ARROW_HEIGHT
            );
        }
    }

    private void renderEnchantmentRowText(
            GuiGraphics guiGraphics,
            int rowX,
            int rowY,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        Component levelText = Component.literal(String.valueOf(level));

        Component fullEnchantmentName = Component.literal(
                enchantmentHolder.value().description().getString()
        );

        Component enchantmentName = shortenTextToFit(
                fullEnchantmentName,
                ENCHANT_NAME_MAX_WIDTH
        );

        guiGraphics.drawString(
                this.font,
                levelText,
                rowX + ENCHANT_LEVEL_TEXT_X,
                rowY + ENCHANT_TEXT_Y,
                TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                this.font,
                enchantmentName,
                rowX + ENCHANT_NAME_TEXT_X,
                rowY + ENCHANT_TEXT_Y,
                TEXT_COLOR,
                false
        );
    }

    private void renderSelectedEnchantmentDetails(GuiGraphics guiGraphics) {
        Holder<Enchantment> selectedEnchantment = getSelectedEnchantment();

        int x = this.leftPos + DETAILS_X;
        int y = this.topPos + DETAILS_Y;

        if (selectedEnchantment == null) {
            drawRightPageText(
                    guiGraphics,
                    Component.literal("Select enchantment"),
                    x,
                    y
            );

            return;
        }

        String enchantmentName = selectedEnchantment.value()
                .description()
                .getString();

        int maxLevel = Math.max(1, selectedEnchantment.value().getMaxLevel());
        int knowledgeLevel = getLibraryKnowledgeLevel(selectedEnchantment);
        int itemLevel = getItemEnchantmentLevel(selectedEnchantment);

        int nextKnowledgeLevel = Math.min(knowledgeLevel + 1, maxLevel);
        int nextItemLevel = Math.min(itemLevel + 1, maxLevel);

        int baseResearchXpCost = EnchantmentCostCalculator.getResearchXpCost(
                selectedEnchantment,
                nextKnowledgeLevel
        );

        int researchCostAfterDuplicateBook = DuplicateBookDiscountHelper.getDiscountedResearchXpLevelCost(
                baseResearchXpCost,
                nextKnowledgeLevel,
                this.menu.getDuplicateBookItem(),
                selectedEnchantment
        );

        int researchXpCost = EnvironmentCostModifier.applyCandleResearchDiscount(
                this.minecraft.level,
                this.menu.getTablePos(),
                researchCostAfterDuplicateBook
        );

        int baseEnchantLapisCost = EnchantmentCostCalculator.getEnchantLapisCost(
                selectedEnchantment,
                nextItemLevel
        );

        int enchantCostAfterDuplicateBook = DuplicateBookDiscountHelper.getDiscountedEnchantLapisCost(
                baseEnchantLapisCost,
                nextItemLevel,
                this.menu.getDuplicateBookItem(),
                selectedEnchantment
        );

        int enchantLapisCost = EnvironmentCostModifier.applyMobHeadEnchantDiscount(
                this.minecraft.level,
                this.menu.getTablePos(),
                enchantCostAfterDuplicateBook
        );

        int enchantBookCost = 1;

        drawRightPageText(
                guiGraphics,
                Component.literal(enchantmentName),
                x,
                y
        );

        drawRightPageText(
                guiGraphics,
                Component.literal("Knowledge Level"),
                x,
                this.topPos + KNOWLEDGE_SECTION_Y
        );

        drawRightPageText(
                guiGraphics,
                Component.literal(
                        makeTextProgressBar(knowledgeLevel, maxLevel)
                                + " " + knowledgeLevel + "/" + maxLevel
                ),
                x,
                this.topPos + KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT
        );

        if (isMaxed(knowledgeLevel, maxLevel)) {
            drawRightPageText(
                    guiGraphics,
                    Component.literal("Maxed"),
                    x,
                    this.topPos + KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );
        } else {
            drawRightPageText(
                    guiGraphics,
                    Component.literal(
                            "Cost: " + researchXpCost + " XP"
                    ),
                    x,
                    this.topPos + KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );

            drawRightPageText(
                    guiGraphics,
                    Component.literal("[ Research ]"),
                    x,
                    this.topPos + KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT * 3
            );
        }

        drawRightPageText(
                guiGraphics,
                Component.literal("----------------"),
                x,
                this.topPos + KNOWLEDGE_SECTION_Y + DETAILS_LINE_HEIGHT * 4
        );

        drawRightPageText(
                guiGraphics,
                Component.literal("On Item"),
                x,
                this.topPos + ITEM_SECTION_Y
        );

        drawRightPageText(
                guiGraphics,
                Component.literal(
                        makeTextProgressBar(itemLevel, maxLevel)
                                + " " + itemLevel + "/" + maxLevel
                ),
                x,
                this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT
        );

        if (this.menu.getResearchItem().isEmpty()) {
            drawRightPageText(
                    guiGraphics,
                    Component.literal("Insert item"),
                    x,
                    this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );
        } else if (isMaxed(itemLevel, maxLevel)) {
            drawRightPageText(
                    guiGraphics,
                    Component.literal("Maxed"),
                    x,
                    this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );
        } else if (knowledgeLevel <= itemLevel) {
            drawRightPageText(
                    guiGraphics,
                    Component.literal("Research first"),
                    x,
                    this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );
        } else {
            drawRightPageText(
                    guiGraphics,
                    Component.literal(
                            "Cost: " + enchantLapisCost + " Lapis"
                    ),
                    x,
                    this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 2
            );

            drawRightPageText(
                    guiGraphics,
                    Component.literal("[ Enchant ]"),
                    x,
                    this.topPos + ITEM_SECTION_Y + DETAILS_LINE_HEIGHT * 3
            );
        }
    }

    private void drawRightPageText(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y
    ) {
        guiGraphics.drawString(
                this.font,
                text,
                x,
                y,
                TEXT_COLOR,
                false
        );
    }

    private int getDisplayedEnchantmentLevel(
            Holder<Enchantment> enchantmentHolder
    ) {
        ItemStack researchItem = this.menu.getResearchItem();

        if (!researchItem.isEmpty()) {
            return researchItem.getEnchantmentLevel(enchantmentHolder);
        }

        return getLibraryKnowledgeLevel(enchantmentHolder);
    }

    private int getLibraryKnowledgeLevel(
            Holder<Enchantment> enchantmentHolder
    ) {
        return this.menu.getLibraryLevel(enchantmentHolder);
    }

    private int getItemEnchantmentLevel(
            Holder<Enchantment> enchantmentHolder
    ) {
        ItemStack researchItem = this.menu.getResearchItem();

        if (researchItem.isEmpty()) {
            return 0;
        }

        return researchItem.getEnchantmentLevel(enchantmentHolder);
    }

    private boolean isMaxed(int currentLevel, int maxLevel) {
        return currentLevel >= maxLevel;
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

        int previousArrowX = this.leftPos + PAGE_PREVIOUS_X;
        int nextArrowX = this.leftPos + PAGE_NEXT_X;
        int arrowY = this.topPos + PAGE_Y;

        boolean canGoPrevious = this.enchantmentPage > 0;
        boolean canGoNext = this.enchantmentPage < totalPages - 1;

        boolean previousHovered = isMouseOver(
                mouseX,
                mouseY,
                previousArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        boolean nextHovered = isMouseOver(
                mouseX,
                mouseY,
                nextArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        renderPageArrow(
                guiGraphics,
                previousArrowX,
                arrowY,
                canGoPrevious ? ACTIVE_LEFT_ARROW : INACTIVE_LEFT_ARROW,
                canGoPrevious && previousHovered
        );

        renderPageArrow(
                guiGraphics,
                nextArrowX,
                arrowY,
                canGoNext ? ACTIVE_RIGHT_ARROW : INACTIVE_RIGHT_ARROW,
                canGoNext && nextHovered
        );

        Component pageText = Component.literal(
                (this.enchantmentPage + 1) + "/" + totalPages
        );

        guiGraphics.drawString(
                this.font,
                pageText,
                previousArrowX + PAGE_TEXT_X_OFFSET,
                arrowY + PAGE_TEXT_Y_OFFSET,
                TEXT_COLOR,
                false
        );
    }

    private void renderPageArrow(
            GuiGraphics guiGraphics,
            int x,
            int y,
            ResourceLocation arrowTexture,
            boolean showHoverOutline
    ) {
        guiGraphics.blit(
                arrowTexture,
                x,
                y,
                0,
                0,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        if (showHoverOutline) {
            guiGraphics.blit(
                    PAGE_ARROW_HOVER_TEXTURE,
                    x,
                    y,
                    0,
                    0,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE
            );
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (handlePageButtonClick(mouseX, mouseY)) {
                return true;
            }

            if (handleResearchButtonClick(mouseX, mouseY)) {
                return true;
            }

            if (handleEnchantButtonClick(mouseX, mouseY)) {
                return true;
            }

            if (handleEnchantmentRowClick(mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePageButtonClick(
            double mouseX,
            double mouseY
    ) {
        int totalPages = getTotalPages(
                this.menu.getVisibleEnchantments().size()
        );

        if (totalPages <= 1) {
            return false;
        }

        int previousArrowX = this.leftPos + PAGE_PREVIOUS_X;
        int nextArrowX = this.leftPos + PAGE_NEXT_X;
        int arrowY = this.topPos + PAGE_Y;

        if (isMouseOver(
                mouseX,
                mouseY,
                previousArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        )) {
            if (this.enchantmentPage > 0) {
                this.enchantmentPage--;
                playPageFlipSound();
            }

            return true;
        }

        if (isMouseOver(
                mouseX,
                mouseY,
                nextArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        )) {
            if (this.enchantmentPage < totalPages - 1) {
                this.enchantmentPage++;
                playPageFlipSound();
            }

            return true;
        }

        return false;
    }

    private boolean handleEnchantmentRowClick(
            double mouseX,
            double mouseY
    ) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int firstIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int lastIndex = Math.min(firstIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int rowX = this.leftPos + ENCHANT_ROW_X;

        for (int enchantmentIndex = firstIndex; enchantmentIndex < lastIndex; enchantmentIndex++) {
            int rowIndexOnPage = enchantmentIndex - firstIndex;

            int rowY = this.topPos
                    + ENCHANT_ROW_Y
                    + rowIndexOnPage * ENCHANT_ROW_SPACING;

            if (!isMouseOver(
                    mouseX,
                    mouseY,
                    rowX,
                    rowY,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            )) {
                continue;
            }

            ResourceLocation enchantmentId = getEnchantmentId(
                    enchantments.get(enchantmentIndex)
            );

            if (enchantmentId != null) {
                boolean changedSelection = !enchantmentId.equals(this.selectedEnchantmentId);

                this.selectedEnchantmentId = enchantmentId;

                if (changedSelection) {
                    playPageFlipSound();
                }
            }

            return true;
        }

        return false;
    }

    private boolean handleResearchButtonClick(
            double mouseX,
            double mouseY
    ) {
        Holder<Enchantment> selectedEnchantment = getSelectedEnchantment();

        if (selectedEnchantment == null) {
            return false;
        }

        ResourceLocation enchantmentId = getEnchantmentId(selectedEnchantment);

        if (enchantmentId == null) {
            return false;
        }

        int buttonX = this.leftPos + RESEARCH_BUTTON_X;
        int buttonY = this.topPos + RESEARCH_BUTTON_Y;

        if (!isMouseOver(
                mouseX,
                mouseY,
                buttonX,
                buttonY,
                RESEARCH_BUTTON_WIDTH,
                RESEARCH_BUTTON_HEIGHT
        )) {
            return false;
        }

        int knowledgeLevel = getLibraryKnowledgeLevel(selectedEnchantment);
        int maxLevel = Math.max(1, selectedEnchantment.value().getMaxLevel());

        if (isMaxed(knowledgeLevel, maxLevel)) {
            return false;
        }

        PacketDistributor.sendToServer(
                new ResearchEnchantmentPayload(enchantmentId)
        );

        return true;
    }

    private boolean handleEnchantButtonClick(
            double mouseX,
            double mouseY
    ) {
        Holder<Enchantment> selectedEnchantment = getSelectedEnchantment();

        if (selectedEnchantment == null) {
            return false;
        }

        ResourceLocation enchantmentId = getEnchantmentId(selectedEnchantment);

        if (enchantmentId == null) {
            return false;
        }

        int buttonX = this.leftPos + ENCHANT_BUTTON_X;
        int buttonY = this.topPos + ENCHANT_BUTTON_Y;

        if (!isMouseOver(
                mouseX,
                mouseY,
                buttonX,
                buttonY,
                ENCHANT_BUTTON_WIDTH,
                ENCHANT_BUTTON_HEIGHT
        )) {
            return false;
        }

        ItemStack researchItem = this.menu.getResearchItem();

        if (researchItem.isEmpty()) {
            return false;
        }

        int knowledgeLevel = getLibraryKnowledgeLevel(selectedEnchantment);
        int itemLevel = getItemEnchantmentLevel(selectedEnchantment);
        int maxLevel = Math.max(1, selectedEnchantment.value().getMaxLevel());

        if (isMaxed(itemLevel, maxLevel)) {
            return false;
        }

        if (knowledgeLevel <= itemLevel) {
            return false;
        }

        PacketDistributor.sendToServer(
                new EnchantItemPayload(enchantmentId)
        );

        return true;
    }

    private void clampCurrentPage(int totalPages) {
        if (this.enchantmentPage >= totalPages) {
            this.enchantmentPage = Math.max(0, totalPages - 1);
        }
    }

    private int getTotalPages(int enchantmentCount) {
        if (enchantmentCount <= 0) {
            return 1;
        }

        return (int) Math.ceil(
                enchantmentCount / (double) ENCHANTMENTS_PER_PAGE
        );
    }

    private Holder<Enchantment> getSelectedEnchantment() {
        if (this.selectedEnchantmentId == null) {
            return null;
        }

        for (Holder<Enchantment> enchantmentHolder : this.menu.getVisibleEnchantments()) {
            ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

            if (this.selectedEnchantmentId.equals(enchantmentId)) {
                return enchantmentHolder;
            }
        }

        return null;
    }

    private ResourceLocation getEnchantmentId(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    private String makeTextProgressBar(int currentLevel, int maxLevel) {
        int safeMaxLevel = Math.max(1, maxLevel);
        int clampedCurrentLevel = Math.max(0, Math.min(currentLevel, safeMaxLevel));

        int filledUntil = clampedCurrentLevel
                * TEXT_PROGRESS_BAR_WIDTH
                / safeMaxLevel;

        StringBuilder progressBar = new StringBuilder("[");

        for (int i = 1; i <= TEXT_PROGRESS_BAR_WIDTH; i++) {
            if (i <= filledUntil) {
                progressBar.append("#");
            } else {
                progressBar.append("-");
            }
        }

        progressBar.append("]");

        return progressBar.toString();
    }

    private Component shortenTextToFit(Component text, int maxWidth) {
        String fullText = text.getString();

        if (this.font.width(fullText) <= maxWidth) {
            return Component.literal(fullText);
        }

        String dots = ".";
        int availableWidth = maxWidth - this.font.width(dots);

        String shortenedText = fullText;

        while (!shortenedText.isEmpty()
                && this.font.width(shortenedText) > availableWidth) {

            shortenedText = shortenedText.substring(
                    0,
                    shortenedText.length() - 1
            );
        }

        return Component.literal(shortenedText + dots);
    }

    private void playPageFlipSound() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.playSound(
                    SoundEvents.BOOK_PAGE_TURN,
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