package dev.sixik.unigui.widgets.caching;



public enum CachedSubtreeMissReason {
    NONE,
    NO_TEXTURE,
    MANUAL_DIRTY,
    RESIZED,
    TARGET_OPTIONS_CHANGED,
    BACKEND_CHANGED,
    OWN_LAYOUT_DIRTY,
    OWN_TEXTURE_DIRTY,
    CHILD_SUBTREE_DIRTY
}
