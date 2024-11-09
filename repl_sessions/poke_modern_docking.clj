(ns repl-sessions.poke-modern-docking
  (:import
   (java.awt BorderLayout)
   (javax.swing JFrame JPanel SwingConstants SwingUtilities)
   (ModernDocking Dockable DockableStyle DockingRegion)
   (ModernDocking.app Docking RootDockingPanel)
   (ModernDocking.ui DefaultHeaderUI)))

(def frame (JFrame.))

(SwingUtilities/invokeLater
 (fn []
   (.setVisible frame true)
   (.setSize frame 400 500)
   (.setTitle frame "foo")))

(Docking/initialize frame)

(def root (RootDockingPanel. frame))

(.add frame root BorderLayout/CENTER)

(def dockables (for [i (range 5)
                     :let [id (str (random-uuid))]]
                 (proxy [JPanel Dockable] []
                   (getPersistentID []
                     id)
                   (getTabText []
                     (str "dock-" i))
                   (createHeaderUI [a b]
                     (DefaultHeaderUI. a  b))
                   (getTabPosition []
                     SwingConstants/BOTTOM)
                   (isWrappableInScrollpane []
                     true)
                   (getIcon [])
                   (getHasMoreOptions []
                     false)
                   (isMinMaxAllowed []
                     false)
                   (isPinningAllowed []
                     false)
                   (isAutoHideAllowed []
                     false)
                   (isFloatingAllowed []
                     false)
                   (isClosable []
                     true)
                   (isLimitedToRoot []
                     false)
                   (updateProperties [])
                   (getStyle []
                     DockableStyle/BOTH)
                   (getTabTooltip []
                     "tooltop"))))

(run! Docking/registerDockable dockables)

(Docking/dock (nth dockables 0) frame)
(Docking/dock (nth dockables 2) frame
              DockingRegion/EAST)

;; (run! #(Docking/dock % frame) dockables)


;; System.setProperty("apple.laf.useScreenMenuBar", "true");
;; System.setProperty("apple.awt.application.name", "My app");
