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
}
