package com.yuiwai.tokachi

trait Entity {
  type ID
  val id: ID
}

trait Aggregation {
  type Root <: AggregationRootEntity
  val root: Root
  val handler: Handler[_]
}

trait AggregationRootEntity extends Entity
case class Entities[E <: Entity](added: Map[E#ID, E]) {
  def add(entity: E): Entities[E] = copy(added = added.updated(entity.id, entity))
}
object Entities {
  def empty[E <: Entity]: Entities[E] = Entities[E](Map.empty)
}
