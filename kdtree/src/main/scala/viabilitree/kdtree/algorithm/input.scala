package viabilitree.kdtree.algorithm

import viabilitree.kdtree.structure._
;

object input {

  def zone[CONTENT](evaluator: Evaluator[CONTENT], label: CONTENT => Boolean, testPoint: CONTENT => Vector[Double])(zone: Zone, depth: Int, rng: util.Random) = {
    //val point = sampler(zone, rng)
    //val content = contentBuilder(point)
    val tree = TreeContent(Leaf[CONTENT](evaluator(Vector(zone), rng).head, zone), depth)
    KdTreeComputation.findTrueLabel(evaluator, label, testPoint)(tree, rng) //, contentBuilder)
  }

  def zoneAndPoint[CONTENT](contentBuilder: Vector[Double] => CONTENT, sampler: Sampler, label: CONTENT => Boolean)(zone: Zone, point: Vector[Double], depth: Int): util.Try[TreeContent[CONTENT]] = {
    val content = contentBuilder(sampler.align(point))

    def tree =
      TreeContent(
        Leaf(
          content,
          zone
        ),
        depth
      )


    label(content) match {
      case true => util.Success(tree)
      case false => util.Failure(new RuntimeException(s"The sample aligned on the grid should be true and it is not. It could mean either that the sample point is false or that the grid size is too large."))
    }
  }

//  def initialTree[CONTENT](
//    contentBuilder: Point => CONTENT,
//    zone: Zone,
//    depth: Int,
//    sampler: Sampler,
//    findTrueLabel: FindTrueLabel[CONTENT])(implicit rng: Random, m: Manifest[CONTENT]): Option[Tree[CONTENT]] = {
//    val point = sampler(zone, rng)
//    val content = contentBuilder(point)
//
//    val tree = Tree(Leaf[CONTENT](content, zone), depth)
//    findTrueLabel(tree, rng)
//  }

}
