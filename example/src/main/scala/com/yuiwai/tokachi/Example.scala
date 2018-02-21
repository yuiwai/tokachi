package com.yuiwai.tokachi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Example extends App {
  implicit val userAssetHandler: UserAssetHandler =
    new UserAssetHandler(new UserAssetRepository, new UserWeaponRepository)

  val userId = 10
  val weaponId = 12
  assert(await(userAssetHandler.insert(UserAsset(userId))) == 1)
  val userAssetAgg = await(userAssetHandler.lock(userId)).map { agg =>
    agg.addWeapon(weaponId)
  }.get
  assert(userAssetAgg.addedWeapons.size == 1)
  assert(userAssetAgg.root.nextWeaponId == 2)
  assert(await(userAssetHandler.save(userAssetAgg)) == 2)
  assert(await(userAssetAgg.findUserWeapon(1)).isDefined)
  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}

case class UserAssetAggregation(
  root: UserAsset,
  userWeapons: Entities[UserWeapon],
  handler: UserAssetHandler)
  extends Aggregation {
  override type Root = UserAsset
  def findUserWeapon(id: Int): Future[Option[UserWeapon]] = handler.findUserWeapon(id)
  def addWeapon(weaponId: Int): UserAssetAggregation = {
    copy(
      root = root.copy(nextWeaponId = root.nextWeaponId + 1),
      userWeapons = userWeapons.add(UserWeapon(root.nextWeaponId, weaponId))
    )
  }
  def addedWeapons: Iterable[UserWeapon] = userWeapons.added.values
}
class UserAssetHandler(val rootRepository: UserAssetRepository, userWeaponRepository: UserWeaponRepository)
  extends Handler[UserAssetAggregation] {
  override def makeAggregation(entity: Root) = UserAssetAggregation(entity, Entities.empty, this)
  def findUserWeapon(id: UserWeapon#ID): Future[Option[UserWeapon]] = find[UserWeapon](userWeaponRepository, id)
  override def saveChildren(aggregation: UserAssetAggregation): Future[Int] = {
    Future.sequence(aggregation.addedWeapons.map { userWeapon =>
      userWeaponRepository.insert(userWeapon)
    }).map(_.sum)
  }
}

class UserAssetRepository extends InMemoryRepository[UserAsset]
case class UserAsset(id: Int, nextWeaponId: UserWeapon#ID) extends AggregationRootEntity {
  override type ID = Int
}
object UserAsset {
  def apply(id: Int): UserAsset = UserAsset(id, 1)
}
class UserWeaponRepository extends InMemoryRepository[UserWeapon]
case class UserWeapon(id: Int, weaponId: Int) extends Entity {
  override type ID = Int
}

