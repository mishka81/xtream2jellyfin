package uk.humbkr.xtream2jellyfin.filemanager;

public interface FileManager {

    void initialize();

    void complete();

    void save(String path, Object content, String date);

}
