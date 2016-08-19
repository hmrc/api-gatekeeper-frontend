/*
 * Copyright 2016 HM Revenue & Customs
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

package model

case class PageableCollection[A](items: Seq[A], pageNumber: Int, pageSize: Int) {
  def start = {
    val offset = (pageNumber - 1) * pageSize
    if (offset < 0 || offset > length) 0
    else offset
  }
  def end = math.min(length, start + pageSize)
  def length = items.length

  def projection: Seq[A] = {
    if (hasNext || hasPrevious || length <= pageSize) {
      items.toList.slice(start, end)
    } else {
      Nil
    }
  }

  def valid = {
    val offset = (pageNumber - 1) * pageSize
    offset == 0 || offset > 0 && offset < length
  }
  def hasPrevious = valid && pageNumber > 1
  def hasNext = valid && pageNumber * pageSize < length
}
