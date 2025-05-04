(ns sport-quiz.ui-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.ui :as ui]))

(facts "read-int"
       (fact "parses a valid integer"
             (with-in-str "42\n"
               (ui/read-int)) => 42)

       (fact "returns -1 for invalid input"
             (with-in-str "abc\n"
               (ui/read-int)) => -1)

       (fact "returns -1 for empty input"
             (with-in-str "\n"
               (ui/read-int)) => -1))