(ns sport-quiz.core-test
  (:require [midje.sweet :refer :all]
            [sport-quiz.core :as core]))

(facts "read-int"
       (fact "parses a valid integer"
             (with-in-str "42\n"
               (core/read-int)) => 42)
       (fact "returns -1 for invalid input"
             (with-in-str "abc\n"
               (core/read-int)) => -1)
       (fact "returns -1 for empty input"
             (with-in-str "\n"
               (core/read-int)) => -1))