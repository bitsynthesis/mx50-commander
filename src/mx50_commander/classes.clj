(ns mx50-commander.classes)


(gen-class
 :name mx50_commander.classes.DeviceFileFilter
 :extends javax.swing.filechooser.FileFilter
 :prefix deviceFileFilter-)


(defn deviceFileFilter-accept [_ file]
  (not (nil? (re-find #"^/dev/ttyUSB[0-9]+" (.getPath file)))))


(defn deviceFileFilter-getDescription [_]
  "USB devices")
