package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.input.KeyCodes;
import org.lwjgl.input.Keyboard;

/**
 * Converts LWJGL 2 keyboard codes into UniGUI's GLFW-compatible key codes.
 */
final class CleanroomKeyCodes {
    private CleanroomKeyCodes() {
    }

    static int toCore(int keyCode) {
        return switch (keyCode) {
            case Keyboard.KEY_RETURN -> KeyCodes.ENTER;
            case Keyboard.KEY_NUMPADENTER -> KeyCodes.KEYPAD_ENTER;
            case Keyboard.KEY_TAB -> KeyCodes.TAB;
            case Keyboard.KEY_ESCAPE -> KeyCodes.ESCAPE;
            case Keyboard.KEY_SPACE -> KeyCodes.SPACE;
            case Keyboard.KEY_BACK -> KeyCodes.BACKSPACE;
            case Keyboard.KEY_DELETE -> KeyCodes.DELETE;
            case Keyboard.KEY_RIGHT -> KeyCodes.RIGHT;
            case Keyboard.KEY_LEFT -> KeyCodes.LEFT;
            case Keyboard.KEY_DOWN -> KeyCodes.DOWN;
            case Keyboard.KEY_UP -> KeyCodes.UP;
            case Keyboard.KEY_PRIOR -> KeyCodes.PAGE_UP;
            case Keyboard.KEY_NEXT -> KeyCodes.PAGE_DOWN;
            case Keyboard.KEY_HOME -> KeyCodes.HOME;
            case Keyboard.KEY_END -> KeyCodes.END;
            case Keyboard.KEY_EQUALS -> KeyCodes.EQUAL;
            case Keyboard.KEY_MINUS -> KeyCodes.MINUS;
            case Keyboard.KEY_LSHIFT -> KeyCodes.LEFT_SHIFT;
            case Keyboard.KEY_LCONTROL -> KeyCodes.LEFT_CONTROL;
            case Keyboard.KEY_LMENU -> KeyCodes.LEFT_ALT;
            case Keyboard.KEY_LMETA -> KeyCodes.LEFT_SUPER;
            case Keyboard.KEY_RSHIFT -> KeyCodes.RIGHT_SHIFT;
            case Keyboard.KEY_RCONTROL -> KeyCodes.RIGHT_CONTROL;
            case Keyboard.KEY_RMENU -> KeyCodes.RIGHT_ALT;
            case Keyboard.KEY_RMETA -> KeyCodes.RIGHT_SUPER;
            case Keyboard.KEY_F1 -> KeyCodes.F1;
            case Keyboard.KEY_F2 -> KeyCodes.F2;
            case Keyboard.KEY_F3 -> KeyCodes.F3;
            case Keyboard.KEY_F4 -> KeyCodes.F4;
            case Keyboard.KEY_F5 -> KeyCodes.F5;
            case Keyboard.KEY_F6 -> KeyCodes.F6;
            case Keyboard.KEY_F7 -> KeyCodes.F7;
            case Keyboard.KEY_F8 -> KeyCodes.F8;
            case Keyboard.KEY_F9 -> KeyCodes.F9;
            case Keyboard.KEY_F10 -> KeyCodes.F10;
            case Keyboard.KEY_F11 -> KeyCodes.F11;
            case Keyboard.KEY_F12 -> KeyCodes.F12;
            case Keyboard.KEY_A -> KeyCodes.A;
            case Keyboard.KEY_B -> 'B';
            case Keyboard.KEY_C -> KeyCodes.C;
            case Keyboard.KEY_D -> KeyCodes.D;
            case Keyboard.KEY_E -> KeyCodes.E;
            case Keyboard.KEY_F -> 'F';
            case Keyboard.KEY_G -> 'G';
            case Keyboard.KEY_H -> 'H';
            case Keyboard.KEY_I -> 'I';
            case Keyboard.KEY_J -> 'J';
            case Keyboard.KEY_K -> 'K';
            case Keyboard.KEY_L -> 'L';
            case Keyboard.KEY_M -> 'M';
            case Keyboard.KEY_N -> 'N';
            case Keyboard.KEY_O -> 'O';
            case Keyboard.KEY_P -> KeyCodes.P;
            case Keyboard.KEY_Q -> KeyCodes.Q;
            case Keyboard.KEY_R -> KeyCodes.R;
            case Keyboard.KEY_S -> KeyCodes.S;
            case Keyboard.KEY_T -> 'T';
            case Keyboard.KEY_U -> KeyCodes.U;
            case Keyboard.KEY_V -> KeyCodes.V;
            case Keyboard.KEY_W -> KeyCodes.W;
            case Keyboard.KEY_X -> KeyCodes.X;
            case Keyboard.KEY_Y -> KeyCodes.Y;
            case Keyboard.KEY_Z -> KeyCodes.Z;
            case Keyboard.KEY_0 -> '0';
            case Keyboard.KEY_1 -> '1';
            case Keyboard.KEY_2 -> '2';
            case Keyboard.KEY_3 -> '3';
            case Keyboard.KEY_4 -> '4';
            case Keyboard.KEY_5 -> '5';
            case Keyboard.KEY_6 -> '6';
            case Keyboard.KEY_7 -> '7';
            case Keyboard.KEY_8 -> '8';
            case Keyboard.KEY_9 -> '9';
            case Keyboard.KEY_ADD -> KeyCodes.KEYPAD_ADD;
            case Keyboard.KEY_SUBTRACT -> KeyCodes.KEYPAD_SUBTRACT;
            default -> -1;
        };
    }
}
