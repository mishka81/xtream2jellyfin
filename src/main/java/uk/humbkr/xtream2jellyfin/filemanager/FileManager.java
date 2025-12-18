package uk.humbkr.xtream2jellyfin.filemanager;

public interface FileManager {

    static Object get(String path, Object defaultValue) {
        return FileManagerUtils.get(path, defaultValue);
    }

    void onProcessStart();

    void onProcessEnd();

    void save(String path, Object content, String date);

}
