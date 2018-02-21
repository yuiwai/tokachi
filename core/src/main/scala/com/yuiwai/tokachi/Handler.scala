package com.yuiwai.tokachi

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Handler[A <: Aggregation] {
  type Root = A#Root
  val rootRepository: Repository[Root]
  private val locked: mutable.Map[Root#ID, A] = mutable.Map.empty
  def makeAggregation(entity: Root): A
  def isLocked(aggregation: A): Boolean = locked.contains(aggregation.root.id.asInstanceOf[Root#ID])
  def lock(id: Root#ID): Future[Option[A]] = {
    rootRepository.lock(id).map(_.map { root =>
      val agg = makeAggregation(root)
      locked.update(id, agg)
      agg
    })
  }
  def find[T <: Entity](repository: Repository[T], id: T#ID): Future[Option[T]] = repository.find(id)
  def save(aggregation: A): Future[Int] = {
    Future.sequence(
      (if (isLocked(aggregation)) update(aggregation.root) else insert(aggregation.root)) ::
        saveChildren(aggregation) ::
        Nil
    ).map(_.sum)
  }
  def saveChildren(aggregation: A): Future[Int] = Future.successful(0)
  def insert(entity: Root): Future[Int] = rootRepository.insert(entity)
  def update(entity: Root): Future[Int] = rootRepository.update(entity)
  def flush(): Unit = {
    locked.clear()
  }
}
