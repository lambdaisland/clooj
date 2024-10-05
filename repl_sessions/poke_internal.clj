(require 'clooj.main
         '[clooj.state :as state]
         '[clooj.gui :as gui]
         '[clooj.text-area :as text-area]
         '[clooj.document :as doc]
         '[clooj.analysis.parse-tree :as parse-tree])

@clooj.main/current-app



(swap! state/component-config assoc-in [:doc-text-area :buffer] "*scratch*")

(clojure.reflect/reflect
 javax.swing.text.JTextComponent
 #_javax.swing.JTextArea
 #_org.fife.ui.rtextarea.RTextAreaBase
 #_ org.fife.ui.rtextarea.RTextArea
 #_ (clooj.gui/resolve :doc-text-area))

( (clooj.gui/resolve :doc-text-area))
