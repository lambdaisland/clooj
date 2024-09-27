(ns poke-keymaps
  (:import
   (java.awt KeyboardFocusManager)
   (org.fife.ui.rsyntaxtextarea RSyntaxTextArea)))

(.getFocusOwner (KeyboardFocusManager/getCurrentKeyboardFocusManager))

(filter #(instance? RSyntaxTextArea (val %)) @clooj.main/current-app)
;; => (:docs-tree
;;     :repl-out-scroll-pane
;;     :arglist-label
;;     :frame
;;     :classpath-queue
;;     :repl-in-text-area
;;     :repl-label
;;     :docs-tree-panel
;;     :docs-tree-label
;;     :help-text-area
;;     :file
;;     :doc-text-area
;;     :settings
;;     :var-maps
;;     :docs-tree-scroll-pane
;;     :help-text-scroll-pane
;;     :split-pane
;;     :search-match-case-checkbox
;;     :changed
;;     :repl
;;     :repl-out-text-area
;;     :search-text-area
;;     :repl-split-pane
;;     :doc-label
;;     :completion-panel
;;     :repl-out-writer
;;     :search-regex-checkbox
;;     :completion-list
;;     :pos-label
;;     :completion-scroll-pane
;;     :search-close-button
;;     :doc-split-pane)

(seq
 (.keys (.getInputMap (:doc-text-area @clooj.main/current-app))))
