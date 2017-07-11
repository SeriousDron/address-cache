package model

trait Cache[E] {

  /** Adds element in cache and returns if it was successfully added
   *
   * @param el Element to add
   */
  def add(el: E): Boolean

  /** Removes element from cache and returns if it was successfully removed
   *
   * @param el Element to remove
   */
  def remove(el: E): Boolean

  /** Returns last added element or None
   *
   * @return Last element added or None
   */
  def peek(): Option[E]

  /**
   * Returns last element added. Blocks and wait if there are no elements
   *
   */
  def take(): E
}
