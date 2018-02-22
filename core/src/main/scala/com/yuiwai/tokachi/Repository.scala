package com.yuiwai.tokachi

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

trait Repository[E <: Entity] {
  def lock(id: E#ID): Future[Option[E]]
  def find(id: E#ID): Future[Option[E]]
  def insert(entity: E): Future[Int]
  def update(entity: E): Future[Int]
}

trait InMemoryRepository[E <: Entity] extends Repository[E] {
  private val data: TrieMap[E#ID, E] = TrieMap.empty
  override def lock(id: E#ID): Future[Option[E]] = Future.successful(data.get(id))
  override def find(id: E#ID): Future[Option[E]] = Future.successful(data.get(id))
  override def insert(entity: E): Future[Int] = {
    Future.successful {
      if (data.contains(entity.id)) 0
      else {
        data.update(entity.id, entity)
        1
      }
    }
  }
  override def update(entity: E): Future[Int] = {
    Future.successful {
      if (!data.contains(entity.id)) 0
      else {
        data.update(entity.id, entity)
        1
      }
    }
  }
}
