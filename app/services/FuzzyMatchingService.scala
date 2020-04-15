/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

class FuzzyMatchingService {

  def score(s1: String, s2: String, n: Int = 1): Double = overlap(s1.toCharArray, s2.toCharArray, n)

  private def overlap[T](s1: Array[T], s2: Array[T], n: Int = 1): Double = {
    require(n > 0, "Overlap score, ngram size must be a positive number.")
    foldNGram(s1, s2, n)(0d)(_ => 1d) {
      (s1Tok, s2Tok, dist) => dist.toDouble / math.min(s1Tok.length, s2Tok.length)
    }
  }

  private def foldNGram[T, R](s1: Array[T], s2: Array[T], n: Int = 1)
                               (err: => R)(success: Int => R)
                               (fuzzy: (List[List[T]], List[List[T]], Int) => R): R = {
    if (n <= 0 || s1.length < n || s2.length < n) { err }
    else if (s1.sameElements(s2)) {
      val s1Tokenized = tokenizeNGram(s1, n)
      success(s1Tokenized.length)
    }
    else {
      val s1Tokenized = tokenizeNGram(s1, n)
      val s2Tokenized = tokenizeNGram(s2, n)
      val intersectionLength = intersectLength(s1Tokenized, s2Tokenized)
      fuzzy(s1Tokenized, s2Tokenized, intersectionLength)
    }
  }

  private def intersectLength[T]: (List[List[T]], List[List[T]]) => Int = (mt1, mt2) => mt1.intersect(mt2).length

  private def tokenize[T](a: List[T], n: Int): List[List[T]] =
    sequence(a, List.empty, n)

  private def tokenizeNGram[T](a: Array[T], n: Int): List[List[T]] = tokenize(a.toList, n)

  @annotation.tailrec
  private def sequence[T](i: List[T], o: List[List[T]], n: Int) : List[List[T]] = {
    if (i.length <= n) { o :+ i }
    else { sequence[T](i.tail, o :+ i.take(n), n) }
  }
}