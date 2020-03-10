package grass.plugins

public enum PluginEventType {
    INIT, BEFORE_INDEX, AFTER_INDEX, BEFORE_WRITE, AFTER_WRITE,
        BEFORE_PAGE, RENDER_PAGE, AFTER_PAGE, SETUP_BINDING,
        CLEANUP
}
