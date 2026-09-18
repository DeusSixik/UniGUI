package dev.sixik.isf.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/** Собирает прямоугольный pattern крафта из плоского списка ингредиентов. */
public final class IsfCraftingPattern {
    private IsfCraftingPattern() {
    }

    /**
     * Раскладывает клетки (массивы альтернатив) в строки по {@code columns},
     * дополняя короткие строки пустыми клетками.
     *
     * @param cells   плоский список клеток row-major; клетка — JsonArray альтернатив
     * @param columns ингредиентов в строке (ширина крафта)
     * @return массив строк: cells → alternatives → item id
     */
    public static JsonArray reshape(JsonArray cells, int columns) {
        int width = Math.max(1, columns);
        JsonArray rows = new JsonArray();
        if (cells == null || cells.size() == 0) return rows;
        int rowCount = (cells.size() + width - 1) / width;
        for (int row = 0; row < rowCount; row++) {
            JsonArray rowCells = new JsonArray();
            for (int column = 0; column < width; column++) {
                int index = row * width + column;
                JsonElement cell = index < cells.size() ? cells.get(index) : null;
                rowCells.add(cell != null && cell.isJsonArray()
                        ? cell.getAsJsonArray().deepCopy()
                        : new JsonArray());
            }
            rows.add(rowCells);
        }
        return rows;
    }

    /**
     * Центрирует reshaped-паттерн в сетке {@code columns}×{@code rowCount}
     * (как в JEI: предметы стоят на своих местах ванильной формы, а пустые
     * клетки вокруг остаются под текстуру слота).
     */
    public static JsonArray center(JsonArray rows, int columns, int rowCount) {
        int patternHeight = rows == null ? 0 : rows.size();
        int patternWidth = 0;
        if (rows != null) {
            for (JsonElement row : rows) {
                if (row.isJsonArray()) {
                    patternWidth = Math.max(patternWidth, row.getAsJsonArray().size());
                }
            }
        }
        int offsetX = Math.max(0, (columns - patternWidth) / 2);
        int offsetY = Math.max(0, (rowCount - patternHeight) / 2);
        JsonArray grid = new JsonArray();
        for (int row = 0; row < rowCount; row++) {
            JsonArray rowCells = new JsonArray();
            for (int column = 0; column < columns; column++) {
                int sourceRow = row - offsetY;
                int sourceColumn = column - offsetX;
                JsonElement cell = null;
                if (sourceRow >= 0 && sourceRow < patternHeight && sourceColumn >= 0) {
                    JsonArray source = rows.get(sourceRow).getAsJsonArray();
                    if (sourceColumn < source.size()) cell = source.get(sourceColumn);
                }
                rowCells.add(cell != null && cell.isJsonArray()
                        ? cell.getAsJsonArray().deepCopy()
                        : new JsonArray());
            }
            grid.add(rowCells);
        }
        return grid;
    }
}
