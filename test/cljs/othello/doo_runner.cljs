(ns othello.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [othello.core-test]))

(doo-tests 'othello.core-test)

