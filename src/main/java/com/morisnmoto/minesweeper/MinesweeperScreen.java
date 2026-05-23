package com.morisnmoto.minesweeper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class MinesweeperScreen extends Screen {
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int MINES = 15;
    private static final int CELL_SIZE = 22;
    private static final int BOARD_WIDTH = COLS * CELL_SIZE;
    private static final int BOARD_HEIGHT = ROWS * CELL_SIZE;

    private static final int COLOR_BACKGROUND = 0xEE101010;
    private static final int COLOR_PANEL = 0xFF202020;
    private static final int COLOR_CLOSED = 0xFF666666;
    private static final int COLOR_CLOSED_HOVER = 0xFF7A7A7A;
    private static final int COLOR_OPEN = 0xFFBDBDBD;
    private static final int COLOR_GRID = 0xFF2B2B2B;
    private static final int COLOR_MINE = 0xFFB71C1C;
    private static final int COLOR_FLAG = 0xFFFFD54F;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFFAAAAAA;

    private final Random random = new Random();

    private int[][] cells = new int[ROWS][COLS]; // -1 = mine, 0-8 = nearby mines.
    private boolean[][] opened = new boolean[ROWS][COLS];
    private boolean[][] flagged = new boolean[ROWS][COLS];

    private boolean generated;
    private boolean gameOver;
    private boolean won;
    private int flagsUsed;

    private int boardX;
    private int boardY;

    public MinesweeperScreen() {
        super(Component.literal("Сапёр"));
        resetGame();
    }

    @Override
    protected void init() {
        boardX = (width - BOARD_WIDTH) / 2;
        boardY = Math.max(58, (height - BOARD_HEIGHT) / 2 + 12);

        int buttonY = boardY + BOARD_HEIGHT + 14;
        addRenderableWidget(Button.builder(Component.literal("Новая игра"), button -> resetGame())
                .bounds(width / 2 - 102, buttonY, 98, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
                .bounds(width / 2 + 4, buttonY, 98, 20)
                .build());
    }

    private void resetGame() {
        cells = new int[ROWS][COLS];
        opened = new boolean[ROWS][COLS];
        flagged = new boolean[ROWS][COLS];
        generated = false;
        gameOver = false;
        won = false;
        flagsUsed = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, COLOR_BACKGROUND);

        Component title = Component.literal("Сапёр в Minecraft").withStyle(ChatFormatting.BOLD);
        graphics.drawCenteredString(font, title, width / 2, 18, COLOR_TEXT);

        Component help = Component.literal("ЛКМ — открыть | ПКМ — флаг | Ж — открыть игру | Esc — закрыть");
        graphics.drawCenteredString(font, help, width / 2, 34, COLOR_MUTED);

        String status = getStatusText();
        graphics.drawCenteredString(font, Component.literal(status), width / 2, 48, getStatusColor());

        graphics.fill(boardX - 6, boardY - 6, boardX + BOARD_WIDTH + 6, boardY + BOARD_HEIGHT + 6, COLOR_PANEL);
        renderBoard(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private String getStatusText() {
        if (won) {
            return "Победа! Все безопасные клетки открыты.";
        }
        if (gameOver) {
            return "Ты подорвался. Нажми «Новая игра».";
        }
        return "Мины: " + MINES + " | Флаги: " + flagsUsed + "/" + MINES;
    }

    private int getStatusColor() {
        if (won) {
            return 0xFF66FF66;
        }
        if (gameOver) {
            return 0xFFFF7777;
        }
        return COLOR_TEXT;
    }

    private void renderBoard(GuiGraphics graphics, int mouseX, int mouseY) {
        int hoverRow = getRow(mouseY);
        int hoverCol = getCol(mouseX);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = boardX + col * CELL_SIZE;
                int y = boardY + row * CELL_SIZE;
                boolean hover = row == hoverRow && col == hoverCol && isInsideBoard(mouseX, mouseY);

                renderCell(graphics, row, col, x, y, hover);
            }
        }
    }

    private void renderCell(GuiGraphics graphics, int row, int col, int x, int y, boolean hover) {
        int right = x + CELL_SIZE;
        int bottom = y + CELL_SIZE;

        graphics.fill(x, y, right, bottom, COLOR_GRID);

        if (opened[row][col]) {
            if (cells[row][col] == -1) {
                graphics.fill(x + 1, y + 1, right - 1, bottom - 1, COLOR_MINE);
                graphics.drawCenteredString(font, Component.literal("✹"), x + CELL_SIZE / 2, y + 7, COLOR_TEXT);
            } else {
                graphics.fill(x + 1, y + 1, right - 1, bottom - 1, COLOR_OPEN);
                if (cells[row][col] > 0) {
                    graphics.drawCenteredString(font, Component.literal(String.valueOf(cells[row][col])),
                            x + CELL_SIZE / 2, y + 7, getNumberColor(cells[row][col]));
                }
            }
            return;
        }

        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, hover ? COLOR_CLOSED_HOVER : COLOR_CLOSED);

        if (flagged[row][col]) {
            graphics.drawCenteredString(font, Component.literal("⚑"), x + CELL_SIZE / 2, y + 7, COLOR_FLAG);
        } else if (gameOver && cells[row][col] == -1) {
            graphics.drawCenteredString(font, Component.literal("✹"), x + CELL_SIZE / 2, y + 7, COLOR_MINE);
        }
    }

    private int getNumberColor(int number) {
        return switch (number) {
            case 1 -> 0xFF1565C0;
            case 2 -> 0xFF2E7D32;
            case 3 -> 0xFFC62828;
            case 4 -> 0xFF283593;
            case 5 -> 0xFF6D4C41;
            case 6 -> 0xFF00838F;
            case 7 -> 0xFF111111;
            case 8 -> 0xFF555555;
            default -> COLOR_TEXT;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInsideBoard((int) mouseX, (int) mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int row = getRow((int) mouseY);
        int col = getCol((int) mouseX);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftClick(row, col);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightClick(row, col);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void leftClick(int row, int col) {
        if (gameOver || won || flagged[row][col] || opened[row][col]) {
            return;
        }

        if (!generated) {
            generateBoard(row, col);
        }

        if (cells[row][col] == -1) {
            opened[row][col] = true;
            revealAllMines();
            gameOver = true;
            return;
        }

        reveal(row, col);
        checkWin();
    }

    private void rightClick(int row, int col) {
        if (gameOver || won || opened[row][col]) {
            return;
        }

        if (!flagged[row][col] && flagsUsed >= MINES) {
            return;
        }

        flagged[row][col] = !flagged[row][col];
        flagsUsed += flagged[row][col] ? 1 : -1;
    }

    private void generateBoard(int safeRow, int safeCol) {
        int placed = 0;
        while (placed < MINES) {
            int row = random.nextInt(ROWS);
            int col = random.nextInt(COLS);

            if (cells[row][col] == -1 || isInSafeZone(row, col, safeRow, safeCol)) {
                continue;
            }

            cells[row][col] = -1;
            placed++;
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (cells[row][col] != -1) {
                    cells[row][col] = countMinesAround(row, col);
                }
            }
        }

        generated = true;
    }

    private boolean isInSafeZone(int row, int col, int safeRow, int safeCol) {
        return Math.abs(row - safeRow) <= 1 && Math.abs(col - safeCol) <= 1;
    }

    private int countMinesAround(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }

                int nr = row + dr;
                int nc = col + dc;
                if (isValidCell(nr, nc) && cells[nr][nc] == -1) {
                    count++;
                }
            }
        }
        return count;
    }

    private void reveal(int startRow, int startCol) {
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];

            if (!isValidCell(row, col) || opened[row][col] || flagged[row][col]) {
                continue;
            }

            opened[row][col] = true;

            if (cells[row][col] != 0) {
                continue;
            }

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }

                    int nr = row + dr;
                    int nc = col + dc;
                    if (isValidCell(nr, nc) && !opened[nr][nc] && !flagged[nr][nc]) {
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (cells[row][col] == -1) {
                    opened[row][col] = true;
                }
            }
        }
    }

    private void checkWin() {
        int safeOpened = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (cells[row][col] != -1 && opened[row][col]) {
                    safeOpened++;
                }
            }
        }

        if (safeOpened == ROWS * COLS - MINES) {
            won = true;
        }
    }

    private boolean isInsideBoard(int mouseX, int mouseY) {
        return mouseX >= boardX && mouseX < boardX + BOARD_WIDTH
                && mouseY >= boardY && mouseY < boardY + BOARD_HEIGHT;
    }

    private int getRow(int mouseY) {
        return (mouseY - boardY) / CELL_SIZE;
    }

    private int getCol(int mouseX) {
        return (mouseX - boardX) / CELL_SIZE;
    }

    private boolean isValidCell(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
