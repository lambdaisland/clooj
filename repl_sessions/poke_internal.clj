(require 'clooj.main
         '[clooj.state :as state])

@clooj.main/current-app



(swap! state/component-config assoc-in [:doc-text-area :buffer] "*scratch*")
